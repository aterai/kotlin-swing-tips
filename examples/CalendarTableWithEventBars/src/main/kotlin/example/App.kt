package example

import java.awt.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.chrono.Chronology
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Arrays
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.min

fun makeUI(): Component {
  val date = LocalDate.now(ZoneId.systemDefault())
  val calendarTable = CalendarTable()
  calendarTable.currentLocalDate = date
  calendarTable.setModel(CalendarViewTableModel(date))
  val currentMonth = YearMonth.from(date)
  val events = makeSampleEvents(currentMonth)
  val layer = JLayer<JTable>(calendarTable, EventBarLayerUI(events))
  val scroll: JScrollPane = object : JScrollPane(layer) {
    override fun updateUI() {
      super.updateUI()
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS)
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    }
  }
  val comp = JScrollPane(makeLegendPanel(currentMonth, events))
  val split = JSplitPane(JSplitPane.VERTICAL_SPLIT, scroll, comp)
  split.setResizeWeight(.8)
  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSampleEvents(ym: YearMonth): MutableList<EventPeriod> {
  val events = ArrayList<EventPeriod>()
  // Event 1: 3-day meeting
  events.add(
    EventPeriod(
      "Project Meeting",
      LocalDate.of(ym.year, ym.month, 5),
      LocalDate.of(ym.year, ym.month, 7),
      Color(100, 150, 255, 180),
    ),
  )

  // Event 2: 1-week training (overlaps with Event 1)
  events.add(
    EventPeriod(
      "New Employee Training",
      LocalDate.of(ym.year, ym.month, 6),
      LocalDate.of(ym.year, ym.month, 12),
      Color(255, 180, 100, 180),
    ),
  )

  // Event 3: 2-day event
  events.add(
    EventPeriod(
      "Exhibition",
      LocalDate.of(ym.year, ym.month, 20),
      LocalDate.of(ym.year, ym.month, 21),
      Color(150, 255, 150, 180),
    ),
  )

  // Event 4: Long-term task until month-end
  events.add(
    EventPeriod(
      "Year-End Processing",
      LocalDate.of(ym.year, ym.month, 18),
      LocalDate.of(ym.year, ym.month, ym.lengthOfMonth()),
      Color(255, 150, 200, 180),
    ),
  )

  // Event 5: Another task overlapping with Event 4
  events.add(
    EventPeriod(
      "System Maintenance",
      LocalDate.of(ym.year, ym.month, 22),
      LocalDate.of(ym.year, ym.month, 26),
      Color(200, 150, 255, 180),
    ),
  )
  return events
}

private fun makeLegendPanel(
  currentMonth: YearMonth,
  events: MutableList<EventPeriod>,
): JPanel {
  val locale = Locale.getDefault()
  val fmt = CalendarUtils.getLocalizedYearMonthFormatter(locale)
  val txt = currentMonth.format(fmt.withLocale(locale))
  val title = CalendarUtils.getLocalizedYearMonthText(txt)
  val panel = JPanel(FlowLayout(FlowLayout.LEFT))
  panel.setBorder(BorderFactory.createTitledBorder(title))
  events.forEach { ev ->
    val label = JLabel(
      String.format(
        "%s (%d/%d-%d/%d)",
        ev.name,
        ev.startDate.monthValue,
        ev.startDate.dayOfMonth,
        ev.endDate.monthValue,
        ev.endDate.dayOfMonth,
      ),
    )
    label.setOpaque(true)
    label.setBackground(ev.color)
    label.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3))
    panel.add(label)
  }
  return panel
}

private class CalendarTable : JTable() {
  var currentLocalDate: LocalDate? = null

  override fun updateUI() {
    super.updateUI()
    setDefaultRenderer(LocalDate::class.java, CalendarCellRenderer())
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    setCellSelectionEnabled(true)
    setFillsViewportHeight(true)
    setRowHeight(64)
    val header = getTableHeader()
    header.setResizingAllowed(false)
    header.setReorderingAllowed(false)
    val headerRenderer = header.defaultRenderer
    if (headerRenderer is JLabel) {
      (headerRenderer as? JLabel)?.setHorizontalAlignment(SwingConstants.CENTER)
    }
  }
}

