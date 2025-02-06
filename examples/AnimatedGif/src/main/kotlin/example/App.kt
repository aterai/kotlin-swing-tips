package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  val cl = Thread.currentThread().contextClassLoader
  listOf(
    "no_disposal_specified",
    "do_not_dispose",
    "restore_to_background_color",
    "restore_to_previous",
  ).forEach {
    val icon = ImageIcon(cl.getResource("example/$it.gif"))
    box.add(JLabel(it, icon, SwingConstants.LEFT))
    box.add(Box.createVerticalStrut(20))
  }
  box.add(Box.createVerticalGlue())
  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(20, 40, 20, 40)
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
