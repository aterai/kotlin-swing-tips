package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val check = JCheckBox("Always On Top", true)
  check.addActionListener { e ->
    (e.source as? JCheckBox)?.also {
      (it.topLevelAncestor as? Window)?.isAlwaysOnTop = it.isSelected
    }
  }

  val p = JPanel().also {
    it.border = BorderFactory.createTitledBorder("JFrame#setAlwaysOnTop(boolean)")
    it.add(check)
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
      isAlwaysOnTop = true
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
