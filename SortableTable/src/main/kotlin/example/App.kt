package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.Serializable
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val model = RowDataModel()
  model.addRowData(RowData("Name 1", "comment..."))
  model.addRowData(RowData("Name 2", "Test"))
  model.addRowData(RowData("Name d", "ee"))
  model.addRowData(RowData("Name c", "Test cc"))
  model.addRowData(RowData("Name b", "Test bb"))
  model.addRowData(RowData("Name a", "ff"))
  model.addRowData(RowData("Name 0", "Test aa"))

  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ) = super.prepareRenderer(tcr, row, column).also {
      if (isRowSelected(row)) {
        it.foreground = getSelectionForeground()
        it.background = getSelectionBackground()
      } else {
        it.foreground = foreground
        it.background = if (row % 2 == 0) evenColor else background
      }
    }
  }
  table.rowSelectionAllowed = true
  table.fillsViewportHeight = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.componentPopupMenu = TablePopupMenu()

  val header = table.tableHeader
  header.defaultRenderer = SortButtonRenderer()
  header.addMouseListener(HeaderMouseListener())
  header.reorderingAllowed = false

  table.columnModel.getColumn(0).also {
    it.minWidth = 80
    it.maxWidth = 80
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      if (table?.isEditing == true) {
        table.cellEditor.stopCellEditing()
      }
      (table?.model as? RowDataModel)?.also {
        it.addRowData(RowData("New row", ""))
        val r = table.getCellRect(it.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      if (table?.isEditing == true) {
        table.cellEditor.stopCellEditing()
      }
      (table?.model as? DefaultTableModel)?.also {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          it.removeRow(selection[i])
        }
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }
}

private class RowDataModel : SortableTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    val obj = arrayOf(number, t.name, t.comment)
    super.addRow(obj)
    number++
  }

  override fun isCellEditable(
    row: Int,
    col: Int,
  ) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(
    val columnName: String,
    val columnClass: Class<*>,
    val isEditable: Boolean,
  )

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Comment", String::class.java, true),
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

private class ColumnComparator(
  val index: Int,
  val ascending: Boolean,
) : Comparator<Any>, Serializable {
  override fun compare(one: Any, two: Any): Int {
    val one1 = (one as? List<*>)?.filterIsInstance<Comparable<Any>>()
    val two1 = (two as? List<*>)?.filterIsInstance<Comparable<Any>>()
    if (one1?.isNotEmpty() == true && two1?.isNotEmpty() == true) {
      // val cp = Comparator.nullsFirst(Comparator.naturalOrder<Comparable<Any>>())
      // val c = Objects.compare(one1[index], two1[index], cp)
      val c = compareValuesBy(one1, two1) { it[index] }
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
    column: Int,
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
    val header = e.component as? JTableHeader ?: return
    val table = header.table
    if (table.isEditing) {
      table.cellEditor.stopCellEditing()
    }
    val renderer = header.defaultRenderer
    val viewColumn = table.columnAtPoint(e.point)
    if (viewColumn >= 0 && renderer is SortButtonRenderer) {
      val column = table.convertColumnIndexToModel(viewColumn)
      renderer.setPressedColumn(column)
      renderer.setSelectedColumn(column)
      header.repaint()
      val isAscent = SortButtonRenderer.DOWN == renderer.getState(column)
      (table.model as? SortableTableModel)?.sortByColumn(column, isAscent)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.repaint()
  }
}

private class EmptyIcon(private val size: Dimension) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // Empty icon
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
