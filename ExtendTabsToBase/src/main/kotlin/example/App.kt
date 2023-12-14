package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = makeTabbedPane()
  val d = UIManager.getLookAndFeelDefaults()
  d["TabbedPane.extendTabsToBase"] = false
  tabbedPane.putClientProperty("Nimbus.Overrides", d)
  tabbedPane.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("TabbedPane.extendTabsToBase: true", makeTabbedPane()))
    it.add(makeTitledPanel("TabbedPane.extendTabsToBase: false", tabbedPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane() = JTabbedPane().also {
  it.addTab("JTable", JScrollPane(JTable(8, 3)))
  it.addTab("JTree", JScrollPane(JTree()))
  it.addTab("JLabel", JLabel("label"))
  it.addTab("JButton", JButton("button"))
  it.addTab("JSplitPane", JSplitPane())
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
