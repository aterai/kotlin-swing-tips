package example

import java.awt.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer

private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy / MM")
private val defaultLocale = Locale.getDefault()
private val today = LocalDate.now(ZoneId.systemDefault())
private var currentLocalDate = today
private val dateLabel = JLabel(today.toString(), SwingConstants.CENTER)
private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  override fun updateUI() {
    setDefaultRenderer(LocalDate::class.java, null)
    super.updateUI()
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    cellSelectionEnabled = true
    rowHeight = 32
    fillsViewportHeight = true
    val header = getTableHeader()
    header.resizingAllowed = false
    header.reorderingAllowed = false
    (header.defaultRenderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
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
            val isSameMonth = YearMonth.from(value) == YearMonth.from(currentLocalDate)
            it.foreground = if (isSameMonth) table.foreground else Color.GRAY
            it.background = if (value.isEqual(today)) {
              Color(0xDC_FF_DC)
            } else {
              getDayOfWeekColor(table, value.dayOfWeek)
            }
          }
        }
    }
  }
}
private val scroll = JScrollPane(monthTable)

fun createUI(): Component {
  // The renderer, selection mode, row height and header settings
  // are configured in monthTable#updateUI()
  val selectionListener = ListSelectionListener { e ->
    val row = monthTable.selectedRow
    val column = monthTable.selectedColumn
    // Ignore the intermediate state where only one of the row or column is selected
    if (!e.valueIsAdjusting && row >= 0 && column >= 0) {
      (monthTable.getValueAt(row, column) as? LocalDate)?.also {
        dateLabel.text = it.toString()
      }
    }
  }
  monthTable.selectionModel.addListSelectionListener(selectionListener)
  monthTable.columnModel.selectionModel.addListSelectionListener(selectionListener)
  val date = getTopLeftCellDayOfMonth(today, defaultLocale)
  monthTable.model = CalendarViewTableModel(date, defaultLocale)
  val verticalScrollBar = object : JScrollBar(VERTICAL) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0
      return d
    }
  }
  verticalScrollBar.unitIncrement = monthTable.rowHeight
  scroll.verticalScrollBar = verticalScrollBar
  verticalScrollBar.model.addChangeListener { verticalScrollChanged() }

  updateMonthView(today, defaultLocale)

  val prev = JButton("<")
  prev.addActionListener {
    updateMonthView(currentLocalDate.minusMonths(1), defaultLocale)
  }

  val next = JButton(">")
  next.addActionListener {
    updateMonthView(currentLocalDate.plusMonths(1), defaultLocale)
  }

  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.add(dateLabel, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun verticalScrollChanged() {
  EventQueue.invokeLater { updateCurrentMonth() }
}

private fun updateCurrentMonth() {
  val viewport = scroll.viewport
  val pt = SwingUtilities.convertPoint(viewport, 0, 0, monthTable)
  val row = monthTable.rowAtPoint(pt)
  if (row >= 0) {
    // The top-right cell (the last day of the top week) always belongs to
    // the month whose first week is displayed at the top
    val column = monthTable.columnCount - 1
    (monthTable.getValueAt(row, column) as? LocalDate)?.also {
      currentLocalDate = it
      updateMonthLabel(it, defaultLocale)
      viewport.repaint()
    }
  }
}

private fun updateMonthView(
  date: LocalDate,
  locale: Locale,
) {
  currentLocalDate = date
  updateMonthLabel(date, locale)
  // The center row of the model is the first week of this month (today)
  val centerRow = monthTable.rowCount / 2
  val baseDate = getTopLeftCellDayOfMonth(today, locale)
  val targetDate = getTopLeftCellDayOfMonth(date, locale)
  val weeks = ChronoUnit.WEEKS.between(baseDate, targetDate).toInt()
  val r = monthTable.getCellRect(centerRow + weeks, 0, false)
  r.height = scroll.viewport.viewRect.height
  monthTable.scrollRectToVisible(r)
  scroll.repaint()
}

private fun updateMonthLabel(
  date: LocalDate,
  locale: Locale,
) {
  monthLabel.text = date.format(MONTH_FORMATTER.withLocale(locale))
}

private fun getTopLeftCellDayOfMonth(
  date: LocalDate,
  locale: Locale,
): LocalDate {
  val weekFields = WeekFields.of(locale)
  val firstDayOfMonth = date.withDayOfMonth(1)
  val offset = firstDayOfMonth[weekFields.dayOfWeek()] - 1
  return firstDayOfMonth.minusDays(offset.toLong())
}

private fun getDayOfWeekColor(table: JTable, dow: DayOfWeek) = when (dow) {
  DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
  DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
  else -> table.background
}

private class CalendarViewTableModel(
  date: LocalDate,
  private val locale: Locale,
) : AbstractTableModel() {
  private val startDate = date.minusWeeks((WEEK_COUNT / 2).toLong())
  private val weekFields = WeekFields.of(locale)

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, locale)

  override fun getRowCount() = WEEK_COUNT

  override fun getColumnCount() = DayOfWeek.entries.size // 7 days a week

  override fun getValueAt(
    row: Int,
    column: Int,
  ): LocalDate = startDate.plusDays((row * columnCount + column).toLong())

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = false

  companion object {
    private const val WEEK_COUNT = 1000
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
