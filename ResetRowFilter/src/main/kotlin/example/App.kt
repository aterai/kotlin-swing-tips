package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private const val USER_SPECIFIED_NUMBER_OF_ROWS = 5

fun makeUI(): Component {
  val check1 = JCheckBox("Custom Sorting")

  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf<Any>("AA", 1, true), arrayOf<Any>("BB", 2, false),
    arrayOf<Any>("cc", 3, true), arrayOf<Any>("dd", 4, false),
    arrayOf<Any>("ee", 5, false), arrayOf<Any>("FF", -1, true),
    arrayOf<Any>("GG", -2, false), arrayOf<Any>("HH", -3, true),
    arrayOf<Any>("II", -4, false), arrayOf<Any>("JJ", -5, false),
    arrayOf<Any>("KK", 11, true), arrayOf<Any>("LL", 22, false),
    arrayOf<Any>("MM", 33, true), arrayOf<Any>("NN", 44, false),
    arrayOf<Any>("OO", 55, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.setFillsViewportHeight(true)
  // XXX: sorter.setSortsOnUpdates(true)

  val filter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val vidx = table.convertRowIndexToView(entry.getIdentifier().toInt())
      return vidx < USER_SPECIFIED_NUMBER_OF_ROWS
    }
  }
  val sorter = object : TableRowSorter<TableModel>(model) {
    override fun toggleSortOrder(column: Int) {
      super.toggleSortOrder(column)
      if (check1.isSelected()) {
        model.fireTableDataChanged()
        sort() // allRowsChanged()
      }
    }
  }

  table.setRowSorter(sorter)
  sorter.setSortKeys(listOf(RowSorter.SortKey(1, SortOrder.DESCENDING)))

  val check2 = JCheckBox("viewRowIndex < $USER_SPECIFIED_NUMBER_OF_ROWS")
  check2.addActionListener {
    sorter.setRowFilter((it.getSource() as? JCheckBox)?.isSelected()?.let { filter })
  }

  val box = Box.createHorizontalBox()
  box.add(check1)
  box.add(Box.createHorizontalStrut(5))
  box.add(check2)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
