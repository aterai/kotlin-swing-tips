package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val KEY = "TabbedPane:TabbedPaneTabArea.contentMargins"

fun makeUI(): Component {
  val tabbedPane = makeTabbedPane()
  val d = UIDefaults()
  d[KEY] = Insets(3, 30, 4, 30)
  tabbedPane.putClientProperty("Nimbus.Overrides", d)
  tabbedPane.putClientProperty("Nimbus.Overrides.InheritDefaults", true)

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTabbedPane())
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane() = JTabbedPane().also {
  // it.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
  it.addTab("JTree", JScrollPane(JTree()))
  it.addTab("JSplitPane", JSplitPane())
  it.addTab("JTextArea", JScrollPane(JTextArea()))
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      // UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
      // UIManager.put("TabbedPane.tabAreaInsets", new Insets(10, 10, 2, 10));

      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      val d = UIManager.getLookAndFeelDefaults()
      // d.put("TabbedPane:TabbedPaneContent.contentMargins", new Insets(0, 5, 5, 5));
      // d.put("TabbedPane:TabbedPaneTab.contentMargins", new Insets(2, 8, 3, 8));
      // d.put("TabbedPane:TabbedPaneTabArea.contentMargins", new Insets(3, 10, 4, 10));
      val i = d.getInsets(KEY)
      d.put(KEY, Insets(i.top, 0, i.bottom, 0))
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
