package example

import java.awt.*
import java.awt.geom.Path2D
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
import java.util.EnumSet
import java.util.Locale
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

fun makeUI(): Component {
  val monthLabel = JLabel("", SwingConstants.CENTER)
  val monthTable = MonthTable()
  updateMonthView(monthTable, monthLabel, LocalDate.now(ZoneId.systemDefault()))

  val prevButton = JButton("<")
  prevButton.addActionListener {
    monthTable.currentDate?.also {
      updateMonthView(monthTable, monthLabel, it.minusMonths(1))
    }
  }
  val nextButton = JButton(">")
  nextButton.addActionListener {
    monthTable.currentDate?.also {
      updateMonthView(monthTable, monthLabel, it.plusMonths(1))
    }
  }

  val topPanel = JPanel(BorderLayout())
  topPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 5, 0))
  topPanel.setOpaque(false)
  topPanel.add(monthLabel)
  topPanel.add(prevButton, BorderLayout.WEST)
  topPanel.add(nextButton, BorderLayout.EAST)

  val p = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      setBackground(UIManager.getColor("Table.background"))
    }
  }
  p.add(topPanel, BorderLayout.NORTH)
  p.add(MonthScrollPane(monthTable))
  p.setBorder(BorderFactory.createEmptyBorder(5, 25, 15, 25))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateMonthView(table: MonthTable, label: JLabel, date: LocalDate) {
  table.currentDate = date
  val locale = Locale.getDefault()
  val formatter = CalendarUtils.getLocalizedYearMonthFormatter(locale)
  val formattedText = date.format(formatter.withLocale(locale))
  label.setText(CalendarUtils.getLocalizedYearMonthText(formattedText))
  table.setModel(CalendarViewTableModel(date))
}

private class MonthScrollPane(
  view: Component,
) : JScrollPane(view) {
  override fun updateUI() {
    super.updateUI()
    setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
    setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    setBorder(BorderFactory.createEmptyBorder())
    setViewportBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0))
  }

  override fun isOpaque() = false
}

private class MonthTable : JTable() {
  var currentDate: LocalDate? = null
  private var prevHeight = -1

  override fun updateUI() {
    super.updateUI()
    setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    setCellSelectionEnabled(true)
    setFillsViewportHeight(true)
    setShowVerticalLines(false)
    setShowHorizontalLines(false)
    setIntercellSpacing(Dimension(0, 0))
    setBackground(UIManager.getColor("Table.background"))
    val header = getTableHeader()
    header.setResizingAllowed(false)
    header.setReorderingAllowed(false)
    header.setOpaque(false)
    updateWeekHeaderRenderer()
  }

  override fun setModel(dataModel: TableModel) {
    super.setModel(dataModel)
    prevHeight = -1
    EventQueue.invokeLater { updateWeekHeaderRenderer() }
  }

  private fun updateWeekHeaderRenderer() {
    val columnModel = getColumnModel()
    val renderer = WeekHeaderRenderer()
    for (i in 0..<columnModel.columnCount) {
      columnModel.getColumn(i).setHeaderRenderer(renderer)
    }
    getTableHeader().repaint()
  }

  override fun doLayout() {
    super.doLayout()
    val clz = JViewport::class.java
    (SwingUtilities.getAncestorOfClass(clz, this) as? JViewport)?.also {
      adjustRowHeights(it)
    }
  }

  private fun adjustRowHeights(viewport: JViewport) {
    val height = viewport.extentSize.height
    val rowCount = model.rowCount
    val baseRowHeight = height / rowCount
    if (height != prevHeight && baseRowHeight > 0) {
      var remainder = height % rowCount
      for (i in 0..<rowCount) {
        val adjustedHeight = baseRowHeight + min(max(remainder, 0), 1)
        setRowHeight(i, max(1, adjustedHeight))
        remainder -= 1
      }
    }
    prevHeight = height
  }
}

