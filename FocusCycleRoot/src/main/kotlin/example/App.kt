package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("left")
  repeat(6) {
    p1.add(JTextField(16))
  }
  p1.isFocusCycleRoot = true

  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("right")
  repeat(6) {
    p2.add(JTextField(16))
  }
  p2.isFocusTraversalPolicyProvider = true
  p2.isFocusCycleRoot = true
  p2.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, emptySet())
  p2.setFocusTraversalKeys(
    KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS,
    HashSet(listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)))
  )
  p2.setFocusTraversalKeys(
    KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS,
    HashSet(listOf(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)))
  )
  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(p1)
    it.add(p2)
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
