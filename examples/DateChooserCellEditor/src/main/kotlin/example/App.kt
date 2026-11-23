package example

import java.awt.*
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
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableModel
import kotlin.math.max

fun createUI(): Component {
  val table = object : JTable(createModel()) {
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

private fun createModel(): TableModel {
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
  TableCellEditor {
  private val button = JButton()
  private val formatter: DateFormat = DateFormat.getDateInstance()
  private val dateChooser = CalendarPanel()
  private val popup = JPopupMenu()

  // The date returned by getCellEditorValue(), kept separately from the month
  // displayed in the calendar so that browsing months does not change the value
  private var selectedDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
  private var table: JTable? = null

  init {
    button.addActionListener { showPopup() }
    button.setContentAreaFilled(false)
    button.setFocusPainted(false)
    button.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1))
    button.setHorizontalAlignment(SwingConstants.LEFT)
    button.setHorizontalTextPosition(SwingConstants.RIGHT)
    popup.add(dateChooser)
    popup.addPopupMenuListener(object : PopupMenuListener {
      override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
        // not needed
      }

      override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
        // not needed
      }

      override fun popupMenuCanceled(e: PopupMenuEvent) {
        // The popup was closed without selecting a date (e.g. by clicking outside)
        cancelCellEditing()
      }
    })
  }

  // The button receives the mouse event that started editing and opens the calendar
  private fun showPopup() {
    val t = table ?: return
    if (t.isEditing) {
      val rect = t.getCellRect(t.editingRow, t.editingColumn, true)
      popup.pack()
      popup.show(t, rect.x, rect.y + rect.height)
      dateChooser.requestFocusInWindow()
    }
  }

  override fun isCellEditable(e: EventObject?): Boolean =
    e is MouseEvent && e.getClickCount() >= 2

  override fun getCellEditorValue(): Any =
    Date.from(selectedDate.atStartOfDay(ZoneId.systemDefault()).toInstant())

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is Date) {
      button.setText(formatter.format(value))
      button.setOpaque(true)
      // Copy the color so that it is not a UIResource replaced by the button UI
      val fgc = table.getSelectionForeground()
      button.setForeground(Color(fgc.rgb))
      button.setBackground(table.getSelectionBackground())
      val dateTime = value.toInstant().atZone(ZoneId.systemDefault())
      selectedDate = dateTime.toLocalDate()
      dateChooser.localDate = selectedDate
      this.table = table
    }
    return button
  }

  private inner class CalendarPanel : JPanel(BorderLayout()) {
    val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
    var localDate: LocalDate?
      get() = currentLocalDate
      set(date) {
        currentLocalDate = date?.also {
          monthLabel.setText(it.format(MONTH_FORMATTER.withLocale(Locale.getDefault())))
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
        override fun mouseClicked(e: MouseEvent) {
          // Use the clicked cell rather than the selected cell, which may be -1
          val pt = e.point
          val row = monthTable.rowAtPoint(pt)
          val column = monthTable.columnAtPoint(pt)
          val date = if (row >= 0 &&
            column >= 0
          ) {
            monthTable.getValueAt(row, column)
          } else {
            null
          }
          if (date is LocalDate) {
            selectedDate = date
            popup.isVisible = false
            stopCellEditing()
          }
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

    override fun getPreferredSize() = Dimension(220, 143)
  }

  private class MonthTable : JTable() {
    // Created once and kept across updateUI() so that the renderer can share it
    val highlighter = HighlightListener()

    init {
      addMouseListener(highlighter)
      addMouseMotionListener(highlighter)
    }

    private fun adjustRowHeights(viewport: JViewport) {
      val height = viewport.extentSize.height
      val rowCount = model.rowCount
      val baseRowHeight = height / rowCount
      val remainder = height % rowCount
      for (i in 0..<rowCount) {
        val adjustedHeight = baseRowHeight + (if (i < remainder) 1 else 0)
        setRowHeight(i, max(1, adjustedHeight))
      }
    }

    override fun getScrollableTracksViewportHeight() = getParent() is JViewport

    override fun doLayout() {
      super.doLayout()
      val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, this)
      if (c is JViewport) {
        adjustRowHeights(c)
      }
    }
  }

  private class CalendarTableRenderer(
    private val calendar: CalendarPanel,
    private val highlighter: HighlightListener,
  ) : DefaultTableCellRenderer() {
    private val holidayColorMap = EnumMap<DayOfWeek, Color>(DayOfWeek::class.java)
    private val realLocalDate: LocalDate = calendar.realLocalDate

    init {
      holidayColorMap[DayOfWeek.SUNDAY] = Color(0xFF_DC_DC)
      holidayColorMap[DayOfWeek.SATURDAY] = Color(0xDC_DC_FF)
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
        val isSameMonth = YearMonth.from(value) == YearMonth.from(calendar.localDate)
        c.foreground = if (isSameMonth) table.foreground else Color.GRAY
        if (value.isEqual(realLocalDate)) {
          c.background = Color(0xDC_FF_DC)
        } else {
          c.background = getDayOfWeekColor(table, value.getDayOfWeek())
        }
        highlighter.getCellHighlightColor(row, column)?.also { c.setBackground(it) }
      }
      return c
    }

    fun getDayOfWeekColor(table: JTable, dow: DayOfWeek?): Color? =
      holidayColorMap[dow] ?: table.getBackground()
  }

  companion object {
    private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy / MM")
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

  override fun getColumnCount() = DayOfWeek.entries.size // 7

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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
