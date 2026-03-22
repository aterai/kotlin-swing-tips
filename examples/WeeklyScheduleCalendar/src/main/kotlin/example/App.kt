package example

import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI
import javax.swing.plaf.synth.Region
import javax.swing.plaf.synth.SynthConstants
import javax.swing.plaf.synth.SynthContext
import javax.swing.plaf.synth.SynthLookAndFeel

fun makeUI(): Component {
  val tabs = ClippedTitleTabbedPane()
  val locale = Locale.getDefault()
  val today = LocalDate.now(ZoneId.systemDefault())
  val firstDay = WeekFields.of(locale).firstDayOfWeek
  val startOfWeek = today.with(TemporalAdjusters.previousOrSame(firstDay))
  for (i in DayOfWeek.entries.toTypedArray().indices) {
    val date = startOfWeek.plusDays(i.toLong())
    tabs.addTab("", makeTabContent(date))
    tabs.setTabComponentAt(i, makeDayTab(date, today, locale))
  }
  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.setBackground(ModernTabbedPaneUI.BG_DARK)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabContent(date: LocalDate?): JPanel {
  val label = JLabel("Schedule for $date")
  label.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)
  val content = JPanel()
  content.setBackground(ModernTabbedPaneUI.BG_DARK)
  content.add(label)
  return content
}

private fun makeDayTab(date: LocalDate, today: LocalDate?, locale: Locale): JPanel {
  val dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
  val lblDay = JLabel(dayName, SwingConstants.CENTER)
  lblDay.setFont(lblDay.getFont().deriveFont(11f))
  lblDay.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)

  val dayOfMonth = date.dayOfMonth.toString()
  val lblDate = JLabel(dayOfMonth, SwingConstants.CENTER)
  lblDate.setFont(lblDate.getFont().deriveFont(Font.BOLD, 18f))
  lblDate.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)

  val panel = JPanel(BorderLayout(0, 2))
  panel.setOpaque(false)
  panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
  panel.add(lblDay, BorderLayout.NORTH)
  panel.add(lblDate)
  panel.add(makeIndicator(date, today), BorderLayout.SOUTH)
  return panel
}

private fun makeIndicator(date: LocalDate, today: LocalDate?): JLabel {
  val iw = 20
  val ih = 4
  val ir = 2
  val lblIndicator = JLabel("", SwingConstants.CENTER)
  if (date == today) {
    val indicatorColor = ModernTabbedPaneUI.ACCENT_TODAY
    val activeIcon: Icon = IndicatorIcon(indicatorColor, iw, ih, ir)
    lblIndicator.setIcon(activeIcon)
  } else {
    val inactiveColor = Color(0x0, true)
    val inactiveIcon: Icon = IndicatorIcon(inactiveColor, iw, ih, ir)
    lblIndicator.setIcon(inactiveIcon)
  }
  return lblIndicator
}

private class ClippedTitleTabbedPane : JTabbedPane() {
  override fun updateUI() {
    super.updateUI()
    UIManager.put("TabbedPane.selectedLabelShift", 0)
    UIManager.put("TabbedPane.labelShift", 0)
    setUI(ModernTabbedPaneUI())
  }

  private fun getSynthInsets(region: Region): Insets {
    val style = SynthLookAndFeel.getStyle(this, region)
    val ctx = SynthContext(this, region, style, SynthConstants.ENABLED)
    return style.getInsets(ctx, null)
  }

  private fun getTabInsets() =
    UIManager.getInsets("TabbedPane.tabInsets")
      ?: getSynthInsets(Region.TABBED_PANE_TAB)

  private fun getTabAreaInsets() =
    UIManager.getInsets("TabbedPane.tabAreaInsets")
      ?: getSynthInsets(Region.TABBED_PANE_TAB_AREA)

  override fun doLayout() {
    val tabCount = getTabCount()
    if (tabCount > 0 && isVisible) {
      val tabIns = getTabInsets()
      val areaIns = getTabAreaInsets()
      val i = getInsets()
      val tabPlacement = getTabPlacement()
      val areaWidth = getWidth() - areaIns.left - areaIns.right - i.left - i.right
      val isTopBottom = tabPlacement == TOP || tabPlacement == BOTTOM
      var tabWidth = if (isTopBottom) areaWidth / tabCount else areaWidth / 4
      val gap = if (isTopBottom) areaWidth - tabWidth * tabCount else 0
      tabWidth -= tabIns.left + tabIns.right + 3
      updateAllTabWidth(tabWidth, gap)
    }
    super.doLayout()
  }

  private fun updateAllTabWidth(tabWidth: Int, gap: Int) {
    val dim = Dimension()
    var rest = gap
    val count = tabCount
    for (i in 0..<count) {
      val tab = getTabComponentAt(i)
      if (tab is JComponent) {
        val a = if (i == count - 1) rest else 1
        val w = if (rest > 0) tabWidth + a else tabWidth
        dim.setSize(w, tab.getPreferredSize().height)
        tab.preferredSize = dim
        rest -= a
      }
    }
  }
}

private class IndicatorIcon(
  private val color: Color,
  private val width: Int,
  private val height: Int,
  private val arcRadius: Int,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = color
    if (c is JComponent) {
      val r = SwingUtilities.calculateInnerArea(c, null)
      val ox = r.centerX.toInt() - arcRadius
      val oy = r.centerY.toInt() - arcRadius
      val arcDiameter = arcRadius * 2
      g2.fillOval(ox, oy, arcDiameter, arcDiameter)
    }
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class ModernTabbedPaneUI : BasicTabbedPaneUI() {
  override fun paintTabBackground(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = if (isSelected) BG_TAB_SEL else BG_TAB_IDLE
    val arc = 12f
    g2.fill(RoundRectangle2D.Float(x + 2f, y + 2f, w - 4f, h - 2f, arc, arc))
    if (isSelected) {
      g2.color = ACCENT
      g2.stroke = BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
      g2.drawLine(x + arc.toInt(), y + h - 1, x + w - arc.toInt() - 1, y + h - 1)
    }
    g2.dispose()
  }

  override fun paintContentBorder(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
  ) {
    // empty paint
  }

  override fun paintTabBorder(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    // empty paint
  }

  override fun paintFocusIndicator(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    // empty paint
  }

  companion object {
    val BG_DARK = Color(0x0F_0F_13)
    val BG_TAB_IDLE = Color(0x22_22_2E)
    val BG_TAB_SEL = Color(0x2A_2A_3C)
    val ACCENT = Color(0x7C_6A_FF)
    val ACCENT_TODAY = Color(0xFF_6B_6B)
    val TEXT_PRIMARY = Color(0xF0_F0_F8)
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
