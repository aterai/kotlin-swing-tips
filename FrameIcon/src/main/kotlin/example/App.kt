package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = Toolkit.getDefaultToolkit().createImage(cl.getResource("example/16x16.png"))
  val check = JCheckBox("setIconImage")
  check.addActionListener { e ->
    (e.source as? JCheckBox)?.also {
      (it.topLevelAncestor as? Window)?.setIconImage(if (it.isSelected) icon else null)
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("Window#setIconImage(Image)")
  p.add(check)

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
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
