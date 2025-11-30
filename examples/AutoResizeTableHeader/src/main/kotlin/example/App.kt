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
import kotlin.math.max
import kotlin.math.min

fun makeUI(): Component {
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
    getTableHeader().also {
      it.resizingAllowed = false
      it.reorderingAllowed = false
    }
  }

  override fun createDefaultTableHeader() = object : JTableHeader(columnModel) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      val tbl = getTable()
      val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, tbl)
      if (c is JScrollPane && tbl != null) {
        val rowCount = tbl.model.rowCount + 1
        val r = SwingUtilities.calculateInnerArea(c, null)
        d.height = max(r.height / rowCount, 24)
      }
      return d
    }
  }

  override fun setModel(dataModel: TableModel) {
    super.setModel(dataModel)
    EventQueue.invokeLater { updateWeekHeaderRenderer() }
  }

  private fun updateWeekHeaderRenderer() {
    val cm = getColumnModel()
    val r = WeekHeaderRenderer()
    for (i in 0..<cm.columnCount) {
      cm.getColumn(i).setHeaderRenderer(r)
    }
    getTableHeader()?.repaint()
  }

  override fun doLayout() {
    super.doLayout()
    val clz = JViewport::class.java
    (SwingUtilities.getAncestorOfClass(clz, this) as? JViewport)?.also {
      updateRowsHeight(it)
    }
  }

  private fun updateRowsHeight(viewport: JViewport) {
    val height = viewport.extentSize.height
    val rowCount = model.rowCount
    val rowHeight = height / rowCount
    var remainder = height % rowCount
    for (i in 0..<rowCount) {
      val a = rowHeight + min(1, max(0, remainder--))
      setRowHeight(i, max(a, 1))
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
    val week = weekFields.firstDayOfWeek.plus(column.toLong())
    when (week) {
      DayOfWeek.SUNDAY -> {
        c.setForeground(table.getSelectionForeground())
        c.setBackground(SUNDAY_BGC)
      }

      DayOfWeek.SATURDAY -> {
        c.setForeground(table.getSelectionForeground())
        c.setBackground(SATURDAY_BGC)
      }

      else -> {
        c.setForeground(table.getForeground())
        c.setBackground(table.getTableHeader().getBackground())
      }
    }
    if (c is JLabel) {
      c.setHorizontalAlignment(CENTER)
      val gridColor = UIManager.getColor("Table.gridColor")
      val border = BorderFactory.createMatteBorder(0, 0, 1, 1, gridColor)
      val b = BorderFactory.createCompoundBorder(border, c.border)
      c.setBorder(b)
    }
    return c
  }

  companion object {
    val SUNDAY_BGC = Color(0xB0_12_1A)
    val SATURDAY_BGC = Color(0x1A_12_B0)
  }
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val panel = JPanel()
  private var currentLocalDate: LocalDate? = null

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
      val nextWeekDay = value.plusDays(7)
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = TOP
      c.horizontalAlignment = LEFT
      panel.background = c.background
      currentLocalDate = table.getCurrentLocalDate()
      updateWeekColor(value, table, c, selected)

      val lastRow = row == table.model.rowCount - 1
      val m1 = YearMonth.from(value.plusDays(7)).monthValue
      val m2 = YearMonth.from(currentLocalDate).monthValue
      val split = m1 == m2
      if (lastRow && split) {
        val sub = JLabel(nextWeekDay.dayOfMonth.toString())
        sub.font = c.font
        sub.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        sub.isOpaque = false
        sub.verticalAlignment = BOTTOM
        sub.horizontalAlignment = RIGHT
        panel.removeAll()
        panel.layout = BorderLayout()
        panel.add(sub, BorderLayout.SOUTH)
        panel.add(c, BorderLayout.NORTH)
        panel.border = c.border
        c.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        updateWeekColor(value, table, sub, selected)
        return JLayer(panel, DiagonallySplitCellLayerUI())
      }
    }
    return c
  }

  private fun updateWeekColor(
    d: LocalDate,
    table: JTable,
    c: Component,
    selected: Boolean,
  ) {
    if (selected) {
      c.setForeground(table.getSelectionForeground())
    } else {
      val dayOfWeek = d.getDayOfWeek()
      if (dayOfWeek == DayOfWeek.SUNDAY) {
        c.setForeground(SUNDAY_FGC)
      } else if (dayOfWeek == DayOfWeek.SATURDAY) {
        c.setForeground(SATURDAY_FGC)
      } else if (YearMonth.from(d).equals(YearMonth.from(currentLocalDate))) {
        c.setForeground(table.getForeground())
      } else {
        c.setForeground(Color.GRAY)
      }
    }
  }

  companion object {
    val SUNDAY_FGC = Color(0xB0_12_1A)
    val SATURDAY_FGC = Color(0x1A_12_B0)
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

  override fun getColumnCount() = 7

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
