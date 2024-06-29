package example

import java.awt.*
import java.awt.font.TextLayout
import javax.swing.*
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val list = listOf(
    makeTabbedPane(ClippedTitleTabbedPane()),
    makeTabbedPane(TextOverflowFadeTabbedPane()),
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

private fun makeTabbedPane(tabbedPane: JTabbedPane) = tabbedPane.also {
  it.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  it.addTab("1111111111111111111", ColorIcon(Color.RED), JScrollPane(JTree()))
  it.addTab("2", ColorIcon(Color.GREEN), JLabel("22222"))
  it.addTab("33333333333333", ColorIcon(Color.BLUE), JScrollPane(JTree()))
  it.addTab("444444444444444", ColorIcon(Color.ORANGE), JLabel("33333"))
  it.addTab("55555555555555555555555555555555", ColorIcon(Color.CYAN), JLabel("e"))
}

private open class ClippedTitleTabbedPane : JTabbedPane() {
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

  // constructor(tabPlacement: Int) : super(tabPlacement)

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
    setTabComponentAt(index, JLabel(title, icon, SwingConstants.LEADING))
  }

  private fun updateAllTabWidth(
    tabWidth: Int,
    gap: Int,
  ) {
    val dim = Dimension()
    var rest = gap
    for (i in 0..<tabCount) {
      val tab = getTabComponentAt(i) as? JComponent ?: continue
      val a = if (i == tabCount - 1) rest else 1
      val w = if (rest > 0) tabWidth + a else tabWidth
      dim.setSize(w, tab.preferredSize.height)
      tab.preferredSize = dim
      rest -= a
    }
  }
}

private open class TextOverflowFadeTabbedPane : ClippedTitleTabbedPane() {
  // constructor(tabPlacement: Int) : super(tabPlacement)

  override fun insertTab(
    title: String?,
    icon: Icon?,
    c: Component?,
    tip: String?,
    index: Int,
  ) {
    super.insertTab(title, icon, c, tip ?: title, index)
    val p = JPanel(BorderLayout(2, 0)).also {
      it.isOpaque = false
      it.add(JLabel(icon), BorderLayout.WEST)
      it.add(TextOverflowFadeLabel(title ?: ""))
    }
    setTabComponentAt(index, p)
  }
}

private class TextOverflowFadeLabel(text: String) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val i = insets
    val w = width - i.left - i.right
    val h = height - i.top - i.bottom
    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as? Graphics2D ?: return
    g2.font = g.font
    g2.paint = foreground

    // val frc = g2.getFontRenderContext()
    val tl = TextLayout(text, font, g2.fontRenderContext)
    val baseline = getBaseline(w, h).toFloat()
    val fx = i.left.toFloat()

    g2.clip = rect
    tl.draw(g2, fx, baseline)

    rect.width = 1
    var alpha = 1f
    for (x in w - LENGTH..<w) {
      rect.x = x
      alpha = maxOf(0f, alpha - DIFF)
      g2.composite = AlphaComposite.SrcOver.derive(alpha)
      g2.clip = rect
      tl.draw(g2, fx, baseline)
    }
    g2.dispose()
  }

  companion object {
    private const val LENGTH = 20
    private const val DIFF = .05f
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(1, 2, iconWidth - 2, iconHeight - 2)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
