package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("aaa", "bbb")
  val data = arrayOf(
    arrayOf(Int.MIN_VALUE, Int.MIN_VALUE),
    arrayOf(1, 1),
    arrayOf(1, 2),
    arrayOf(1, -1),
    arrayOf(1, 3),
    arrayOf(1, 0),
    arrayOf(1, 5),
    arrayOf(1, 4),
    arrayOf(1, -5),
    arrayOf(1, 0),
    arrayOf(1, 6),
    arrayOf(Int.MAX_VALUE, Int.MAX_VALUE)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = Number::class.java

    override fun isCellEditable(row: Int, column: Int) = row > 0 && row != rowCount - 1
  }
  val table = JTable(model)
  val filter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>) =
      0 != table.convertRowIndexToView(entry.identifier)
  }
  val s = object : TableRowSorter<TableModel>(model) {
    override fun toggleSortOrder(column: Int) {
      val f = rowFilter
      rowFilter = null
      super.toggleSortOrder(column)
      rowFilter = f
    }
  }
  s.rowFilter = filter
  s.toggleSortOrder(1)
  table.rowSorter = s
  model.addTableModelListener { e ->
    if (e.type == TableModelEvent.UPDATE) {
      table.repaint()
    }
  }
  val renderer = object : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int
    ): Component {
      val c: Component
      val m = table.model
      if (row == m.rowCount - 2) {
        val total = (1 until m.rowCount - 1).sumOf {
          m.getValueAt(it, column) as? Int ?: 0
        }
        c = super.getTableCellRendererComponent(table, total, isSelected, hasFocus, row, column)
        c.background = Color.ORANGE
      } else {
        c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        c.background = Color.WHITE
      }
      c.foreground = Color.BLACK
      return c
    }
  }
  val cm = table.columnModel
  for (i in 0 until cm.columnCount) {
    cm.getColumn(i).cellRenderer = renderer
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
