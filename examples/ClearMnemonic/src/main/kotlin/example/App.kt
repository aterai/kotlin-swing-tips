package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val textField = JTextField("B", 1)
  val button = JButton("Button")
  button.addActionListener {
    Toolkit.getDefaultToolkit().beep()
  }
  val btnSetMnemonic = JButton("setMnemonic(...)")
  btnSetMnemonic.addActionListener {
    // val str = textField.text.trim().takeUnless { it.isEmpty() } ?: button.text
    val str = textField.text.trim().ifEmpty { button.text }
    button.mnemonic = str.codePointAt(0)
  }
  val btnClearMnemonic = JButton("clear Mnemonic")
  btnClearMnemonic.addActionListener { button.mnemonic = 0 }
  // btnClearMnemonic.addActionListener { button.mnemonic = '\u0000' }
  // btnClearMnemonic.addActionListener { button.mnemonic = '\0' }
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("setMnemonic")
  p.add(textField)
  p.add(btnSetMnemonic)
  p.add(btnClearMnemonic)
  return JPanel().also {
    it.add(button)
    it.add(p)
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
