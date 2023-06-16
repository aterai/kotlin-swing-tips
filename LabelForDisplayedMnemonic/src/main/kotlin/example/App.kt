package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()

  val label1 = JLabel("Mail Address:", SwingConstants.RIGHT)
  label1.setDisplayedMnemonic('M')
  val textField1 = JTextField(12)
  label1.labelFor = textField1
  addRow(label1, textField1, p, c)

  val label2 = JLabel("Password:", SwingConstants.RIGHT)
  label2.setDisplayedMnemonic('P')
  val textField2 = JPasswordField(12)
  label2.labelFor = textField2
  addRow(label2, textField2, p, c)

  val label3 = JLabel("JLabel:", SwingConstants.RIGHT)
  val textField3 = JTextField(12)
  addRow(label3, textField3, p, c)

  val label4 = JLabel("ComboBox:", SwingConstants.RIGHT)
  label4.setDisplayedMnemonic('C')
  val comboBox = JComboBox<Any>()
  addRow(label4, comboBox, p, c)

  val button = JButton("JComboBox#requestFocusInWindow() Test")
  button.addActionListener { comboBox.requestFocusInWindow() }

  return JPanel(BorderLayout()).also {
    it.add(button, BorderLayout.SOUTH)
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addRow(c1: Component, c2: Component, p: Container, c: GridBagConstraints) {
  c.gridx = 0
  c.weightx = 0.0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.EAST
  p.add(c1, c)
  c.gridx = 1
  c.weightx = 1.0
  c.insets = Insets(5, 5, 5, 5)
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(c2, c)
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
