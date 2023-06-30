package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val tab = JTabbedPane()
  tab.addTab("Label", JLabel("label"))
  tab.setMnemonicAt(0, KeyEvent.VK_L)
  tab.setDisplayedMnemonicIndexAt(0, 0)
  tab.addTab("Tree", JScrollPane(JTree()))
  tab.setMnemonicAt(1, KeyEvent.VK_T)
  tab.setDisplayedMnemonicIndexAt(1, 0)
  tab.addTab("TextField", JTextField("field"))
  tab.setMnemonicAt(2, KeyEvent.VK_F)
  tab.setDisplayedMnemonicIndexAt(2, 4)
  tab.addTab("Button", JButton("button"))
  tab.setMnemonicAt(3, KeyEvent.VK_B)
  tab.setDisplayedMnemonicIndexAt(3, 0)

  return JPanel(BorderLayout()).also {
    it.add(tab)
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
