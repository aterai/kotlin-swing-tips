package example

import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.text.JTextComponent

private const val MAX_LEN = 6

fun makeUI(): Component {
  val button = JButton("Next")
  button.isEnabled = false

  val p = JPanel(BorderLayout())
  p.focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
    override fun getComponentAfter(
      focusCycleRoot: Container,
      cmp: Component,
    ): Component {
      // println("getComponentAfter")
      button.isEnabled = isAllValid(p)
      return super.getComponentAfter(focusCycleRoot, cmp)
    }

    override fun getComponentBefore(
      focusCycleRoot: Container,
      cmp: Component,
    ): Component {
      // println("getComponentBefore")
      button.isEnabled = isAllValid(p)
      return super.getComponentBefore(focusCycleRoot, cmp)
    }
  }
  p.isFocusCycleRoot = true

  val check = JCheckBox("use FocusTraversalPolicy", true)
  check.addActionListener { e -> p.isFocusCycleRoot = (e.source as? JCheckBox)?.isSelected == true }

  val box = Box.createVerticalBox()
  box.add(check)
  box.border = BorderFactory.createTitledBorder("Max text length: $MAX_LEN")
  listOf(makeTextField(button, p), makeTextField(button, p)).forEach {
    box.add(Box.createVerticalStrut(10))
    box.add(it)
  }

  val pnl = JPanel(FlowLayout(FlowLayout.RIGHT))
  pnl.add(button)

  p.add(box, BorderLayout.NORTH)
  p.add(pnl, BorderLayout.SOUTH)
  p.preferredSize = Dimension(320, 240)
  return p
}

fun isAllValid(c: Container) = c.components
  .filterIsInstance<JTextField>()
  .all { it.inputVerifier.verify(it) }

fun makeTextField(
  button: JButton,
  root: Container,
): JTextField {
  val textField = JTextField(24)
  textField.inputVerifier = object : InputVerifier() {
    override fun verify(c: JComponent): Boolean {
      if (c is JTextComponent) {
        val str = c.text.trim()
        return str.isNotEmpty() && MAX_LEN - str.length >= 0
      }
      return false
    }

    override fun shouldYieldFocus(input: JComponent): Boolean {
      // println("shouldYieldFocus")
      button.isEnabled = isAllValid(root)
      return super.shouldYieldFocus(input)
    }
  }
  textField.addFocusListener(object : FocusAdapter() {
    override fun focusLost(e: FocusEvent) {
      if (e.isTemporary) {
        return
      }
      // println("focusLost")
      button.isEnabled = isAllValid(root)
    }
  })
  return textField
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
