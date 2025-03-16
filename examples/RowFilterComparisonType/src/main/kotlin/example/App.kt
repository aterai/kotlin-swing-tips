package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.RowFilter.ComparisonType
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val model = SpinnerNumberModel(10, 0, 100, 5)

fun makeUI(): Component {
  val m = makeModel()
  val table = JTable(m)
  val sorter = TableRowSorter(m)
  table.rowSorter = sorter
  val combo = JComboBox(ComparisonType.entries.toTypedArray())
  combo.isEnabled = false
  val check = JCheckBox("setRowFilter")
  check.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      setFilter(sorter, getComparisonType(combo))
      combo.setEnabled(true)
    } else {
      sorter.rowFilter = null
      combo.setEnabled(false)
    }
  }
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val o = e.item
      if (o is ComparisonType && check.isSelected) {
        setFilter(sorter, o)
      }
    }
  }
  model.addChangeListener {
    if (check.isSelected) {
      setFilter(sorter, getComparisonType(combo))
    }
  }
  val p = JPanel()
  p.add(check)
  p.add(JSpinner(model))
  p.add(combo)
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getComparisonType(c: JComboBox<ComparisonType>) =
  c.getItemAt(c.selectedIndex)

private fun setFilter(sorter: TableRowSorter<TableModel>, type: ComparisonType) {
  val num = model.number.toInt()
  sorter.rowFilter = RowFilter.numberFilter(type, num)
}

private fun makeModel(): TableModel {
  val min = 0
  val max = 100
  val columnNames = arrayOf("Integer(%d..%d)".format(min, max))
  val model = object : DefaultTableModel(columnNames, 5) {
    override fun getColumnClass(column: Int) = Integer::class.java
  }
  repeat(50) {
    val i = (min..max).random()
    model.addRow(arrayOf<Any>(i))
  }
  return model
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
