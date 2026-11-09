package example

import java.awt.*
import java.text.Collator
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

private val HTML_TAG = "<[^>]*>".toRegex()

fun createUI(): Component {
  val tabbedPane = JTabbedPane()
  tabbedPane.addTab("<html>Test<p>Test</p></html>", JLabel("Test1"))
  // The tags do not have to be well-formed
  tabbedPane.addTab("<html>Test<p>test", JLabel("Test2"))

  val model = makeModel()
  val sorter = TableRowSorter<TableModel>(model)
  // Sort by the displayed text instead of the raw "<html>..." markup
  val collator = Collator.getInstance()
  sorter.setComparator(0, compareBy<String, String>(collator) { removeHtmlTags(it) })
  val table = JTable(model)
  table.rowSorter = sorter
  table.rowHeight = 32
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "<html><span style='color:red'>Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("<html><span style='color:blue'>bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("<html><span style='color:green'>DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private fun removeHtmlTags(text: String) = HTML_TAG.replace(text, "")

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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
