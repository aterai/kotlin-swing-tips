package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val t = ClippedTitleTabbedPane()
  t.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  addTab(t, "JTree", ImageIcon(cl.getResource("example/wi0009-32.png")), JScrollPane(JTree()))
  addTab(t, "JTextArea", ImageIcon(cl.getResource("example/wi0054-32.png")), JScrollPane(JTextArea()))
  addTab(t, "Preference", ImageIcon(cl.getResource("example/wi0062-32.png")), JScrollPane(JTree()))
  addTab(t, "Help", ImageIcon(cl.getResource("example/wi0063-32.png")), JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    it.add(t)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addTab(tabbedPane: JTabbedPane, title: String, icon: Icon, c: Component) {
  tabbedPane.addTab(title, c)
  val label = JLabel(title, icon, SwingConstants.CENTER)
  label.verticalTextPosition = SwingConstants.BOTTOM
  label.horizontalTextPosition = SwingConstants.CENTER
  tabbedPane.setTabComponentAt(tabbedPane.tabCount - 1, label)
}

private class ClippedTitleTabbedPane : JTabbedPane() {
  private fun getSynthInsets(region: Region): Insets {
    val style = SynthLookAndFeel.getStyle(this, region)
    val context = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(context, null)
  }

  private val tabInsets: Insets
    get() = UIManager.getInsets("TabbedPane.tabInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB)

  private val tabAreaInsets: Insets
    get() = UIManager.getInsets("TabbedPane.tabAreaInsets") ?: getSynthInsets(Region.TABBED_PANE_TAB_AREA)

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabInsets = tabInsets
    val tabAreaInsets = tabAreaInsets
    val insets = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaInsets.left - tabAreaInsets.right - insets.left - insets.right
    var tabWidth: Int // = tabInsets.left + tabInsets.right + 3;
    val gap: Int
    if (tabPlacement == SwingConstants.LEFT || tabPlacement == SwingConstants.RIGHT) {
      tabWidth = areaWidth / 4
      gap = 0
    } else { // TOP || BOTTOM
      tabWidth = areaWidth / tabCount
      gap = areaWidth - tabWidth * tabCount
    }

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabInsets.left + tabInsets.right + 3
    updateAllTabWidth(tabWidth, gap)
    super.doLayout()
  }

  private fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until tabCount) {
      (getTabComponentAt(i) as? JComponent)?.also {
        val a = if (i == tabCount - 1) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, it.preferredSize.height)
        it.preferredSize = dim
        rest -= a
      }
    }
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
