package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.text.DateFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Date
import java.util.EnumMap
import java.util.EventObject
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table: JTable = object : JTable(makeModel()) {
    override fun updateUI() {
      super.updateUI()
      setCellSelectionEnabled(true)
      setDefaultEditor(Date::class.java, DateEditor())
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("A", "B")
  val data = arrayOf<Array<Any>>(
    arrayOf(Date(), ""),
    arrayOf(Date(), ""),
    arrayOf(Date(), ""),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = column == 0
  }
}

private class DateEditor :
  AbstractCellEditor(),
  TableCellEditor,
  ActionListener {
  private val button = JButton()
  private val formatter: DateFormat = DateFormat.getDateInstance()
  private val dateChooser = CalenderPanel()
  private var popup: JPopupMenu? = null
  private var table: JTable? = null

  init {
    button.actionCommand = EDIT
    button.addActionListener(this)
    button.setContentAreaFilled(false)
    button.setFocusPainted(false)
    button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    button.setHorizontalAlignment(SwingConstants.LEFT)
    button.setHorizontalTextPosition(SwingConstants.RIGHT)
  }

  override fun actionPerformed(e: ActionEvent) {
    if (EDIT == e.getActionCommand() && table != null) {
      val row = table?.selectedRow ?: -1
      val col = table?.selectedColumn ?: -1
      val rect = table?.getCellRect(row, col, true) ?: Rectangle()
      val p = Point(rect.x, rect.maxY.toInt())
      val pop = popup ?: JPopupMenu().also {
        it.add(dateChooser)
        it.pack()
      }
      pop.show(table, p.x, p.y)
      popup = pop
      dateChooser.requestFocusInWindow()
    }
  }

  override fun isCellEditable(e: EventObject?): Boolean =
    e is MouseEvent && e.getClickCount() >= 2

  override fun getCellEditorValue(): Any {
    val d = dateChooser.localDate
    return Date.from(d?.atStartOfDay(ZoneId.systemDefault())?.toInstant())
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is Date) {
      val date = value
      button.setText(formatter.format(date))
      button.setOpaque(true)
      val fgc = table.getSelectionForeground()
      button.setForeground(Color(fgc.rgb))
      button.setBackground(table.getSelectionBackground())
      val dateTime = date.toInstant().atZone(ZoneId.systemDefault())
      dateChooser.localDate = dateTime.toLocalDate()
      this.table = table
    }
    return button
  }

  private inner class CalenderPanel : JPanel(BorderLayout()) {
    val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    var localDate: LocalDate?
      get() = currentLocalDate
      set(date) {
        currentLocalDate = date?.also {
          val fmt = DateTimeFormatter.ofPattern("yyyy / MM")
          monthLabel.setText(it.format(fmt.withLocale(Locale.getDefault())))
          monthTable.setModel(CalendarViewTableModel(it))
        }
      }
    private val monthLabel = JLabel("", SwingConstants.CENTER)
    private val monthTable = MonthTable()
    private var currentLocalDate: LocalDate? = null

    init {
      val r = CalendarTableRenderer(this, monthTable.highlighter)
      monthTable.setDefaultRenderer(LocalDate::class.java, r)
      monthTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
      monthTable.setCellSelectionEnabled(true)
      monthTable.setRowHeight(16)
      monthTable.setFillsViewportHeight(true)

      val header = monthTable.getTableHeader()
      header.setResizingAllowed(false)
      header.setReorderingAllowed(false)
      val hr = header.defaultRenderer
      (hr as? JLabel)?.setHorizontalAlignment(SwingConstants.CENTER)

      val mouseHandler = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent?) {
          val row = monthTable.selectedRow
          val column = monthTable.selectedColumn
          currentLocalDate = monthTable.getValueAt(row, column) as? LocalDate
          popup?.setVisible(false)
          fireEditingStopped()
        }
      }
      monthTable.addMouseListener(mouseHandler)
      this.localDate = realLocalDate
      val prev = JButton("<")
      prev.addActionListener {
        this.localDate = this.localDate?.minusMonths(1)
      }
      val next = JButton(">")
      next.addActionListener {
        this.localDate = this.localDate?.plusMonths(1)
      }
      val p = JPanel(BorderLayout())
      p.add(monthLabel)
      p.add(prev, BorderLayout.WEST)
      p.add(next, BorderLayout.EAST)
      add(p, BorderLayout.NORTH)
      add(JScrollPane(monthTable))
    }

    override fun getPreferredSize(): Dimension = Dimension(220, 143)
  }

  private class MonthTable : JTable() {
    var highlighter: HighlightListener? = null
    private var prevHeight = -1
    private var prevCount = -1

    override fun updateUI() {
      removeMouseListener(highlighter)
      removeMouseMotionListener(highlighter)
      super.updateUI()
      setRowSelectionAllowed(false)
      highlighter = HighlightListener()
      addMouseListener(highlighter)
      addMouseMotionListener(highlighter)
    }

    override fun doLayout() {
      super.doLayout()
      val clz = JViewport::class.java
      (SwingUtilities.getAncestorOfClass(clz, this) as? JViewport)?.also {
        updateRowsHeight(it)
      }
    }

    private fun updateRowsHeight(viewPort: JViewport) {
      val height = viewPort.extentSize.height
      val rowCount = model.rowCount
      val defaultRowHeight = height / rowCount
      if ((height != prevHeight || rowCount != prevCount) && defaultRowHeight > 0) {
        var remainder = height % rowCount
        for (i in 0..<rowCount) {
          val a = 1.coerceAtMost(0.coerceAtLeast(remainder--))
          setRowHeight(i, defaultRowHeight + a)
        }
      }
      prevHeight = height
      prevCount = rowCount
    }
  }

  private class CalendarTableRenderer(
    private val calender: CalenderPanel,
    private val highlighter: HighlightListener?,
  ) : DefaultTableCellRenderer() {
    private val holidayColorMap = EnumMap<DayOfWeek, Color>(DayOfWeek::class.java)
    private val realLocalDate: LocalDate = calender.realLocalDate

    init {
      holidayColorMap.put(DayOfWeek.SUNDAY, Color(0xFFDCDC))
      holidayColorMap.put(DayOfWeek.SATURDAY, Color(0xDCDCFF))
    }

    override fun getTableCellRendererComponent(
      table: JTable,
      value: Any?,
      selected: Boolean,
      focused: Boolean,
      row: Int,
      column: Int,
    ): Component? {
      val c = super.getTableCellRendererComponent(
        table,
        value,
        selected,
        focused,
        row,
        column,
      )
      if (c is JLabel && value is LocalDate) {
        c.setHorizontalAlignment(CENTER)
        c.setText(value.dayOfMonth.toString())
        if (YearMonth.from(value) == YearMonth.from(calender.localDate)) {
          c.setForeground(table.getForeground())
        } else {
          c.setForeground(Color.GRAY)
        }
        if (value.isEqual(realLocalDate)) {
          c.setBackground(Color(0xDC_FF_DC))
        } else {
          c.setBackground(getDayOfWeekColor(table, value.getDayOfWeek()))
        }
        highlighter?.getCellHighlightColor(row, column)?.also { c.setBackground(it) }
      }
      return c
    }

    fun getDayOfWeekColor(table: JTable, dow: DayOfWeek?): Color? =
      holidayColorMap[dow] ?: table.getBackground()
  }

  companion object {
    private const val EDIT = "edit"
  }
}

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String =
    weekFields.firstDayOfWeek
      .plus(column.toLong())
      .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(row: Int, column: Int): Any =
    startDate.plusDays(row.toLong() * columnCount + column)

  override fun isCellEditable(row: Int, column: Int) = false
}

private class HighlightListener : MouseAdapter() {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1

  fun getCellHighlightColor(row: Int, column: Int): Color? {
    val ri = this.viewRowIndex == row
    val ci = this.viewColumnIndex == column
    return if (ri && ci) Color.LIGHT_GRAY else null
  }

  private fun setHighlightTableCell(e: MouseEvent) {
    (e.component as? JTable)?.also {
      viewRowIndex = it.rowAtPoint(e.point)
      viewColumnIndex = it.columnAtPoint(e.point)
      if (viewRowIndex < 0 || viewColumnIndex < 0) {
        viewRowIndex = -1
        viewColumnIndex = -1
      }
      it.repaint()
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    setHighlightTableCell(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    setHighlightTableCell(e)
  }

  override fun mouseExited(e: MouseEvent) {
    viewRowIndex = -1
    viewColumnIndex = -1
    e.component.repaint()
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
