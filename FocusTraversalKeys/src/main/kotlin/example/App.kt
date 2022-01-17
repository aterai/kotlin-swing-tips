package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val HELP = """FORWARD_TRAVERSAL_KEYS: TAB, RIGHT, DOWN
BACKWARD_TRAVERSAL_KEYS: SHIFT+TAB, LEFT, UP
"""

fun makeUI(): Component {
  val button = JButton("showOptionDialog")
  button.addActionListener { e ->
    when (JOptionPane.showConfirmDialog((e.source as? JComponent)?.rootPane, HELP)) {
      JOptionPane.YES_OPTION -> println("YES_OPTION")
      JOptionPane.NO_OPTION -> println("NO_OPTION")
      JOptionPane.CANCEL_OPTION -> println("CANCEL_OPTION")
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(JButton("111"))
  box.add(JButton("222"))
  box.add(button)
  box.add(JButton("333"))

  val focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager()
  val ftk = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS
  val forwardKeys = HashSet(focusManager.getDefaultFocusTraversalKeys(ftk))
  forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0))
  forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0))
  focusManager.setDefaultFocusTraversalKeys(ftk, forwardKeys)
  val btk = KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS
  val backwardKeys = HashSet(focusManager.getDefaultFocusTraversalKeys(btk))
  backwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0))
  backwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0))
  focusManager.setDefaultFocusTraversalKeys(btk, backwardKeys)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.defaultButton = button }
    it.isFocusCycleRoot = true
    it.focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
      override fun accept(c: Component) = c !is JTextArea && super.accept(c)

      override fun getDefaultComponent(container: Container) = it.rootPane.defaultButton
    }
    it.add(JScrollPane(JTextArea(HELP)))
    it.add(box, BorderLayout.SOUTH)
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
