package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val check = JCheckBox("Always On Top", true)
    check.addActionListener { e ->
      val c = e.getSource() as? JCheckBox ?: return@addActionListener
      (c.getTopLevelAncestor() as? Window)?.setAlwaysOnTop(c.isSelected())
    }

    val p = JPanel()
    p.setBorder(BorderFactory.createTitledBorder("JFrame#setAlwaysOnTop(boolean)"))
    p.add(check)
    add(p, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setAlwaysOnTop(true)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
