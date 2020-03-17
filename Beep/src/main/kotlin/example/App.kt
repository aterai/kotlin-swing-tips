package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder("Toolkit.getDefaultToolkit().beep()")
  val button = JButton("showMessageDialog")
  button.addActionListener {
    Toolkit.getDefaultToolkit().beep()
    JOptionPane.showMessageDialog(p.rootPane, "Error Message", "Title", JOptionPane.ERROR_MESSAGE)
  }
  p.add(button)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
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
