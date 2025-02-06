package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(JTextField("Another focusable component"))
  box.add(Box.createVerticalStrut(5))

  val bg1 = ButtonGroup()
  box.add(makeButtonGroupPanel("Default", bg1))
  box.add(Box.createVerticalStrut(5))

  val bg2 = ButtonGroup()
  val buttons = makeButtonGroupPanel("FocusTraversalPolicy", bg2)
  buttons.isFocusTraversalPolicyProvider = true
  buttons.focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
    override fun getDefaultComponent(focusCycleRoot: Container): Component {
      val selection = bg2.selection
      return focusCycleRoot.components.first {
        (it as? JRadioButton)?.model == selection
      } ?: super.getDefaultComponent(focusCycleRoot)
    }
  }
  box.add(buttons)
  box.add(Box.createVerticalStrut(5))

  val clear = JButton("clear selection")
  clear.addActionListener {
    bg1.clearSelection()
    bg2.clearSelection()
  }

  val b = Box.createHorizontalBox()
  b.add(Box.createHorizontalGlue())
  b.add(clear)
  box.add(b)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeButtonGroupPanel(
  title: String,
  bg: ButtonGroup,
): Container {
  val p = JPanel()
  listOf("aaa", "bbb", "ccc", "ddd", "eee").forEach {
    val rb = JRadioButton(it, "ccc" == it)
    bg.add(rb)
    p.add(rb)
  }
  p.border = BorderFactory.createTitledBorder(title)
  return p
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
