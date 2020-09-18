package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Collections
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("Group", "Name", "Count")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = RowData::class.java
  }
  val colors = "colors"
  addRowData(model, RowData(colors, "blue", 1))
  addRowData(model, RowData(colors, "violet", 2))
  addRowData(model, RowData(colors, "red", 3))
  addRowData(model, RowData(colors, "yellow", 4))
  val sports = "sports"
  addRowData(model, RowData(sports, "baseball", 23))
  addRowData(model, RowData(sports, "soccer", 22))
  addRowData(model, RowData(sports, "football", 21))
  addRowData(model, RowData(sports, "hockey", 20))
  val food = "food"
  addRowData(model, RowData(food, "hot dogs", 10))
  addRowData(model, RowData(food, "pizza", 11))
  addRowData(model, RowData(food, "ravioli", 12))
  addRowData(model, RowData(food, "bananas", 13))

  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      fillsViewportHeight = true
      setDefaultRenderer(RowData::class.java, RowDataRenderer())
      val sorter = TableRowSorter(getModel())
      val c = Comparator.comparing(RowData::group)
      sorter.setComparator(0, c)
      sorter.setComparator(1, c.thenComparing(RowData::name))
      sorter.setComparator(2, c.thenComparing(RowData::count))
      rowSorter = sorter
    }
  }
  val button = JButton("clear SortKeys")
  button.addActionListener {
    table.rowSorter.setSortKeys(null)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addRowData(model: DefaultTableModel, data: RowData) {
  model.addRow(Collections.nCopies(model.columnCount, data).toTypedArray())
}

private data class RowData(val group: String, val name: String, val count: Int)

private class RowDataRenderer : TableCellRenderer {
  private val renderer: TableCellRenderer = DefaultTableCellRenderer()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = renderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    if (c is JLabel && value is RowData) {
      c.horizontalAlignment = SwingConstants.LEFT
      when (table.convertColumnIndexToModel(column)) {
        0 -> {
          val str = value.group
          val prev = if (row > 0) {
            (table.getValueAt(row - 1, column) as? RowData)?.group
          } else {
            null
          }
          c.text = if (str == prev) " " else "+ $str"
        }
        1 -> c.text = value.name
        2 -> {
          c.horizontalAlignment = SwingConstants.RIGHT
          c.text = value.count.toString()
        }
      }
    }
    return c
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
