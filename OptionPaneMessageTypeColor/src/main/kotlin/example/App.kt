package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("JOptionPane")
  for (type in MessageType.values()) {
    p.add(makeButton(p, type))
  }
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButton(p: JPanel, type: MessageType): JButton {
  val msg = type.toString()
  val b = JButton(msg)
  b.addActionListener { showDialog(p.rootPane, msg, type.messageType) }
  return b
}

private fun showDialog(c: Component, msg: String, type: Int) {
  JOptionPane.showMessageDialog(c, msg, msg, type)
}

private enum class MessageType(val messageType: Int) {
  PLAIN(JOptionPane.PLAIN_MESSAGE),
  ERROR(JOptionPane.ERROR_MESSAGE),
  INFORMATION(JOptionPane.INFORMATION_MESSAGE),
  WARNING(JOptionPane.WARNING_MESSAGE),
  QUESTION(JOptionPane.QUESTION_MESSAGE);
}

fun main() {
  EventQueue.invokeLater {
    JFrame.setDefaultLookAndFeelDecorated(true)
    JDialog.setDefaultLookAndFeelDecorated(true)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
