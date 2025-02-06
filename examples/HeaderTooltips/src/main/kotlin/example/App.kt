package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val table = JTable(5, 3)
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.tableHeader = object : JTableHeader(table.columnModel) {
    override fun getToolTipText(e: MouseEvent): String? {
      val column = columnAtPoint(e.point)
      return if (column >= 0) getToolTipText(column) else null
    }

    private fun getToolTipText(column: Int): String {
      val c = columnModel.getColumn(column)
      return "%s (width=%dpx)".format(c.headerValue, c.width)
    }
  }
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
