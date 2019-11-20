package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  box.add(JTextField("Another focusable component"))
  box.add(Box.createVerticalStrut(5))
  val bg1 = ButtonGroup()
  box.add(makeButtonGroupPanel("Default", bg1))
  box.add(Box.createVerticalStrut(5))
  val bg2 = ButtonGroup()
  val buttons = makeButtonGroupPanel("FocusTraversalPolicy", bg2)
  buttons.setFocusTraversalPolicyProvider(true)
  buttons.setFocusTraversalPolicy(object : LayoutFocusTraversalPolicy() {
    override fun getDefaultComponent(focusCycleRoot: Container): Component {
      val selection = bg2.getSelection()
      return focusCycleRoot.getComponents().first {
        (it as? JRadioButton)?.getModel() == selection
      } ?: super.getDefaultComponent(focusCycleRoot)
    }
  })
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

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.NORTH)
  p.setPreferredSize(Dimension(320, 240))
  return p
}

private fun makeButtonGroupPanel(title: String, bg: ButtonGroup): Container {
  val p = JPanel()
  listOf("aaa", "bbb", "ccc", "ddd", "eee").forEach {
    val rb = JRadioButton(it, "ccc" == it)
    bg.add(rb)
    p.add(rb)
  }
  p.setBorder(BorderFactory.createTitledBorder(title))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
