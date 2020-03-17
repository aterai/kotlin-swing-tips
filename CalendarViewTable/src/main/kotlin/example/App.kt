package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(BorderLayout()) {
  private val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
  private var currentLocalDate: LocalDate = realLocalDate
  private val dateLabel = JLabel(realLocalDate.toString(), SwingConstants.CENTER)
  private val monthLabel = JLabel("", SwingConstants.CENTER)
  private val monthTable = object : JTable() {
    override fun updateUI() {
      super.updateUI()
      setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      setCellSelectionEnabled(true)
      setRowHeight(20)
      setFillsViewportHeight(true)
      val renderer = DefaultTableCellRenderer()
      setDefaultRenderer(LocalDate::class.java) { table, value, selected, focused, row, column ->
        val c = renderer.getTableCellRendererComponent(table, value, selected, focused, row, column)
        if (c is JLabel && value is LocalDate) {
          c.setHorizontalAlignment(SwingConstants.CENTER)
          c.setText(value.getDayOfMonth().toString())
          val flg = YearMonth.from(value) == YearMonth.from(currentLocalDate)
          c.setForeground(if (flg) Color.BLACK else Color.GRAY)
          c.setBackground(when {
            value.isEqual(realLocalDate) -> Color(0xDC_FF_DC)
            else -> getDayOfWeekColor(value.getDayOfWeek())
          })
        }
        return@setDefaultRenderer c
      }
    }
  }

  init {
    val header = monthTable.getTableHeader()
    header.setResizingAllowed(false)
    header.setReorderingAllowed(false)
    (header.getDefaultRenderer() as? JLabel)?.setHorizontalAlignment(SwingConstants.CENTER)

    val selectionListener = ListSelectionListener { e ->
      if (!e.getValueIsAdjusting()) {
        val o = monthTable.getValueAt(monthTable.getSelectedRow(), monthTable.getSelectedColumn())
        (o as? LocalDate)?.also {
          dateLabel.setText(it.toString())
        }
      }
    }
    monthTable.getSelectionModel().addListSelectionListener(selectionListener)
    monthTable.getColumnModel().getSelectionModel().addListSelectionListener(selectionListener)

    updateMonthView(realLocalDate)

    val prev = JButton("<")
    prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }

    val next = JButton(">")
    next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

    val p = JPanel(BorderLayout())
    p.add(monthLabel)
    p.add(prev, BorderLayout.WEST)
    p.add(next, BorderLayout.EAST)

    add(p, BorderLayout.NORTH)
    add(JScrollPane(monthTable))
    add(dateLabel, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun updateMonthView(localDate: LocalDate) {
    currentLocalDate = localDate
    val dtf = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
    monthLabel.setText(localDate.format(dtf))
    monthTable.setModel(CalendarViewTableModel(localDate))
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
    DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
    else -> Color.WHITE
  }
}

internal class CalendarViewTableModel(date: LocalDate) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    // int minusDays = firstDayOfMonth.get(WeekFields.SUNDAY_START.dayOfWeek()) - 1;
    val minusDays = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(minusDays.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields
    .getFirstDayOfWeek()
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(row: Int, column: Int): LocalDate =
    startDate.plusDays((row * getColumnCount() + column).toLong())

  override fun isCellEditable(row: Int, column: Int) = false
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
