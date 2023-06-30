package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("AAA", 0, true),
    arrayOf("BBB", 1, false),
    arrayOf("CCC", 2, true),
    arrayOf("DDD", 3, true),
    arrayOf("EEE", 4, true),
    arrayOf("FFF", 5, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  val sorter = object : TableRowSorter<TableModel>(model) {
    override fun isSortable(column: Int) = false
  }
  table.rowSorter = sorter
  val defFilter = sorter.rowFilter
  val filter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel?, out Int>) = entry.identifier % 2 == 0
  }

  val check = JCheckBox("filter: idx%2==0")
  check.addActionListener { e ->
    sorter.rowFilter = if ((e.source as? JCheckBox)?.isSelected == true) filter else defFilter
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
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
