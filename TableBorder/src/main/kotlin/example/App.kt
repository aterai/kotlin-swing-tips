package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val table = JTable(DefaultTableModel(15, 3))
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.border = BorderFactory.createLineBorder(Color.GREEN, 5)
  table.background = Color.WHITE

  table.tableHeader.border = BorderFactory.createMatteBorder(0, 5, 0, 5, Color.ORANGE)
  table.tableHeader.background = Color.MAGENTA

  val scroll = JScrollPane(table)
  scroll.border = BorderFactory.createLineBorder(Color.BLUE, 5)
  scroll.viewportBorder = BorderFactory.createLineBorder(Color.RED, 5)
  scroll.background = Color.YELLOW
  scroll.viewport.background = Color.PINK

  EventQueue.invokeLater {
    val vp = scroll.columnHeader
    vp.isOpaque = true
    vp.background = Color.CYAN
  }

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(scroll)
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