private class CalendarCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component? {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      false,
      false,
      row,
      column,
    )
    if (c is JLabel && table is CalendarTable && value is LocalDate) {
      c.setHorizontalAlignment(CENTER)
      c.setVerticalAlignment(TOP)
      val isToday = value == LocalDate.now(ZoneId.systemDefault())
      val day = value.dayOfMonth
      val txt = if (isToday) getCircledNumber(day) else day.toString()
      c.setText(txt)
      table.currentLocalDate?.also {
        c.setForeground(getDayOfWeekColor(value, it, isToday))
      }
    }
    return c
  }

  private fun getDayOfWeekColor(
    date: LocalDate,
    currentDate: LocalDate,
    isToday: Boolean,
  ): Color {
    val color: Color
    val isCurrentMonth = date.month == currentDate.month
    if (isCurrentMonth) {
      val dow = date.getDayOfWeek()
      color = if (isToday) {
        Color(255, 100, 0)
      } else if (dow == DayOfWeek.SUNDAY) {
        Color(255, 100, 100)
      } else if (dow == DayOfWeek.SATURDAY) {
        Color(100, 100, 255)
      } else {
        Color.BLACK
      }
    } else {
      color = Color.LIGHT_GRAY
    }
    return color
  }

  /**
   * Convert numbers to circled numbers.
   */
  private fun getCircledNumber(number: Int) = when (number) {
    in 1..20 -> (0x2460 + number - 1).toChar().toString()
    in 21..31 -> (0x3251 + number - 21).toChar().toString()
    else -> number.toString()
  }
}

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields: WeekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getColumnClass(column: Int): Class<*> {
    return LocalDate::class.java
  }

  override fun getColumnName(column: Int): String {
    return weekFields.firstDayOfWeek.plus(column.toLong())
      .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())
  }

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(row: Int, column: Int): Any? =
    startDate.plusDays(row.toLong() * columnCount + column)

  override fun isCellEditable(row: Int, column: Int) = false

  companion object {
    const val WEEKS = 6
  }
}

private class EventPeriod(
  val name: String,
  val startDate: LocalDate,
  val endDate: LocalDate,
  val color: Color,
) {
  var track: Int = 0 // Track Number (for duplicate avoidance)
}

