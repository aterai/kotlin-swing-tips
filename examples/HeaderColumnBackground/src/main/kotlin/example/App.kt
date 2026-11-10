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
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel

fun createUI(): Component {
  val monthLabel = JLabel("", SwingConstants.CENTER)
  val monthTable = MonthTable()
  updateMonthView(monthTable, monthLabel, LocalDate.now(ZoneId.systemDefault()))

  val prev = JButton("<")
  prev.addActionListener {
    val d = monthTable.getCurrentLocalDate().minusMonths(1)
    updateMonthView(monthTable, monthLabel, d)
  }

  val next = JButton(">")
  next.addActionListener {
    val d = monthTable.getCurrentLocalDate().plusMonths(1)
    updateMonthView(monthTable, monthLabel, d)
  }

  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(MonthScrollPane(monthTable))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateMonthView(table: MonthTable, label: JLabel, date: LocalDate) {
  table.setCurrentLocalDate(date)
  val locale = Locale.getDefault()
  val fmt = CalendarUtils.getLocalizedYearMonthFormatter(locale)
  val txt = date.format(fmt.withLocale(locale))
  label.setText(CalendarUtils.getLocalizedYearMonthText(txt))
  table.setModel(CalendarViewTableModel(date))
}

private class MonthScrollPane(
  view: Component,
) : JScrollPane(view) {
  override fun updateUI() {
    super.updateUI()
    verticalScrollBarPolicy = VERTICAL_SCROLLBAR_NEVER
    horizontalScrollBarPolicy = HORIZONTAL_SCROLLBAR_NEVER
  }
}

private class MonthTable : JTable() {
  private var currentLocalDate = LocalDate.now(ZoneId.systemDefault())

  fun setCurrentLocalDate(date: LocalDate) {
    currentLocalDate = date
  }

  fun getCurrentLocalDate(): LocalDate = currentLocalDate

  override fun updateUI() {
    super.updateUI()
    setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    cellSelectionEnabled = true
    fillsViewportHeight = true
    rowHeight = 32
    getTableHeader().also {
      it.resizingAllowed = false
      it.reorderingAllowed = false
    }
  }

  override fun createDefaultTableHeader() = object : JTableHeader(columnModel) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 32
      return d
    }
  }

  override fun setModel(dataModel: TableModel) {
    super.setModel(dataModel)
    // The columns are recreated synchronously in JTable#setModel(...), so setting
    // the renderer here prevents the default header from being painted even once.
    updateWeekHeaderRenderer()
  }

  private fun updateWeekHeaderRenderer() {
    val cm = getColumnModel()
    val r = WeekHeaderRenderer()
    for (i in 0..<cm.columnCount) {
      cm.getColumn(i).setHeaderRenderer(r)
    }
  }
}

private class WeekHeaderRenderer : DefaultTableCellRenderer() {
  private val weekFields = WeekFields.of(Locale.getDefault())

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    val dayOfWeek = weekFields.firstDayOfWeek.plus(column.toLong())
    when (dayOfWeek) {
      DayOfWeek.SUNDAY -> {
        // Table.selectionForeground is dark in some LookAndFeels and unreadable here
        c.foreground = Color.WHITE
        c.background = SUNDAY_BGC
      }

      DayOfWeek.SATURDAY -> {
        c.foreground = Color.WHITE
        c.background = SATURDAY_BGC
      }

      else -> {
        val header = table.tableHeader
        c.foreground = header.foreground
        c.background = header.background
      }
    }
    if (c is JLabel) {
      c.setHorizontalAlignment(CENTER)
      val border = BorderFactory.createMatteBorder(0, 0, 1, 1, table.gridColor)
      val b = BorderFactory.createCompoundBorder(border, c.border)
      c.setBorder(b)
    }
    return c
  }

  companion object {
    private val SUNDAY_BGC = Color(0xB0_12_1A)
    private val SATURDAY_BGC = Color(0x1A_12_B0)
  }
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val subLabel = createSubLabel()
  private val panel = JPanel(BorderLayout())
  private val layer = JLayer(panel, DiagonallySplitCellLayerUI())

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    selected: Boolean,
    focused: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      selected,
      focused,
      row,
      column,
    )
    if (value is LocalDate && c is JLabel && table is MonthTable) {
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = TOP
      c.horizontalAlignment = LEFT
      panel.background = c.background
      val currentMonth = YearMonth.from(table.getCurrentLocalDate())
      updateWeekColor(value, currentMonth, table, c, selected)
      val model = table.model
      val nextWeekDay = value.plusDays(model.columnCount.toLong()) // plus 7 days
      val isLastRow = row == model.rowCount - 1
      if (isLastRow && isDiagonallySplitCell(nextWeekDay, currentMonth)) {
        subLabel.text = nextWeekDay.dayOfMonth.toString()
        subLabel.font = c.font
        updateWeekColor(nextWeekDay, currentMonth, table, subLabel, selected)
        panel.removeAll()
        panel.add(subLabel, BorderLayout.SOUTH)
        panel.add(c, BorderLayout.NORTH)
        panel.border = c.border
        c.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        return layer
      }
    }
    return c
  }

  companion object {
    private val SUNDAY_FGC = Color(0xB0_12_1A)
    private val SATURDAY_FGC = Color(0x1A_12_B0)

    private fun createSubLabel() = JLabel().also {
      it.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      it.verticalAlignment = BOTTOM
      it.horizontalAlignment = RIGHT
    }

    private fun isDiagonallySplitCell(
      nextWeekDay: LocalDate,
      currentMonth: YearMonth,
    ) = YearMonth.from(nextWeekDay) == currentMonth

    private fun updateWeekColor(
      d: LocalDate,
      currentMonth: YearMonth,
      table: JTable,
      c: Component,
      selected: Boolean,
    ) {
      c.foreground = if (selected) {
        table.selectionForeground
      } else {
        when {
          d.dayOfWeek == DayOfWeek.SUNDAY -> SUNDAY_FGC
          d.dayOfWeek == DayOfWeek.SATURDAY -> SATURDAY_FGC
          YearMonth.from(d) == currentMonth -> table.foreground
          else -> Color.GRAY
        }
      }
    }
  }
}

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val minusDays = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(minusDays.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 5

  override fun getColumnCount() = DayOfWeek.entries.size // 7

  override fun getValueAt(
    row: Int,
    column: Int,
  ): Any = startDate.plusDays((row * columnCount + column).toLong())

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = false
}

private class DiagonallySplitCellLayerUI : LayerUI<JPanel>() {
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = UIManager.getColor("Table.gridColor")
    g2.drawLine(c.width, 0, 0, c.height)
    g2.dispose()
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
    val parts = str
      .split(" ".toRegex())
      .dropLastWhile { it.isEmpty() }
      .toTypedArray()
    val isNumeric = parts.all { it.toIntOrNull() != null }
    val separator = if (isNumeric) " / " else " "
    return parts.joinToString(separator)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
