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

fun createUI(): Component {
  val tabs = DayScheduleTabbedPane()
  updateCalendar(tabs, true)

  val check = JCheckBox("Center Today", true)
  check.addActionListener { updateCalendar(tabs, check.isSelected) }

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.add(check, BorderLayout.SOUTH)
    it.setBackground(ModernTabbedPaneUI.BG_DARK)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateCalendar(tabs: JTabbedPane, centerToday: Boolean) {
  tabs.removeAll()
  val locale = Locale.getDefault()
  val today = LocalDate.now(ZoneId.systemDefault())
  val startDay = getStartDay(today, locale, centerToday)
  var todayIndex = 0
  for (i in DayOfWeek.entries.indices) {
    val date = startDay.plusDays(i.toLong())
    tabs.addTab("", createTabContent(date))
    tabs.setTabComponentAt(i, createDayTab(date, today, locale))
    if (date == today) {
      todayIndex = i
    }
  }
  tabs.setSelectedIndex(todayIndex)
}

private fun getStartDay(today: LocalDate, locale: Locale, centerToday: Boolean): LocalDate {
  val firstDay = WeekFields.of(locale).firstDayOfWeek
  return if (centerToday) {
    today.minusDays((DayOfWeek.entries.size / 2).toLong())
  } else {
    today.with(TemporalAdjusters.previousOrSame(firstDay))
  }
}

private fun createTabContent(date: LocalDate): JPanel {
  val label = JLabel("Schedule for $date")
  label.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)
  val content = JPanel()
  content.setBackground(ModernTabbedPaneUI.BG_DARK)
  content.add(label)
  return content
}

private fun createDayTab(date: LocalDate, today: LocalDate, locale: Locale): JPanel {
  val dayName = date.getDayOfWeek().getDisplayName(TextStyle.SHORT, locale)
  val dayLabel = JLabel(dayName, SwingConstants.CENTER)
  dayLabel.setFont(dayLabel.getFont().deriveFont(11f))
  dayLabel.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)

  val dayOfMonth = date.dayOfMonth.toString()
  val dateLabel = JLabel(dayOfMonth, SwingConstants.CENTER)
  dateLabel.setFont(dateLabel.getFont().deriveFont(Font.BOLD, 18f))
  dateLabel.setForeground(ModernTabbedPaneUI.TEXT_PRIMARY)

  val panel = JPanel(BorderLayout(0, 2))
  panel.setOpaque(false)
  panel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
  panel.add(dayLabel, BorderLayout.NORTH)
  panel.add(dateLabel)
  panel.add(createIndicator(date, today), BorderLayout.SOUTH)
  return panel
}

private fun createIndicator(date: LocalDate, today: LocalDate): JLabel {
  // The transparent icon keeps the layout of the other tabs the same as today's tab
  val color = if (date == today) ModernTabbedPaneUI.ACCENT_TODAY else Color(0x0, true)
  return JLabel(IndicatorIcon(color, 20, 4, 2), SwingConstants.CENTER)
}

private class DayScheduleTabbedPane : JTabbedPane() {
  override fun updateUI() {
    super.updateUI()
    // Synth-based LookAndFeels (e.g. Nimbus) turn this off, which skips paintTabBackground(...)
    UIManager.put("TabbedPane.tabsOpaque", true)
    setUI(ModernTabbedPaneUI())
  }
}

private class IndicatorIcon(
  private val color: Color,
  private val width: Int,
  private val height: Int,
  private val radius: Int,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = color
    val diameter = radius * 2
    g2.fillOval(x + width / 2 - radius, y + height / 2 - radius, diameter, diameter)
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class ModernTabbedPaneUI : BasicTabbedPaneUI() {
  override fun calculateTabWidth(
    tabPlacement: Int,
    tabIndex: Int,
    metrics: FontMetrics,
  ): Int {
    val isTopBottom = tabPlacement == TOP || tabPlacement == BOTTOM
    return if (isTopBottom && tabPane.width > 0) {
      calculateEvenTabWidth(tabPlacement, tabIndex)
    } else {
      super.calculateTabWidth(tabPlacement, tabIndex, metrics)
    }
  }

  private fun calculateEvenTabWidth(tabPlacement: Int, tabIndex: Int): Int {
    val insets = tabPane.insets
    val areaInsets = getTabAreaInsets(tabPlacement)
    val horizontalInsets = insets.left + insets.right + areaInsets.left + areaInsets.right
    val areaWidth = tabPane.width - horizontalInsets
    val tabCount = tabPane.tabCount
    // Integer pixel distribution: the leading tabs take one pixel each of the remainder
    val extraPixel = if (tabIndex < areaWidth % tabCount) 1 else 0
    return areaWidth / tabCount + extraPixel
  }

  override fun getTabLabelShiftX(
    tabPlacement: Int,
    tabIndex: Int,
    isSelected: Boolean,
  ) = 0

  override fun getTabLabelShiftY(
    tabPlacement: Int,
    tabIndex: Int,
    isSelected: Boolean,
  ) = 0

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
    g2.color = if (isSelected) BG_TAB_SELECTED else BG_TAB_IDLE
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
    val BG_TAB_SELECTED = Color(0x2A_2A_3C)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
