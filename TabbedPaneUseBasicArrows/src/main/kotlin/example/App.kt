package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabs = JTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  for (i in 1 until 100) {
    tabs.addTab("title$i", JLabel("label$i"))
  }

  val key = "TabbedPane.useBasicArrows"
  val check = JCheckBox(key, UIManager.getBoolean(key))
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    UIManager.put(key, b)
    SwingUtilities.updateComponentTreeUI(tabs)
  }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
