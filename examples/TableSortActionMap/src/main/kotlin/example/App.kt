package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private val help = """
JTableHeader, toggleSortOrder, SPACE(default)
JTableHeader, selectColumnToLeft, LEFT(default)
JTableHeader, selectColumnToRight, RIGHT(default)
JTableHeader, focusTable, ESCAPE(default)
JTableHeader, ascendant, ctrl UP
JTableHeader, descendant, ctrl DOWN
JTableHeader, unsorted, F9
JTable, F8: focusHeader(default)
JTable, ascendant, ctrl UP
JTable, descendant, ctrl DOWN
JTable, unsorted, F9
""".trimIndent()

fun makeUI(): Component {
  val table = object : JTable(makeModel()) {
    override fun updateUI() {
      super.updateUI()
      setCellSelectionEnabled(true)
      autoCreateRowSorter = true
      val header = getTableHeader()
      val cm = header.columnModel
      val r = ColumnHeaderRenderer()
      for (i in 0..<cm.columnCount) {
        cm.getColumn(i).headerRenderer = r
      }
      cm.selectionModel.addListSelectionListener { header.repaint() }
    }
  }
  initMap(table, "ascendant", "ctrl UP", SortOrder.ASCENDING)
  initMap(table, "descendant", "ctrl DOWN", SortOrder.DESCENDING)
  initMap(table, "unsorted", "F9", SortOrder.UNSORTED)
  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(table))
    it.add(JScrollPane(JTextArea(help)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      getValueAt(0, column).javaClass
  }
}

private fun initMap(table: JTable, key: String, ks: String, order: SortOrder) {
  val a = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      columnSort(e, order)
    }
  }
  table.actionMap.put(key, a)
  val im1 = table.getInputMap(JComponent.WHEN_FOCUSED)
  im1.put(KeyStroke.getKeyStroke(ks), key)
  val header = table.tableHeader
  header.actionMap.put(key, a)
  val im2 = header.getInputMap(JComponent.WHEN_FOCUSED)
  im2.put(KeyStroke.getKeyStroke(ks), key)
}

private fun columnSort(e: ActionEvent, order: SortOrder) {
  val src = e.source
  if (src is JTable) {
    src.tableHeader?.also {
      src.actionMap["focusHeader"].actionPerformed(e)
      sort(src, src.selectedColumn, order)
      val cmd = "focusTable"
      val id = ActionEvent.ACTION_PERFORMED
      it.actionMap[cmd].actionPerformed(ActionEvent(it, id, cmd))
    }
  } else if (src is JTableHeader) {
    sort(src.table, getSelectedColumnIndex(src), order)
  }
}

private fun sort(table: JTable, col: Int, order: SortOrder) {
  if (col >= 0) {
    val sortKey = RowSorter.SortKey(col, order)
    table.rowSorter.sortKeys = listOf(sortKey)
  }
}

private fun getSelectedColumnIndex(header: JTableHeader): Int {
  var col = -1
  val cm = header.columnModel
  for (i in 0..<cm.columnCount) {
    val r = cm.getColumn(i).headerRenderer
    if (r is ColumnHeaderRenderer) {
      col = r.selectedColumnIndex
      if (col >= 0) {
        break
      }
    }
  }
  return col
}

private class ColumnHeaderRenderer : TableCellRenderer {
  var selectedColumnIndex: Int = -1
    private set

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (hasFocus) {
      selectedColumnIndex = column
    }
    val r = table.tableHeader.defaultRenderer
    return r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
