package example

import java.awt.*
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.max

private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = CalendarTable()
var currentLocalDate: LocalDate = LocalDate.of(2020, 8, 1)
  private set

fun createUI(): Component {
  monthTable.setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())
  monthTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  monthTable.cellSelectionEnabled = true
  monthTable.fillsViewportHeight = true

  val header = monthTable.tableHeader
  header.resizingAllowed = false
  header.reorderingAllowed = false
  (header.defaultRenderer as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
  updateMonthView(LocalDate.of(2020, 8, 1))

  val prev = JButton("<")
  prev.addActionListener { updateMonthView(currentLocalDate.minusMonths(1)) }

  val next = JButton(">")
  next.addActionListener { updateMonthView(currentLocalDate.plusMonths(1)) }

  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  val scroll = JScrollPane(monthTable)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateMonthView(localDate: LocalDate) {
  val pattern = DateTimeFormatter
    .ofPattern("yyyy / MM")
    .withLocale(Locale.getDefault())
  currentLocalDate = localDate
  monthLabel.text = localDate.format(pattern)
  monthTable.model = CalendarViewTableModel(localDate)
}

private class CalendarTableRenderer : DefaultTableCellRenderer {
  private val cellBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  private val sub = JLabel()
  private val panel = JPanel(BorderLayout())
  private val layer = JLayer(panel, DiagonallySplitCellLayerUI())

  constructor() {
    sub.setBorder(cellBorder)
    sub.setOpaque(false)
    sub.setVerticalAlignment(BOTTOM)
    sub.setHorizontalAlignment(RIGHT)
  }

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
    return if (value is LocalDate && c is JLabel) {
      val model = table.model
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = TOP
      c.horizontalAlignment = LEFT
      updateCellColors(value, table, c, c)
      val isLastRow = row == model.rowCount - 1
      val nextWeekDay = value.plusDays(model.columnCount.toLong())
      if (isLastRow && isCurrentMonth(nextWeekDay)) {
        sub.text = nextWeekDay.dayOfMonth.toString()
        sub.font = c.font
        sub.border = cellBorder
        c.border = cellBorder
        panel.removeAll()
        panel.layout = BorderLayout()
        panel.add(c, BorderLayout.NORTH)
        panel.add(sub, BorderLayout.SOUTH)
        panel.border = c.border
        updateCellColors(value, table, sub, panel)
        layer
      } else {
        c
      }
    } else {
      c
    }
  }

  private fun updateCellColors(
    d: LocalDate,
    table: JTable,
    fgc: JComponent,
    bgc: JComponent,
  ) {
    fgc.foreground = if (isCurrentMonth(d)) table.foreground else Color.GRAY
    bgc.background = getDayOfWeekColor(table, d.getDayOfWeek())
  }

  private fun getDayOfWeekColor(table: JTable, dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
    DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
    else -> table.background
  }

  private fun isCurrentMonth(d: LocalDate) =
    YearMonth.from(d).equals(YearMonth.from(currentLocalDate))
}

private class CalendarTable : JTable() {
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

private class CalendarViewTableModel(
  date: LocalDate,
) : DefaultTableModel() {
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
