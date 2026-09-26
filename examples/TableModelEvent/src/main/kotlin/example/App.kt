package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.synth.SynthUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val columnNames = arrayOf<Any>(Status.INDETERMINATE, "Integer", "String")
  val data = arrayOf<Array<Any>>(
    arrayOf(true, 1, "BBB"),
    arrayOf(false, 12, "AAA"),
    arrayOf(true, 2, "DDD"),
    arrayOf(false, 5, "CCC"),
    arrayOf(true, 3, "EEE"),
    arrayOf(false, 6, "GGG"),
    arrayOf(true, 4, "FFF"),
    arrayOf(false, 7, "HHH"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    private val columnClasses = arrayOf(
      Boolean::class.javaObjectType,
      Int::class.javaObjectType,
      String::class.java,
    )

    // getValueAt(0, column) throws an exception after all rows are deleted
    override fun getColumnClass(column: Int) = columnClasses[column]
  }
  val table = object : JTable(model) {
    private val CHECKBOX_COLUMN = 0

    private var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader().removeMouseListener(handler)
      getModel()?.removeTableModelListener(handler)
      super.updateUI()
      val m = getModel()
      for (i in 0..<m.columnCount) {
        val r = getDefaultRenderer(m.getColumnClass(i))
        SwingUtilities.updateComponentTreeUI(r as? Component)
      }
      val vci = convertColumnIndexToView(CHECKBOX_COLUMN)
      getColumnModel().getColumn(vci).headerRenderer = HeaderRenderer()
      handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN).also {
        it.updateHeaderState()
        m.addTableModelListener(it)
        getTableHeader().addMouseListener(it)
      }
    }

    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ) = super.prepareEditor(editor, row, column).also {
      if (it is JCheckBox) {
        it.background = getSelectionBackground()
        it.isBorderPainted = true
      }
    }
  }
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HeaderRenderer : TableCellRenderer {
  private val check = JCheckBox()
  private val label = JLabel("Check All")
  private val icon = ComponentIcon(label)

  init {
    check.isOpaque = false
    label.isOpaque = false
    label.icon = ComponentIcon(check)
    if (isSynth()) {
      check.text = " "
    }
  }

  private fun isSynth() = check.ui is SynthUI

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val status = value as? Status ?: Status.INDETERMINATE
    status.configureHeaderCheckBox(check)
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      c.isOpaque = false
      if (isSynth()) {
        check.preferredSize = c.preferredSize
      }
      c.icon = icon
      c.text = null
    }
    return c
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add(true)").addActionListener { addRowActionPerformed(true) }
    add("add(false)").addActionListener { addRowActionPerformed(false) }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }

  private fun addRowActionPerformed(isSelected: Boolean) {
    val table = invoker as? JTable
    val model = table?.model
    if (model is DefaultTableModel) {
      model.addRow(arrayOf<Any>(isSelected, 0, ""))
      val rect = table.getCellRect(model.rowCount - 1, 0, true)
      table.scrollRectToVisible(rect)
    }
  }
}

private class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumnIndex: Int,
) : MouseAdapter(),
  TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    val col = e.column
    val targetChanged = col == targetColumnIndex || col == TableModelEvent.ALL_COLUMNS
    if (targetChanged && e.firstRow != TableModelEvent.HEADER_ROW) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
      if (vci >= 0) {
        val column = table.columnModel.getColumn(vci)
        val status = column.headerValue as? Status ?: Status.INDETERMINATE
        val newStatus = if (e.type == TableModelEvent.DELETE) {
          getStatusAfterDelete(status)
        } else {
          getStatusAfterChange(status, e)
        }
        setHeaderStatus(vci, newStatus)
      }
    }
  }

  fun updateHeaderState() {
    val vci = table.convertColumnIndexToView(targetColumnIndex)
    if (vci >= 0) {
      val m = table.model
      setHeaderStatus(vci, resolveStatus(m, 0, m.rowCount - 1))
    }
  }

  private fun setHeaderStatus(vci: Int, status: Status) {
    val column = table.columnModel.getColumn(vci)
    if (column.headerValue !== status) {
      column.headerValue = status
      val h = table.tableHeader
      h.repaint(h.getHeaderRect(vci))
    }
  }

  // TableModelEvent.DELETE: the deleted rows no longer exist in the model
  private fun getStatusAfterDelete(status: Status): Status {
    val m = table.model
    val rowCount = m.rowCount
    return when {
      rowCount == 0 -> Status.DESELECTED
      status == Status.INDETERMINATE -> resolveStatus(m, 0, rowCount - 1)
      // Deleting rows from a uniform column does not change its state
      else -> status
    }
  }

  // TableModelEvent.INSERT or TableModelEvent.UPDATE
  private fun getStatusAfterChange(status: Status, e: TableModelEvent): Status {
    val m = table.model
    val lastIndex = m.rowCount - 1
    val firstRow = maxOf(0, e.firstRow)
    // fireTableDataChanged() sets lastRow to Integer.MAX_VALUE
    val lastRow = minOf(e.lastRow, lastIndex)
    val allRowsChanged = firstRow == 0 && lastRow == lastIndex
    val isUpdate = e.type == TableModelEvent.UPDATE
    return if (allRowsChanged || isUpdate && status == Status.INDETERMINATE) {
      resolveStatus(m, 0, lastIndex)
    } else {
      // The unchanged rows are uniform (or already mixed if INDETERMINATE),
      // so only the changed rows need to be checked
      val changed = resolveStatus(m, firstRow, lastRow)
      if (changed == status) status else Status.INDETERMINATE
    }
  }

  private fun resolveStatus(m: TableModel, firstRow: Int, lastRow: Int): Status {
    val values = (firstRow..lastRow)
      .asSequence()
      .map { m.getValueAt(it, targetColumnIndex) == true }
      .distinct()
      .take(2)
      .toList()
    return when {
      values.isEmpty() -> Status.DESELECTED
      values.size == 1 -> if (values[0]) Status.SELECTED else Status.DESELECTED
      else -> Status.INDETERMINATE
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val model = table.model
    val vci = header.columnAtPoint(e.point)
    val mci = table.convertColumnIndexToModel(vci)
    if (header.isEnabled && mci == targetColumnIndex && model.rowCount > 0) {
      val column = table.columnModel.getColumn(vci)
      val selected = column.headerValue === Status.DESELECTED
      setAllValues(model, selected)
      setHeaderStatus(vci, if (selected) Status.SELECTED else Status.DESELECTED)
    }
  }

  private fun setAllValues(model: TableModel, selected: Boolean) {
    // Suppress the header state check for each row while updating all rows
    model.removeTableModelListener(this)
    try {
      for (i in 0..<model.rowCount) {
        model.setValueAt(selected, i, targetColumnIndex)
      }
    } finally {
      model.addTableModelListener(this)
    }
  }
}

private class ComponentIcon(
  private val cmp: Component,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    SwingUtilities.paintComponent(g, cmp, c?.parent, x, y, iconWidth, iconHeight)
  }

  override fun getIconWidth() = cmp.preferredSize.width

  override fun getIconHeight() = cmp.preferredSize.height
}

private enum class Status(
  private val selected: Boolean,
  private val enabled: Boolean,
) {
  SELECTED(true, true),
  DESELECTED(false, true),
  INDETERMINATE(true, false),
  ;

  fun configureHeaderCheckBox(check: JCheckBox) {
    check.isSelected = selected
    check.isEnabled = enabled
  }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
