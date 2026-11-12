package example

import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader
import kotlin.math.max

private const val HEADER_HEIGHT = 24
private val MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy / MM")
private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  override fun updateUI() {
    super.updateUI()
    fillsViewportHeight = true
    setBackground(Color.WHITE)
    setShowVerticalLines(false)
    setShowHorizontalLines(true)
    intercellSpacing = Dimension(0, 1)
    font = font.deriveFont(Font.BOLD)
    setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())
  }

  // Fix the header height in the header itself instead of replacing the
  // JScrollPane column header viewport, which would drop the header on updateUI()
  override fun createDefaultTableHeader() = object : JTableHeader(columnModel) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = HEADER_HEIGHT
      return d
    }
  }

  private fun adjustRowHeights(viewport: JViewport) {
    if (rowCount == 0) {
      return
    }
    val height = viewport.extentSize.height
    val baseRowHeight = height / rowCount
    val remainder = height % rowCount
    for (i in 0..<rowCount) {
      val adjustedHeight = max(1, baseRowHeight + (if (i < remainder) 1 else 0))
      // Skip unchanged rows: setRowHeight(...) always revalidates the table,
      // which would schedule another doLayout() call
      if (getRowHeight(i) != adjustedHeight) {
        setRowHeight(i, adjustedHeight)
      }
    }
  }

  override fun getScrollableTracksViewportHeight() = getParent() is JViewport

  override fun doLayout() {
    super.doLayout()
    (parent as? JViewport)?.also { adjustRowHeights(it) }
  }
}
private val themeColors = listOf(
  Color(0xD5_0B_17),
  Color(0x02_6C_B6),
  Color(0xED_87_AD),
  Color(0xCE_30_6A),
  Color(0x48_B0_37),
  Color(0xA4_62_A2),
  Color(0x00_BD_E7),
  Color(0xEB_5E_31),
  Color(0xC8_01_82),
  Color(0x8F_19_19),
  Color(0x6A_31_8F),
  Color(0x00_7A_70),
)

var currentLocalDate: LocalDate = LocalDate.of(2021, 6, 21)
  private set

fun createUI(): Component {
  monthTable.tableHeader.also {
    it.foreground = Color.WHITE
    it.isOpaque = false
    it.defaultRenderer = RoundedHeaderRenderer()
    it.resizingAllowed = false
    it.reorderingAllowed = false
  }

  updateMonthView(LocalDate.of(2021, 6, 21))

  monthLabel.also {
    it.isOpaque = false
    it.font = it.font.deriveFont(Font.BOLD)
  }

  val prev = JButton("<").also {
    it.addActionListener {
      updateMonthView(currentLocalDate.minusMonths(1))
    }
  }
  val next = JButton(">").also {
    it.addActionListener {
      updateMonthView(currentLocalDate.plusMonths(1))
    }
  }

  val p = JPanel(BorderLayout()).also {
    it.isOpaque = false
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(monthLabel)
    it.add(prev, BorderLayout.WEST)
    it.add(next, BorderLayout.EAST)
  }

  val scroll = JScrollPane(monthTable).also {
    it.border = BorderFactory.createEmptyBorder()
    it.viewportBorder = BorderFactory.createEmptyBorder()
    it.viewport.background = Color.WHITE
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateMonthView(localDate: LocalDate) {
  currentLocalDate = localDate
  val color = themeColors[localDate.monthValue - 1]
  monthLabel.text = localDate.format(MONTH_FORMATTER)
  monthLabel.foreground = color
  monthTable.model = CalendarViewTableModel(localDate)
  monthTable.tableHeader.background = color
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val p = JPanel(BorderLayout())

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
      false,
      false,
      row,
      column,
    )
    if (value is LocalDate && c is JLabel) {
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = TOP
      c.horizontalAlignment = CENTER
      updateCellWeekColor(value, c)
      val model = table.model
      val nextWeekDay = value.plusDays(model.columnCount.toLong())
      val isLastRow = row == model.rowCount - 1
      // The last row also shows the date of the following week if it is still in the current month
      if (isLastRow && YearMonth.from(nextWeekDay) == YearMonth.from(currentLocalDate)) {
        val sub = JLabel(nextWeekDay.dayOfMonth.toString()).also {
          updateCellWeekColor(nextWeekDay, it)
          it.font = table.font
          it.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
          it.isOpaque = false
          it.verticalAlignment = BOTTOM
          it.horizontalAlignment = RIGHT
        }
        p.removeAll()
        p.isOpaque = false
        p.foreground = getDayOfWeekColor(value.dayOfWeek)
        p.add(sub, BorderLayout.SOUTH)
        p.add(c, BorderLayout.NORTH)
        p.border = c.border
        c.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        c.horizontalAlignment = LEFT
        updateCellWeekColor(value, sub)
        return JLayer(p, DiagonallySplitCellLayerUI())
      }
    }
    return c
  }

  private fun updateCellWeekColor(
    d: LocalDate,
    fgc: Component,
  ) {
    if (YearMonth.from(d) == YearMonth.from(currentLocalDate)) {
      fgc.foreground = getDayOfWeekColor(d.dayOfWeek)
    } else {
      fgc.foreground = Color.GRAY
    }
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xD9_0B_0D)
    DayOfWeek.SATURDAY -> Color(0x10_4A_90)
    else -> Color.BLACK
  }
}

private class RoundedHeaderRenderer : DefaultTableCellRenderer() {
  private val firstLabel = RoundedCornerLabel(true)
  private val lastLabel = RoundedCornerLabel(false)

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val l: JLabel = when (column) {
      0 -> firstLabel

      table.columnCount - 1 -> lastLabel

      else -> super.getTableCellRendererComponent(
        table,
        value,
        isSelected,
        hasFocus,
        row,
        column,
      ) as? JLabel ?: this
    }
    val header = table.tableHeader
    l.text = value?.toString() ?: ""
    l.font = table.font
    l.foreground = header.foreground
    l.background = header.background
    l.horizontalAlignment = CENTER
    return l
  }
}

private class RoundedCornerLabel(
  private val leading: Boolean,
) : JLabel() {
  override fun updateUI() {
    super.updateUI()
    isOpaque = false
    border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = background
    // Extend the round rectangle beyond the opposite edge so that
    // only the corners on the leading (or trailing) side are rounded
    val x = if (leading) 0.0 else -ARC
    val w = width + ARC
    g2.fill(RoundRectangle2D.Double(x, 0.0, w, height.toDouble(), ARC, ARC))
    g2.dispose()
    super.paintComponent(g)
  }

  companion object {
    private const val ARC = 16.0
  }
}

private class CalendarViewTableModel(
  date: LocalDate,
) : AbstractTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String =
    weekFields.firstDayOfWeek
      .plus(column.toLong())
      .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 5

  override fun getColumnCount() = DayOfWeek.entries.size // 7

  override fun getValueAt(
    row: Int,
    column: Int,
  ): Any = startDate.plusDays(row.toLong() * columnCount + column)
}

private class DiagonallySplitCellLayerUI : LayerUI<JPanel>() {
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = c.view.foreground
      g2.drawLine(c.width - 4, 4, 4, c.height - 4)
      g2.dispose()
    }
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
