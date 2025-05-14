package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val model = makeModel()
  val table = JTable(model)
  table.rowSorter = TableRowSorter(model)
  val renderer = TableCellRenderer { tbl, value, isSelected, hasFocus, row, column ->
    val renderer = tbl.tableHeader.defaultRenderer
    renderer
      .getTableCellRendererComponent(
        tbl,
        value,
        isSelected,
        hasFocus,
        row,
        column,
      ).also {
        val rs = tbl.rowSorter
        if (rs is DefaultRowSorter<*, *>) {
          val cmi = tbl.convertColumnIndexToModel(column)
          it.foreground = if (rs.isSortable(cmi)) Color.BLACK else Color.GRAY
        }
      }
  }
  val columns = table.columnModel
  for (i in 0..<columns.columnCount) {
    val c = columns.getColumn(i)
    c.headerRenderer = renderer
    if (i == 0) {
      c.minWidth = 60
      c.maxWidth = 60
      c.resizable = false
    }
  }
  val check = JCheckBox("Sortable(1, false)")
  check.addActionListener { e ->
    val cb = e.source
    val rs = table.rowSorter
    if (rs is DefaultRowSorter<*, *> && cb is JCheckBox) {
      rs.setSortable(1, !cb.isSelected)
      table.tableHeader.repaint()
    }
  }
  val button = JButton("clear SortKeys")
  button.addActionListener {
    table.rowSorter.sortKeys = null
  }
  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(button, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf<Any>("aaa", 12, true),
    arrayOf<Any>("bbb", 5, false),
    arrayOf<Any>("CCC", 92, true),
    arrayOf<Any>("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
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
