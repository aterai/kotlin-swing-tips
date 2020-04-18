package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val frame = JFrame("Test - JFrame")
  frame.defaultCloseOperation = WindowConstants.HIDE_ON_CLOSE
  frame.setSize(240, 240)
  val center = JButton("frame.setLocationRelativeTo(null)")
  center.addActionListener {
    if (frame.isVisible) {
      return@addActionListener
    }
    frame.setLocationRelativeTo(null)
    frame.isVisible = true
  }
  val relative = JButton("frame.setLocationRelativeTo(button)")
  relative.addActionListener { e ->
    if (frame.isVisible) {
      return@addActionListener
    }
    frame.setLocationRelativeTo(e.source as? Component)
    frame.isVisible = true
  }
  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.add(makeTitledPanel("in center of screen", center))
    it.add(makeTitledPanel("relative to this button", relative))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
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