internal enum class Corner {
  TOP_LEFT,
  TOP_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_RIGHT,
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val roundedCorners = EnumSet.noneOf(Corner::class.java)
  private val realDate = LocalDate.now(ZoneId.systemDefault())
  private var row = 0
  private var column = 0

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    selected: Boolean,
    focused: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val renderer = super.getTableCellRendererComponent(
      table,
      value,
      selected,
      focused,
      row,
      column,
    )
    renderer.setBackground(table.getBackground())
    this.row = row
    this.column = column
    updateCorners(table, row, column)
    if (value is LocalDate && renderer is JLabel && table is MonthTable) {
      renderer.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))
      renderer.setText(value.dayOfMonth.toString())
      renderer.setForeground(table.getForeground())
      renderer.setVerticalAlignment(TOP)
      renderer.setHorizontalAlignment(CENTER)
      renderer.setVerticalTextPosition(TOP)
      renderer.setHorizontalTextPosition(CENTER)
      val currentDate: LocalDate = table.currentDate!!
      if (YearMonth.from(value) == YearMonth.from(currentDate)) {
        renderer.setFont(renderer.getFont().deriveFont(Font.BOLD))
      } else {
        renderer.setFont(renderer.getFont().deriveFont(Font.PLAIN))
      }
      if (value == realDate) {
        renderer.setIcon(IndicatorIcon(renderer.getForeground()))
      } else {
        renderer.setIcon(null)
      }
    }
    return renderer
  }

  private fun updateCorners(table: JTable, row: Int, col: Int) {
    roundedCorners.clear()
    val model = table.model
    val lastRow = model.rowCount - 1
    val lastCol = model.columnCount - 1
    if (row == 0 && col == 0) {
      roundedCorners.add(Corner.TOP_LEFT)
    }
    if (row == 0 && col == lastCol) {
      roundedCorners.add(Corner.TOP_RIGHT)
    }
    if (row == lastRow && col == 0) {
      roundedCorners.add(Corner.BOTTOM_LEFT)
    }
    if (row == lastRow && col == lastCol) {
      roundedCorners.add(Corner.BOTTOM_RIGHT)
    }
  }

  override fun isOpaque() = false

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val bounds = getBounds()
    bounds.setLocation(0, 0)
    g2.paint = getBackground()
    g2.fill(bounds)
    g2.paint = UIManager.getColor("Table.gridColor")
    val shape = buildRoundedRectPath(bounds, 16.0, 16.0, row, column)
    g2.draw(shape)
    g2.dispose()
    super.paintComponent(g)
  }

  @Suppress("LongMethod")
  private fun buildRoundedRectPath(
    bounds: Rectangle,
    arcWidth: Double,
    arcHeight: Double,
    row: Int,
    col: Int,
  ): Shape {
    val x = bounds.getX()
    val y = bounds.getY()
    val w = bounds.getWidth() - if (col == 6) 2.0 else 0.0
    val h = bounds.getHeight() - if (row == 5) 2.0 else 0.0
    val halfArcH = arcHeight * .5
    val halfArcW = arcWidth * .5
    val kappa = 4.0 * (sqrt(2.0) - 1.0) / 3.0 // ≒ 0.55228
    val ctrlOffsetW = halfArcW * kappa
    val ctrlOffsetH = halfArcH * kappa
    val path = Path2D.Double()
    if (roundedCorners.contains(Corner.TOP_LEFT)) {
      path.moveTo(x, y + halfArcH)
      path.curveTo(
        x,
        y + halfArcH - ctrlOffsetH,
        x + halfArcW - ctrlOffsetW,
        y,
        x + halfArcW,
        y,
      )
    } else {
      path.moveTo(x, y)
    }
    if (roundedCorners.contains(Corner.TOP_RIGHT)) {
      path.lineTo(x + w - halfArcW, y)
      path.curveTo(
        x + w - halfArcW + ctrlOffsetW,
        y,
        x + w,
        y + halfArcH - ctrlOffsetH,
        x + w,
        y + halfArcH,
      )
    } else {
      path.lineTo(x + w, y)
    }
    if (roundedCorners.contains(Corner.BOTTOM_RIGHT)) {
      path.lineTo(x + w, y + h - halfArcH)
      path.curveTo(
        x + w,
        y + h - halfArcH + ctrlOffsetH,
        x + w - halfArcW + ctrlOffsetW,
        y + h,
        x + w - halfArcW,
        y + h,
      )
    } else {
      path.lineTo(x + w, y + h)
    }
    if (roundedCorners.contains(Corner.BOTTOM_LEFT)) {
      path.lineTo(x + halfArcW, y + h)
      path.curveTo(
        x + halfArcW - ctrlOffsetW,
        y + h,
        x,
        y + h - halfArcH + ctrlOffsetH,
        x,
        y + h - halfArcH,
      )
    } else {
      path.lineTo(x, y + h)
    }
    path.closePath()
    return path
  }
}

private class WeekHeaderRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val renderer = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (renderer is JLabel) {
      renderer.setHorizontalAlignment(CENTER)
      renderer.setBackground(table.getBackground())
      renderer.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0))
    }
    return renderer
  }
}

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields: WeekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val dayOffset = firstDayOfMonth.get(weekFields.dayOfWeek()) - 1
    startDate = firstDayOfMonth.minusDays(dayOffset.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String = weekFields.firstDayOfWeek
    .plus(column.toLong())
    .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = DayOfWeek.entries.size

  override fun getValueAt(row: Int, column: Int): LocalDate =
    startDate.plusDays(row.toLong() * columnCount + column)

  override fun isCellEditable(row: Int, column: Int) = false
}

private class IndicatorIcon(
  private val color: Color,
) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = color
    val arcRadius = 2
    val arcDiameter = arcRadius * 2
    val r = SwingUtilities.calculateInnerArea(c as? JComponent, null)
    val ox = r.centerX.toInt() - arcRadius
    val oy = c.getFont().getSize() + arcDiameter
    g2.fillOval(ox, oy, arcDiameter, arcDiameter)
    g2.dispose()
  }

  override fun getIconWidth() = 8

  override fun getIconHeight() = 8
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
