package example

import java.awt.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

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
    rowHeight = 20
    fillsViewportHeight = true
    val renderer = DefaultTableCellRenderer()
    setDefaultRenderer(
      LocalDate::class.java,
    ) { table, value, selected, focused, row, column ->
      renderer.getTableCellRendererComponent(
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

  updateMonthView(realLocalDate)

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }

  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(monthTable))
    it.add(dateLabel, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateMonthView(localDate: LocalDate) {
  currentLocalDate = localDate
  val dtf = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
  monthLabel.text = localDate.format(dtf)
  monthTable.model = CalendarViewTableModel(localDate)
}

private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
  DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
  DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
  else -> Color.WHITE
}

private class CalendarViewTableModel(date: LocalDate) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    // val minusDays = firstDayOfMonth[WeekFields.SUNDAY_START.dayOfWeek()] - 1
    val minusDays = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(minusDays.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(
    row: Int,
    column: Int,
  ): LocalDate = startDate.plusDays((row * columnCount + column).toLong())

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = false
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
