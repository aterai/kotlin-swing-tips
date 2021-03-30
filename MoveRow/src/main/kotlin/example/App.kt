package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Serializable
import java.util.Objects
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val table = JTable(makeModel())
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.rowSelectionAllowed = true
  table.componentPopupMenu = TablePopupMenu(table)

  val header = table.tableHeader
  header.reorderingAllowed = false

  val headerRenderer = SortButtonRenderer()
  header.defaultRenderer = headerRenderer
  header.addMouseListener(HeaderMouseListener())

  table.columnModel.getColumn(0).also {
    it.minWidth = 80
    it.maxWidth = 80
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(makeToolBar(table), BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel() = RowDataModel().also {
  it.addRowData(RowData("Name 1", "comment..."))
  it.addRowData(RowData("Name 2", "Test"))
  it.addRowData(RowData("Name d", "ee"))
  it.addRowData(RowData("Name c", "Test cc"))
  it.addRowData(RowData("Name b", "Test bb"))
  it.addRowData(RowData("Name a", "ff"))
  it.addRowData(RowData("Name 0", "Test aa"))
}

private fun makeToolBar(table: JTable) = JToolBar().also {
  it.isFloatable = true
  it.add(makeToolButton(UpAction("£", table)))
  it.add(makeToolButton(DownAction("¥", table)))
  it.add(Box.createHorizontalGlue())
  it.add(makeToolButton(InitAction("OK", table)))
}

private fun makeToolButton(action: Action) = JButton(action).also {
  it.isFocusable = false
}

private class TablePopupMenu(private val table: JTable) : JPopupMenu() {
  private val createAction: Action
  private val deleteAction: Action
  private val upAction: Action
  private val downAction: Action

  init {
    createAction = RowDataCreateAction("add", table)
    deleteAction = DeleteAction("delete", table)
    upAction = UpAction("up", table)
    downAction = DownAction("down", table)
    add(createAction)
    addSeparator()
    add(deleteAction)
    addSeparator()
    add(upAction)
    add(downAction)
  }

  override fun show(c: Component, x: Int, y: Int) {
    val row = table.rowAtPoint(Point(x, y))
    if (row > 0 && !table.selectedRows.any { it == row }) {
      table.setRowSelectionInterval(row, row)
    }
    val count = table.selectedRowCount
    createAction.isEnabled = count <= 1
    deleteAction.isEnabled = row >= 0
    upAction.isEnabled = count > 0
    downAction.isEnabled = count > 0
    super.show(c, x, y)
  }
}

private class RowDataCreateAction(str: String, private val table: JTable) : AbstractAction(str) {
  override fun actionPerformed(e: ActionEvent?) {
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    (table.model as? RowDataModel)?.also {
      it.addRowData(RowData("New row", ""))
      val r = table.getCellRect(it.rowCount - 1, 0, true)
      table.scrollRectToVisible(r)
    }
  }
}

private class DeleteAction(str: String, private val table: JTable) : AbstractAction(str) {
  override fun actionPerformed(e: ActionEvent?) {
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    val selection = table.selectedRows
    (table.model as? RowDataModel)?.also {
      for (i in selection.indices.reversed()) {
        it.removeRow(selection[i])
      }
    }
  }
}

private class UpAction(str: String, private val table: JTable) : AbstractAction(str) {
  override fun actionPerformed(e: ActionEvent) {
    val model = table.model
    val pos = table.selectedRows
    if (pos.isEmpty() || model !is RowDataModel) {
      return
    }
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
    println(isShiftDown)
    if (isShiftDown) { // Jump to the top
      model.moveRow(pos[0], pos[pos.size - 1], 0)
      table.setRowSelectionInterval(0, pos.size - 1)
    } else {
      if (pos[0] == 0) {
        return
      }
      model.moveRow(pos[0], pos[pos.size - 1], pos[0] - 1)
      table.setRowSelectionInterval(pos[0] - 1, pos[pos.size - 1] - 1)
    }
    val r = table.getCellRect(model.rowCount - 1, 0, true)
    table.scrollRectToVisible(r)
  }
}

private class DownAction(str: String, private val table: JTable) : AbstractAction(str) {
  override fun actionPerformed(e: ActionEvent) {
    val model = table.model
    val pos = table.selectedRows
    if (pos.isEmpty() || model !is RowDataModel) {
      return
    }
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    val isShiftDown = e.modifiers and ActionEvent.SHIFT_MASK != 0
    if (isShiftDown) { // Jump to the end
      model.moveRow(pos[0], pos[pos.size - 1], model.rowCount - pos.size)
      table.setRowSelectionInterval(model.rowCount - pos.size, model.rowCount - 1)
    } else {
      if (pos[pos.size - 1] == model.rowCount - 1) {
        return
      }
      model.moveRow(pos[0], pos[pos.size - 1], pos[0] + 1)
      table.setRowSelectionInterval(pos[0] + 1, pos[pos.size - 1] + 1)
    }
    val r = table.getCellRect(model.rowCount - 1, 0, true)
    table.scrollRectToVisible(r)
  }
}

private class InitAction(str: String, private val table: JTable) : AbstractAction(str) {
  override fun actionPerformed(e: ActionEvent?) {
    val model = table.model
    val row = table.rowCount
    if (row <= 0 || model !is RowDataModel) {
      return
    }
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    val currentModel = RowDataModel()
    val dv = model.dataVector
    for (i in 0 until row) {
      val v = dv[i] as? List<*> ?: continue
      currentModel.addRowData(makeRowData(v))
    }
    val h = table.tableHeader
    val tcr = h.defaultRenderer
    if (tcr is SortButtonRenderer) {
      tcr.setPressedColumn(-1)
      tcr.setSelectedColumn(-1)
    }
    table.autoCreateColumnsFromModel = false
    table.model = currentModel
    table.clearSelection()
  }

  private fun makeRowData(list: List<*>) = RowData(list[1].toString(), list[2].toString())
}

private class RowDataModel : SortableTableModel() {
  private var number = 0
  fun addRowData(t: RowData) {
    val obj = arrayOf(number, t.name, t.comment)
    super.addRow(obj)
    number++
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(val columnName: String, val columnClass: Class<*>, val isEditable: Boolean)
  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Comment", String::class.java, true)
    )
  }
}

private data class RowData(val name: String, val comment: String)

private open class SortableTableModel : DefaultTableModel() {
  fun sortByColumn(column: Int, isAscent: Boolean) {
    getDataVector().sortWith(ColumnComparator(column, isAscent))
    fireTableDataChanged()
  }
}

private class ColumnComparator(val index: Int, val ascending: Boolean) : Comparator<Any>, Serializable {
  override fun compare(one: Any, two: Any): Int {
    val one1 = (one as? List<*>)?.filterIsInstance<Comparable<Any>>()
    val two1 = (two as? List<*>)?.filterIsInstance<Comparable<Any>>()
    if (one1?.isNotEmpty() == true && two1?.isNotEmpty() == true) {
      val cp = Comparator.nullsFirst(Comparator.naturalOrder<Comparable<Any>>())
      val c = Objects.compare(one1[index], two1[index], cp)
      return c * if (ascending) 1 else -1
    }
    return 0
  }

  companion object {
    private const val serialVersionUID = 1L
  }
}

private class SortButtonRenderer : JButton(), TableCellRenderer {
  private var iconSize: Dimension? = null
  private var pushedColumn = -1
  private val state = mutableMapOf<Int, Int>()

  override fun updateUI() {
    super.updateUI()
    val i = UIManager.getIcon("Table.ascendingSortIcon")
    iconSize = Dimension(i.iconWidth, i.iconHeight).also {
      icon = EmptyIcon(it)
    }
    horizontalTextPosition = LEFT
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    text = value?.toString() ?: ""
    icon = iconSize?.let { EmptyIcon(it) }
    val modelColumn = table.convertColumnIndexToModel(column)
    val iv = state[modelColumn]
    if (iv == DOWN) {
      icon = UIManager.getIcon("Table.ascendingSortIcon")
    } else if (iv == UP) {
      icon = UIManager.getIcon("Table.descendingSortIcon")
    }
    val isPressed = modelColumn == pushedColumn
    getModel().isPressed = isPressed
    getModel().isArmed = isPressed
    return this
  }

  fun setPressedColumn(col: Int) {
    pushedColumn = col
  }

  fun setSelectedColumn(col: Int) {
    if (col < 0) {
      state.clear()
      return
    }
    val obj = state[col]
    val value = if (obj != null && obj == DOWN) UP else DOWN
    state.clear()
    state[col] = value
  }

  fun getState(col: Int) = state[col] ?: NONE

  companion object {
    const val NONE = 0
    const val DOWN = 1
    const val UP = 2
  }
}

private class HeaderMouseListener : MouseAdapter() {
  override fun mousePressed(e: MouseEvent) {
    val h = e.component as? JTableHeader ?: return
    val columnModel = h.columnModel
    val viewColumn = columnModel.getColumnIndexAtX(e.x)
    if (viewColumn < 0) {
      return
    }
    val tcr = h.defaultRenderer
    val column = columnModel.getColumn(viewColumn).modelIndex
    if (column != -1 && tcr is SortButtonRenderer) {
      tcr.setPressedColumn(column)
      tcr.setSelectedColumn(column)
      h.repaint()
      val table = h.table
      if (table.isEditing) {
        table.cellEditor.stopCellEditing()
      }
      (table.model as? SortableTableModel)?.sortByColumn(column, SortButtonRenderer.DOWN == tcr.getState(column))
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.repaint()
  }
}

private class EmptyIcon(private val size: Dimension) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    /* Empty icon */
  }

  override fun getIconWidth() = size.width

  override fun getIconHeight() = size.height
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
