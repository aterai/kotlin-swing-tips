package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val al = ActionListener { e ->
    (e.source as? JButton)?.also {
      it.mnemonic = when {
        it.mnemonic < KeyEvent.VK_A -> KeyEvent.VK_A
        it.mnemonic < KeyEvent.VK_L -> it.mnemonic + 1
        else -> 0
      }
    }
  }
  val button1 = JButton("Hello World")
  button1.addPropertyChangeListener { e ->
    val prop = e.propertyName
    (e.source as? JButton)?.also {
      if (AbstractButton.MNEMONIC_CHANGED_PROPERTY == prop) {
        it.toolTipText = "tooltip (Alt+${KeyEvent.getKeyText(it.mnemonic)})"
      }
    }
  }
  button1.addActionListener(al)
  button1.mnemonic = KeyEvent.VK_E
  button1.toolTipText = "tooltip (Alt+${KeyEvent.getKeyText(button1.mnemonic)})"

  val button2 = object : JButton("abc def ghi jkl") {
    override fun createToolTip() = MnemonicToolTip().also {
      it.component = this
    }
  }
  button2.addActionListener(al)
  button2.mnemonic = KeyEvent.VK_A
  button2.toolTipText = "tooltip"

  val box = Box.createVerticalBox()
  box.add(button1)
  box.add(Box.createVerticalStrut(20))
  box.add(button2)
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(20, 40, 20, 40)
    it.preferredSize = Dimension(320, 240)
  }
}

private class MnemonicToolTip : JToolTip() {
  private val mnemonicLabel = JLabel()

  init {
    layout = BorderLayout()
    mnemonicLabel.foreground = Color.GRAY
    mnemonicLabel.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
    add(mnemonicLabel, BorderLayout.EAST)
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    if (mnemonicLabel.isVisible) {
      d.width += mnemonicLabel.preferredSize.width
    }
    return d
  }

  override fun setComponent(c: JComponent) {
    if (c is AbstractButton) {
      val mnemonic = c.mnemonic
      if (mnemonic > 0) {
        mnemonicLabel.isVisible = true
        mnemonicLabel.text = "Alt+" + KeyEvent.getKeyText(mnemonic)
      } else {
        mnemonicLabel.isVisible = false
      }
    }
    super.setComponent(c)
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
