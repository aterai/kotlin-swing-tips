package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tgtIndex = 1
  val tabs = JTabbedPane()

  val check = JCheckBox("Enable Advanced")
  check.addActionListener { e ->
    tabs.setEnabledAt(tgtIndex, (e.source as? JCheckBox)?.isSelected == true)
  }

  tabs.addTab("Preferences", check)
  tabs.addTab("Advanced", JScrollPane(JTree()))
  tabs.setEnabledAt(tgtIndex, false)

  return JPanel(BorderLayout()).also {
    it.add(tabs)
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