private class EventBarLayerUI(
  private val events: MutableList<EventPeriod>,
) : LayerUI<JTable?>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val table = (c as? JLayer<*>)?.getView() as? JTable ?: return

    // Assign tracks (lanes) to events
    assignTracksToEvents()

    // Draw a color bar for each event
    for (ev in events) {
      drawEventBars(g2, table, ev)
    }
    g2.dispose()
  }

  /**
   * Assign track numbers to overlapping events.
   */
  private fun assignTracksToEvents() {
    val tracks = IntArray(events.size)
    val usedTracks = BooleanArray(events.size)
    for (i in events.indices) {
      val event = events[i]
      Arrays.fill(usedTracks, false)
      // Check for overlap with already processed events
      for (j in 0..<i) {
        val other = events[j]
        if (isOverlapping(event, other)) {
          usedTracks[tracks[j]] = true
        }
      }
      // Assign the smallest available track number
      var track = 0
      while (track < usedTracks.size && usedTracks[track]) {
        track++
      }
      tracks[i] = track
      event.track = track
    }
  }

  /**
   * Check if two event periods overlap.
   */
  private fun isOverlapping(e1: EventPeriod, e2: EventPeriod): Boolean {
    val b1 = e1.endDate.isBefore(e2.startDate)
    val b2 = e2.endDate.isBefore(e1.startDate)
    return !(b1 || b2)
  }

  private fun drawEventBars(g2: Graphics2D, table: JTable, event: EventPeriod) {
    val calendarStartDate = table.model.getValueAt(0, 0)
    if (calendarStartDate is LocalDate) {
      val daysInTable = DayOfWeek.entries.size * CalendarViewTableModel.WEEKS
      var current = event.startDate
      while (!current.isAfter(event.endDate)) {
        val sinceStart = ChronoUnit.DAYS.between(calendarStartDate, current)
        if (sinceStart in 0..<daysInTable) {
          val consecutiveDays = getConsecutiveDaysAndPaintBar(g2, table, event, current)
          current = current.plusDays(consecutiveDays.toLong())
        } else {
          current = current.plusDays(1)
        }
      }
    }
  }

  private fun drawEventBar(g2: Graphics2D, event: EventPeriod, barRect: Rectangle) {
    val clr = event.color
    g2.color = clr
    g2.fillRoundRect(barRect.x, barRect.y, barRect.width, barRect.height, 5, 5)
    g2.color = clr.darker()
    g2.drawRoundRect(barRect.x, barRect.y, barRect.width, barRect.height, 5, 5)
    val b = barRect.width > 60
    if (b) {
      drawBarTitle(g2, event, barRect)
    }
  }

  private fun getConsecutiveDaysAndPaintBar(
    g2: Graphics2D,
    tbl: JTable,
    ev: EventPeriod,
    cur: LocalDate,
  ): Int {
    val calendarStartDate = tbl.model.getValueAt(0, 0) as? LocalDate ?: return 0
    val sinceStart = ChronoUnit.DAYS.between(calendarStartDate, cur)
    val trackOffset = ev.track * (BAR_HEIGHT + BAR_MARGIN)
    val headerHeight = tbl.getTableHeader().getHeight()
    val daysInWeek = DayOfWeek.entries.size.toLong()
    val weekRow = (sinceStart / daysInWeek).toInt()
    val dayCol = (sinceStart % daysInWeek).toInt()

    var consecutiveDays = 1
    var nextDay = cur.plusDays(1)
    val notEndOfWeek = dayCol.toLong() != daysInWeek - 1
    while (!nextDay.isAfter(ev.endDate) && notEndOfWeek) {
      consecutiveDays++
      nextDay = nextDay.plusDays(1)
      if (dayCol + consecutiveDays >= daysInWeek) {
        break
      }
    }

    val firstRect = tbl.getCellRect(weekRow, dayCol, false)
    val lastRect = tbl.getCellRect(weekRow, dayCol + consecutiveDays - 1, false)

    val barX = firstRect.x + 5
    val barY = firstRect.y + trackOffset + headerHeight
    val barWidth = lastRect.x + lastRect.width - firstRect.x - 10
    drawEventBar(g2, ev, Rectangle(barX, barY, barWidth, BAR_HEIGHT))
    return consecutiveDays
  }

  private fun drawBarTitle(g2: Graphics2D, event: EventPeriod, rect: Rectangle) {
    g2.color = Color.BLACK
    g2.font = g2.font.deriveFont(9f)
    val fm = g2.fontMetrics
    var eventName = event.name
    val textWidth = fm.stringWidth(eventName)
    if (textWidth > rect.width - 6) {
      eventName = eventName.substring(0, min(eventName.length, 5)) + "..."
    }
    val textX = rect.x + 3
    val textY = rect.y + rect.height / 2 + fm.ascent / 2 - 1
    g2.drawString(eventName, textX, textY)
  }

  companion object {
    private const val BAR_HEIGHT = 10
    private const val BAR_MARGIN = 2
  }
}

private object CalendarUtils {
  fun getLocalizedPattern(
    locale: Locale,
  ): String = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
    FormatStyle.LONG,
    null,
    Chronology.ofLocale(locale),
    locale,
  )

  fun getLocalizedYearMonthFormatter(locale: Locale): DateTimeFormatter {
    val localizedPattern = getLocalizedPattern(locale)
    val year = find(localizedPattern, Pattern.compile("(y+)"))
    val month = find(localizedPattern, Pattern.compile("(M+)"))
    val pattern = if (isYearFirst(locale)) "$year $month" else "$month $year"
    return DateTimeFormatter.ofPattern(pattern)
  }

  fun getLocalizedYearMonthText(str: String): String {
    val list = str
      .split(" ".toRegex())
      .dropLastWhile { it.isEmpty() }
      .toTypedArray()
    val isNumeric = list.all { it.toIntOrNull() != null }
    val separator = if (isNumeric) " / " else " "
    return list.joinToString(separator)
  }

  fun find(str: String, ptn: Pattern): String {
    val matcher = ptn.matcher(str)
    return if (matcher.find()) matcher.group(1) else ""
  }

  fun isYearFirst(locale: Locale): Boolean {
    val localizedPattern = getLocalizedPattern(locale)
    val yearIndex = localizedPattern.indexOf('y')
    val monthIndex = localizedPattern.indexOf('M')
    return yearIndex != -1 && monthIndex != -1 && yearIndex < monthIndex
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
