package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val focusHandler = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      (e.component as? JLabel)?.labelFor?.requestFocusInWindow()
    }
  }

  val label1 = JLabel("Mail Address:", SwingConstants.RIGHT)
  label1.addMouseListener(focusHandler)
  label1.setDisplayedMnemonic('M')
  val textField1 = JTextField(12)
  label1.labelFor = textField1

  val label2 = JLabel("Password:", SwingConstants.RIGHT)
  label2.addMouseListener(focusHandler)
  label2.setDisplayedMnemonic('P')
  val textField2 = JPasswordField(12)
  label2.labelFor = textField2

  val label3 = JLabel("TextArea:", SwingConstants.RIGHT)
  label3.addMouseListener(focusHandler)
  label3.setDisplayedMnemonic('T')
  val textField3 = JTextArea(6, 12)
  label3.labelFor = textField3

  val p = JPanel(GridBagLayout())
  val c = GridBagConstraints()
  addRow(label1, textField1, p, c)
  addRow(label2, textField2, p, c)
  addRow(label3, JScrollPane(textField3), p, c)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addRow(c1: Component, c2: Component, p: Container, c: GridBagConstraints) {
  c.gridx = 0
  c.weightx = 0.0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.NORTHEAST
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
