package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun createUI(): Component {
  val list = listOf(
    createTabbedPane(JTabbedPane()),
    createTabbedPane(ClippedTitleTabbedPane()),
  )
  val p = JPanel(GridLayout(list.size, 1))
  list.forEach { p.add(it) }
  val check = JCheckBox("LEFT")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected ?: false
    val tabPlacement = if (b) JTabbedPane.LEFT else JTabbedPane.TOP
    list.forEach { it.tabPlacement = tabPlacement }
  }
  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTabbedPane(tabbedPane: JTabbedPane) = tabbedPane.also {
  it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  it.addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
  it.addTab("2", ColorIcon(Color.GREEN), JLabel("JLabel 1"))
  it.addTab("33333333333333", ColorIcon(Color.BLUE), JScrollPane(JTree()))
  it.addTab("444444444444444", ColorIcon(Color.ORANGE), JLabel("JLabel 2"))
  it.addTab("55555555555555555555555555555555", ColorIcon(Color.CYAN), JLabel("e"))
}

private class ClippedTitleTabbedPane : JTabbedPane() {
  private val tabInsets = UIManager.getInsets("TabbedPane.tabInsets")
    ?: getSynthTabInsets()
  private val tabAreaInsets = UIManager.getInsets("TabbedPane.tabAreaInsets")
    ?: getSynthTabAreaInsets()

  private fun getSynthTabInsets(): Insets {
    val region = Region.TABBED_PANE_TAB
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  private fun getSynthTabAreaInsets(): Insets {
    val region = Region.TABBED_PANE_TAB_AREA
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  override fun doLayout() {
    val tabCount = tabCount
    if (tabCount > 0 && isVisible) {
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
      updateAllTabWidths(tabWidth, gap)
    }
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
    setTabComponentAt(index, JLabel(title, icon, CENTER))
  }

  private fun updateAllTabWidths(
    tabWidth: Int,
    gap: Int,
  ) {
    for (i in 0..<tabCount) {
      val tab = getTabComponentAt(i)
      if (tab is JComponent) {
        val d = tab.getPreferredSize()
        d.width = if (i < gap) tabWidth + 1 else tabWidth
        tab.preferredSize = d
      }
    }
  }
}

private class ColorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
