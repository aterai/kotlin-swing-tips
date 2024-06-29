package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tabs = JTabbedPane(SwingConstants.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  for (i in 1..<100) {
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
