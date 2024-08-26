package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = JTable(makeModel())
  table.autoCreateRowSorter = true
  val footer = JTableHeader(table.columnModel)
  footer.table = table
  val south = JScrollPane()
  val vp = JViewport()
  vp.view = footer
  south.columnHeader = vp
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.add(JScrollPane(table))
  p.add(south, BorderLayout.SOUTH)
  return JPanel(BorderLayout()).also {
    it.add(p)
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
    arrayOf<Any>("eee", 22, true),
    arrayOf<Any>("fff", 6, false),
    arrayOf<Any>("ggg", 83, true),
    arrayOf<Any>("hhh", 9, false),
    arrayOf<Any>("iii", 31, true),
    arrayOf<Any>("jjj", 4, false),
    arrayOf<Any>("kkk", 75, true),
    arrayOf<Any>("lll", 8, false),
    arrayOf<Any>("mmm", 77, true),
    arrayOf<Any>("nnn", 2, false),
    arrayOf<Any>("OOO", 68, true),
    arrayOf<Any>("PPP", 7, false),
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
