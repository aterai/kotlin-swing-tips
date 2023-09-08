package example

import java.awt.*
import java.awt.event.ItemEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

val realDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
private val combo = JComboBox<String>()
private val monthTable = JTable()
var currentLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
  private set

fun updateMonthView(date: LocalDate) {
  currentLocalDate = date
  monthTable.model = CalendarViewTableModel(YearMonth.from(date))
  monthTable.columnModel.getColumn(0).also {
    it.maxWidth = 100
    it.resizable = false
  }
  combo.selectedIndex = 0
}

fun makeRowFilter(selected: String): RowFilter<TableModel, Int>? = when (selected) {
  "within 3 days before" -> LocalDateFilter(realDate.minusDays(3).plusDays(1), realDate, 0)
  "within 1 week before" -> LocalDateFilter(realDate.minusWeeks(1).plusDays(1), realDate, 0)
  "1 week before and after" -> LocalDateFilter(realDate.minusDays(3), realDate.plusDays(3), 0)
  "within 1 week after" -> LocalDateFilter(realDate, realDate.plusWeeks(1).minusDays(1), 0)
  else -> null
}

fun makeUI(): Component {
  val model = CalendarViewTableModel(YearMonth.from(realDate))
  monthTable.model = model
  val sorter: TableRowSorter<out TableModel> = TableRowSorter(model)
  monthTable.rowSorter = sorter
  monthTable.setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())

  val header = monthTable.tableHeader
  (header.defaultRenderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }
  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

  val cm = arrayOf(
    "1 month",
    "within 3 days before",
    "within 1 week before",
    "1 week before and after",
    "within 1 week after",
  )
  combo.model = DefaultComboBoxModel(cm)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      sorter.rowFilter = makeRowFilter(e.item.toString())
    }
  }
  updateMonthView(realDate)

  val p = JPanel(BorderLayout())
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  return JPanel(BorderLayout(2, 2)).also {
    it.add(combo, BorderLayout.NORTH)
    it.add(JScrollPane(monthTable))
    it.add(p, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    selected: Boolean,
    focused: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, selected, focused, row, column)
    if (value is LocalDate && c is JLabel) {
      c.text = value.dayOfMonth.toString()
      c.horizontalAlignment = CENTER
      c.foreground = if (YearMonth.from(value) == YearMonth.from(currentLocalDate)) {
        Color.BLACK
      } else {
        Color.GRAY
      }
      c.background = if (value.isEqual(realDate)) {
        Color(0xDC_FF_DC)
      } else {
        getDayOfWeekColor(value.dayOfWeek)
      }
    }
    return c
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
    DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
    else -> Color.WHITE
  }
}

private class CalendarViewTableModel(
  private val currentMonth: YearMonth
) : DefaultTableModel(currentMonth.lengthOfMonth(), 2) {
  override fun getColumnClass(column: Int) = if (column == 0) {
    LocalDate::class.java
  } else {
    Any::class.java
  }

  override fun getColumnName(column: Int) = if (column == 0) {
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM")
    currentMonth.format(formatter.withLocale(Locale.getDefault()))
  } else {
    ""
  }

  override fun getValueAt(row: Int, column: Int) = if (column == 0) {
    currentMonth.atDay(1).plusDays(row.toLong())
  } else {
    super.getValueAt(row, column)
  }

  override fun isCellEditable(row: Int, column: Int) = column != 0
}

private class LocalDateFilter(
  private val startDate: LocalDate,
  private val endDate: LocalDate,
  private val column: Int
) : RowFilter<TableModel, Int>() {
  override fun include(entry: Entry<out TableModel, out Int>): Boolean {
    val date = entry.model.getValueAt(entry.identifier, column)
    if (date is LocalDate) {
      return !(startDate.isAfter(date) || endDate.isBefore(date))
    }
    return false
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
