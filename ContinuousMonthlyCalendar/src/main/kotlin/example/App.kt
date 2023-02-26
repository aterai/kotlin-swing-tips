package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

private val locale1 = Locale.getDefault()
private val realLocalDate = LocalDate.now(ZoneId.systemDefault())
private var currentLocalDate = realLocalDate
private val dateLabel = JLabel(realLocalDate.toString(), SwingConstants.CENTER)
private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  override fun updateUI() {
    setDefaultRenderer(LocalDate::class.java, null)
    super.updateUI()
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    cellSelectionEnabled = true
    rowHeight = 32
    fillsViewportHeight = true
    val renderer = DefaultTableCellRenderer()
    setDefaultRenderer(LocalDate::class.java) { table, value, selected, focused, row, column ->
      renderer.getTableCellRendererComponent(table, value, selected, focused, row, column).also {
        if (it is JLabel && value is LocalDate) {
          it.horizontalAlignment = SwingConstants.CENTER
          it.text = value.dayOfMonth.toString()
          val flg = YearMonth.from(value) == YearMonth.from(currentLocalDate)
          it.foreground = if (flg) Color.BLACK else Color.GRAY
          it.background = if (value.isEqual(realLocalDate)) {
            Color(0xDC_FF_DC)
          } else {
            getDayOfWeekColor(value.dayOfWeek)
          }
        }
      }
    }
  }
}
private val scroll = JScrollPane(monthTable)

fun makeUI(): Component {
  val header = monthTable.tableHeader
  header.resizingAllowed = false
  header.reorderingAllowed = false
  (header.defaultRenderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER

  val selectionListener = ListSelectionListener { e ->
    if (!e.valueIsAdjusting) {
      val o = monthTable.getValueAt(monthTable.selectedRow, monthTable.selectedColumn)
      (o as? LocalDate)?.also {
        dateLabel.text = it.toString()
      }
    }
  }
  monthTable.selectionModel.addListSelectionListener(selectionListener)
  monthTable.columnModel.selectionModel.addListSelectionListener(selectionListener)
  val date = getTopLeftCellDayOfMonth(realLocalDate, locale1)
  val model = CalendarViewTableModel(date, locale1)
  monthTable.model = model
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

  updateMonthView(realLocalDate, locale1)

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1), locale1) }

  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1), locale1) }

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
  EventQueue.invokeLater {
    val viewport = scroll.viewport
    val pt = SwingUtilities.convertPoint(viewport, 0, 0, monthTable)
    val row = monthTable.rowAtPoint(pt)
    val col = 6 // monthTable.columnAtPoint(pt);
    val localDate = monthTable.getValueAt(row, col) as LocalDate
    currentLocalDate = localDate
    updateMonthLabel(localDate, locale1)
    viewport.repaint()
  }
}

fun updateMonthView(localDate: LocalDate, locale: Locale) {
  currentLocalDate = localDate
  updateMonthLabel(localDate, locale)
  val model = monthTable.model
  val v = model.rowCount / 2
  val startDate1 = getTopLeftCellDayOfMonth(realLocalDate, locale)
  val startDate2 = getTopLeftCellDayOfMonth(localDate, locale)
  val between = ChronoUnit.WEEKS.between(startDate1, startDate2).toInt()
  // monthTable.revalidate();
  val r = monthTable.getCellRect(v + between, 0, false)
  r.height = scroll.viewport.viewRect.height
  monthTable.scrollRectToVisible(r)
  scroll.repaint()
}

private fun updateMonthLabel(localDate: LocalDate, locale: Locale) {
  val fmt = DateTimeFormatter.ofPattern("yyyy / MM")
  monthLabel.text = localDate.format(fmt.withLocale(locale))
}

private fun getTopLeftCellDayOfMonth(date: LocalDate, locale: Locale): LocalDate {
  val weekFields = WeekFields.of(locale)
  val firstDayOfMonth = YearMonth.from(date).atDay(1)
  val v = firstDayOfMonth[weekFields.dayOfWeek()] - 1
  return firstDayOfMonth.minusDays(v.toLong())
}

private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
  DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
  DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
  else -> Color.WHITE
}

private class CalendarViewTableModel(
  date: LocalDate,
  private val locale: Locale
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields: WeekFields

  init {
    weekFields = WeekFields.of(locale)
    startDate = date.minusWeeks((WEEK_COUNT / 2).toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = WEEK_COUNT // week

  override fun getColumnCount() = 7 // day

  override fun getValueAt(row: Int, column: Int): LocalDate =
    startDate.plusDays((row * columnCount + column).toLong())

  override fun isCellEditable(row: Int, column: Int) = false

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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
