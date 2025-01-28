package example

import java.awt.*
import javax.swing.*
import javax.swing.DebugGraphics.BUFFERED_OPTION
import javax.swing.DebugGraphics.FLASH_OPTION
import javax.swing.DebugGraphics.LOG_OPTION
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = JTable(makeModel())
  table.autoCreateRowSorter = true
  val repaintManager = RepaintManager.currentManager(table)
  repaintManager.isDoubleBufferingEnabled = false
  table.debugGraphicsOptions = BUFFERED_OPTION or FLASH_OPTION or LOG_OPTION
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("ccc", 92, true),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
