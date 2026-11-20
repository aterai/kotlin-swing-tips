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
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

private val realLocalDate = LocalDate.now(ZoneId.systemDefault())
private var currentLocalDate = realLocalDate
private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  override fun updateUI() {
    setDefaultRenderer(LocalDate::class.java, null)
    super.updateUI()
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    cellSelectionEnabled = true
    rowHeight = 18
    fillsViewportHeight = true
    val renderer = DefaultTableCellRenderer()
    setDefaultRenderer(
      LocalDate::class.java,
    ) { table, value, selected, focused, row, column ->
      renderer
        .getTableCellRendererComponent(
          table,
          value,
          selected,
          focused,
          row,
          column,
        ).also {
          if (it is JLabel && value is LocalDate) {
            it.horizontalAlignment = SwingConstants.CENTER
            it.text = value.dayOfMonth.toString()
            val m1 = YearMonth.from(value).monthValue
            val m2 = YearMonth.from(currentLocalDate).monthValue
            it.foreground = if (m1 == m2) table.foreground else Color.GRAY
            it.background = if (value.isEqual(realLocalDate)) {
              Color(0xDC_FF_DC)
            } else {
              getDayOfWeekColor(table, value.dayOfWeek)
            }
          }
        }
    }
  }
}

fun createUI(): Component {
  val header = monthTable.tableHeader
  header.resizingAllowed = false
  header.reorderingAllowed = false
  (header.defaultRenderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER

  updateMonthView(realLocalDate)

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }

  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  val log = JTextArea()
  val currentYearMonth = YearMonth.now(ZoneId.systemDefault())
  listOf(Locale.JAPAN, Locale.US, Locale.FRANCE).forEach {
    val isYearFirst = isYearFirst(it)
    val pattern = if (isYearFirst) "yyyy/MMM" else "MMM/yyyy"
    val fmt = DateTimeFormatter.ofPattern(pattern, it)
    val text = currentYearMonth.format(fmt)
    val lang = it.toLanguageTag()
    log.append("%s, %s, isYearFirst? %b%n".format(lang, text, isYearFirst))
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(monthTable))
    it.add(log, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateMonthView(localDate: LocalDate) {
  currentLocalDate = localDate
  val locale = Locale.getDefault()
  val fmt = getLocalizedYearMonthFormatter(locale)
  val text = localDate.format(fmt)
  monthLabel.text = getLocalizedYearMonthText(text)
  monthTable.model = CalendarViewTableModel(localDate)
}

private fun getDayOfWeekColor(table: JTable, dow: DayOfWeek) = when (dow) {
  DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
  DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
  else -> table.background
}

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    // val minusDays = firstDayOfMonth[WeekFields.SUNDAY_START.dayOfWeek()] - 1
    val offset = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(offset.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = DayOfWeek.entries.size // 7

  override fun getValueAt(
    row: Int,
    column: Int,
  ): LocalDate = startDate.plusDays((row * columnCount + column).toLong())

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = false
}

private val YEAR = Pattern.compile("(y+)")
private val MONTH = Pattern.compile("(M+)")

// Quoted literal text such as 'de' in "d 'de' MMMM 'de' y"
private val QUOTED = Pattern.compile("'[^']*'")

fun getLocalizedPattern(locale: Locale): String {
  val pattern = DateTimeFormatterBuilder.getLocalizedDateTimePattern(
    FormatStyle.LONG,
    null,
    Chronology.ofLocale(locale),
    locale,
  )
  // Remove quoted literals so that letters inside them are not
  // mistaken for the year or month pattern symbols
  return QUOTED.matcher(pattern).replaceAll("")
}

fun getLocalizedYearMonthFormatter(locale: Locale): DateTimeFormatter {
  val localizedPattern = getLocalizedPattern(locale)
  val year = find(localizedPattern, YEAR)
  val month = find(localizedPattern, MONTH)
  val yearFirst = isYearFirst(localizedPattern)
  val pattern = if (yearFirst) "$year $month" else "$month $year"
  return DateTimeFormatter.ofPattern(pattern, locale)
}

fun find(str: String, ptn: Pattern): String {
  val matcher = ptn.matcher(str)
  return if (matcher.find()) matcher.group(1) else ""
}

fun getLocalizedYearMonthText(text: String): String {
  val tokens = text.split(" ")
  val isNumeric = tokens.all { token -> token.all { it.isDigit() } }
  return if (isNumeric && tokens.size == 2) {
    // e.g. "2025 7" -> "2025 / 7"
    tokens.joinToString(" / ")
  } else {
    text
  }
}

fun isYearFirst(locale: Locale) = isYearFirst(getLocalizedPattern(locale))

private fun isYearFirst(localizedPattern: String): Boolean {
  val yearIndex = localizedPattern.indexOf('y')
  val monthIndex = localizedPattern.indexOf('M')
  return yearIndex != -1 && monthIndex != -1 && yearIndex < monthIndex
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
