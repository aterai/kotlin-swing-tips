package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val label = JLabel()
  val infoPanel = JPanel()
  infoPanel.add(label)
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.selectionModel.addListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      label.text = if (table.selectedRowCount == 1) getInfo(table) else " "
      infoPanel.isVisible = false
      infoPanel.removeAll()
      infoPanel.add(label)
      infoPanel.isVisible = true
    }
  }
  table.rowSelectionAllowed = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)

  table.columnModel.getColumn(0).also {
    it.minWidth = 60
    it.maxWidth = 60
    it.resizable = false
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(infoPanel, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getInfo(table: JTable): String {
  val model = table.model
  val index = table.convertRowIndexToModel(table.selectedRow)
  val str = model.getValueAt(index, 0) // .toString()
  val idx = model.getValueAt(index, 1) // as Int
  val flg = model.getValueAt(index, 2) // as Boolean
  return "$str, $idx, $flg"
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
