package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun getToolTipText(e: MouseEvent): String? {
      val idx = rowAtPoint(e.point)
      return if (idx >= 0) {
        val row = convertRowIndexToModel(idx)
        val m = getModel()
        val v0 = m.getValueAt(row, 0)
        val v1 = m.getValueAt(row, 1)
        val v2 = m.getValueAt(row, 2)
        "<html>$v0<br>$v1<br>$v2"
      } else {
        super.getToolTipText(e)
      }
    }
  }
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true

  return JPanel(BorderLayout()).also {
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
