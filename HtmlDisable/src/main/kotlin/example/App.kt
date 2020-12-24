package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label1 = JLabel()
  label1.putClientProperty("html.disable", true)

  val button1 = JButton()
  button1.putClientProperty("html.disable", true)
  label1.text = "<html><font color=red>Html l1</font></html>"
  button1.text = "<html><font color=red>Html b1</font></html>"
  label1.toolTipText = "<html>&lt;html&gt;&lt;font color=red&gt;Html&lt;/font&gt;&lt;/html&gt;</html>"
  button1.toolTipText = "<html><font color=red>Html</font></html>"

  val label2 = JLabel()
  label2.text = "<html><font color=red>Html l2</font></html>"

  val button2 = JButton()
  button2.text = "<html><font color=red>Html b2</font></html>"

  val box = Box.createVerticalBox()
  box.add(label1)
  box.add(Box.createVerticalStrut(2))
  box.add(button1)
  box.add(Box.createVerticalStrut(20))
  box.add(label2)
  box.add(Box.createVerticalStrut(2))
  box.add(button2)

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(20, 5, 20, 5)
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
