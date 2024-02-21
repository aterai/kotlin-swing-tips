package example

import java.awt.*
import java.net.URL
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val tabs = ClippedTitleTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT

  val cl = Thread.currentThread().contextClassLoader
  val titles = listOf(
    TabTitle("JTree", cl.getResource("example/wi0009-32.png")),
    TabTitle("JTextArea", cl.getResource("example/wi0054-32.png")),
    TabTitle("JTable", cl.getResource("example/wi0062-32.png")),
    TabTitle("JSplitPane", cl.getResource("example/wi0063-32.png")),
  )
  addTab(tabs, titles[0], JScrollPane(JTree()))
  addTab(tabs, titles[1], JScrollPane(JTextArea()))
  addTab(tabs, titles[2], JScrollPane(JTable(8, 3)))
  addTab(tabs, titles[3], JScrollPane(JSplitPane()))
  tabs.selectedIndex = -1
  EventQueue.invokeLater { tabs.selectedIndex = 0 }

  tabs.addChangeListener { e ->
    val tabbedPane = e.source
    if (tabbedPane is JTabbedPane && tabbedPane.tabCount > 0) {
      val idx = tabbedPane.selectedIndex
      for (i in 0 until tabbedPane.tabCount) {
        tabbedPane.getTabComponentAt(i).also {
          (it as? JLabel)?.text = if (i == idx) tabbedPane.getTitleAt(i) else null
        }
      }
    }
  }

  tabs.preferredSize = Dimension(320, 240)
  return tabs
}

private fun addTab(
  tabbedPane: JTabbedPane,
  tt: TabTitle,
  c: Component,
) {
  tabbedPane.addTab(tt.title, c)
  val url = tt.url
  val icon = if (url != null) ImageIcon(url) else UIManager.getIcon("html.missingImage")
  val label = object : JLabel(null, icon, CENTER) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = icon.iconHeight + tabbedPane.font.size + iconTextGap
      return d
    }
  }
  label.verticalTextPosition = SwingConstants.BOTTOM
  label.horizontalTextPosition = SwingConstants.CENTER
  tabbedPane.setTabComponentAt(tabbedPane.tabCount - 1, label)
}

private data class TabTitle(val title: String, val url: URL?)

private class ClippedTitleTabbedPane : JTabbedPane() {
  private val tabInsets = UIManager.getInsets("TabbedPane.tabInsets")
    ?: getSynthTabInsets()
  private val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets")
    ?: getSynthTabAreaInsets()

  private fun getSynthTabInsets(): Insets {
    val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB)
    val ctx = SynthContext(this, Region.TABBED_PANE_TAB, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  private fun getSynthTabAreaInsets(): Insets {
    val style = SynthLookAndFeel.getStyle(this, Region.TABBED_PANE_TAB_AREA)
    val ctx = SynthContext(this, Region.TABBED_PANE_TAB_AREA, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount == 0 || !isVisible) {
      super.doLayout()
      return
    }
    val tabIns = tabInsets
    val tabAreaIns = tabAreaInsets
    val ins = insets
    val tabPlacement = getTabPlacement()
    val areaWidth = width - tabAreaIns.left - tabAreaIns.right - ins.left - ins.right
    val isSide = tabPlacement == LEFT || tabPlacement == RIGHT
    var tabWidth = if (isSide) areaWidth / 4 else areaWidth / tabCount
    val gap = if (isSide) 0 else areaWidth - tabWidth * tabCount

    // "3" is magic number @see BasicTabbedPaneUI#calculateTabWidth
    tabWidth -= tabIns.left + tabIns.right + 3
    updateAllTabWidth(tabWidth, gap)

    super.doLayout()
  }

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip ?: title, index)
    setTabComponentAt(index, JLabel(title, icon, SwingConstants.CENTER))
  }

  private fun updateAllTabWidth(
    tabWidth: Int,
    gap: Int,
  ) {
    val dim = Dimension()
    var rest = gap
    for (i in 0 until tabCount) {
      val tab = getTabComponentAt(i) as? JComponent ?: continue
      val a = if (i == tabCount - 1) rest else 1
      val w = if (rest > 0) tabWidth + a else tabWidth
      dim.setSize(w, tab.preferredSize.height)
      tab.preferredSize = dim
      rest -= a
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      UIManager.put("TabbedPane.tabInsets", Insets(8, 2, 2, 2))
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
