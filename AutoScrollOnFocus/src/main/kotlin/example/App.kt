package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val box = makeTestBox()
  box.focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
    override fun getComponentAfter(focusCycleRoot: Container, cmp: Component): Component {
      val c = super.getComponentAfter(focusCycleRoot, cmp)
      (focusCycleRoot as? JComponent)?.scrollRectToVisible(c.bounds)
      return c
    }

    override fun getComponentBefore(focusCycleRoot: Container, cmp: Component): Component {
      val c = super.getComponentBefore(focusCycleRoot, cmp)
      (focusCycleRoot as? JComponent)?.scrollRectToVisible(c.bounds)
      return c
    }
  }
  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(JScrollPane(makeTestBox()))
    it.add(JScrollPane(box))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestBox(): Box {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  for (i in 0 until 20) {
    box.add(JTextField("test$i"))
    box.add(Box.createVerticalStrut(5))
  }
  box.add(Box.createVerticalGlue())
  box.isFocusCycleRoot = true
  return box
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
