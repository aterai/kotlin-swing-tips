package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private const val MAXIMUM_ROW_COUNT = 5

fun makeUI(): Component {
  val check1 = JCheckBox("Custom Sorting")

  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("AA", 1, true), arrayOf("BB", 2, false),
    arrayOf("cc", 3, true), arrayOf("dd", 4, false),
    arrayOf("ee", 5, false), arrayOf("FF", -1, true),
    arrayOf("GG", -2, false), arrayOf("HH", -3, true),
    arrayOf("II", -4, false), arrayOf("JJ", -5, false),
    arrayOf("KK", 11, true), arrayOf("LL", 22, false),
    arrayOf("MM", 33, true), arrayOf("NN", 44, false),
    arrayOf("OO", 55, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.fillsViewportHeight = true
  // XXX: sorter.setSortsOnUpdates(true)

  val sorter = object : TableRowSorter<TableModel>(model) {
    override fun toggleSortOrder(column: Int) {
      super.toggleSortOrder(column)
      if (check1.isSelected) {
        model.fireTableDataChanged()
        sort() // allRowsChanged()
      }
    }
  }

  table.rowSorter = sorter
  sorter.sortKeys = listOf(RowSorter.SortKey(1, SortOrder.DESCENDING))

  val filter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val idx = table.convertRowIndexToView(entry.identifier.toInt())
      return idx < MAXIMUM_ROW_COUNT
    }
  }
  val defFilter = sorter.rowFilter

  val check2 = JCheckBox("viewRowIndex < $MAXIMUM_ROW_COUNT")
  check2.addActionListener {
    val b = (it.source as? JCheckBox)?.isSelected == true
    sorter.rowFilter = if (b) filter else defFilter
  }

  val box = Box.createHorizontalBox()
  box.add(check1)
  box.add(Box.createHorizontalStrut(5))
  box.add(check2)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
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
