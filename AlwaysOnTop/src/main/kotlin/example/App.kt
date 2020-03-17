package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("Always On Top", true)
  check.addActionListener { e ->
    val c = e.getSource() as? JCheckBox ?: return@addActionListener
    (c.getTopLevelAncestor() as? Window)?.setAlwaysOnTop(c.isSelected())
  }

  val p = JPanel().also {
    it.setBorder(BorderFactory.createTitledBorder("JFrame#setAlwaysOnTop(boolean)"))
    it.add(check)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.setPreferredSize(Dimension(320, 240))
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
      setAlwaysOnTop(true)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
