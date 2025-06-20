package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val tab = JTabbedPane()
  tab.addTab("<html>Test<p>Test</p></html>", JLabel("Test1"))
  tab.addTab("<html>Test<p>test", JLabel("Test2"))
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("<html>Comment<p>etc.", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.rowHeight = 32
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  return JPanel(BorderLayout()).also {
    it.add(tab, BorderLayout.NORTH)
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
