package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  private fun updateRowsHeight(vp: JViewport) {
    val height = vp.extentSize.height
    val rowCount = model.rowCount
    val defaultRowHeight = height / rowCount
    var remainder = height % rowCount
    for (i in 0 until rowCount) {
      val a = 1.coerceAtMost(0.coerceAtLeast(remainder--))
      setRowHeight(i, defaultRowHeight + a)
    }
  }

  override fun doLayout() {
    super.doLayout()
    (SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport)?.also {
      updateRowsHeight(it)
    }
  }
}
var currentLocalDate: LocalDate = LocalDate.of(2020, 8, 1)
  private set

fun updateMonthView(localDate: LocalDate) {
  currentLocalDate = localDate
  monthLabel.text = localDate.format(DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault()))
  monthTable.model = CalendarViewTableModel(localDate)
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val p = JPanel()
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
      val nextWeekDay = value.plusDays(7)
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = SwingConstants.TOP
      c.horizontalAlignment = SwingConstants.LEFT
      updateCellWeekColor(value, c, c)

      val isLastRow = row == table.model.rowCount - 1
      if (isLastRow && YearMonth.from(value.plusDays(7)) == YearMonth.from(currentLocalDate)) {
        val sub = JLabel(nextWeekDay.dayOfMonth.toString())
        sub.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        sub.isOpaque = false
        sub.verticalAlignment = SwingConstants.BOTTOM
        sub.horizontalAlignment = SwingConstants.RIGHT
        p.removeAll()
        p.layout = BorderLayout()
        p.add(sub, BorderLayout.SOUTH)
        p.add(c, BorderLayout.NORTH)
        p.border = c.border
        c.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
        updateCellWeekColor(value, sub, p)
        return JLayer(p, DiagonallySplitCellLayerUI())
      }
    }
    return c
  }

  private fun updateCellWeekColor(d: LocalDate, fgc: JComponent, bgc: JComponent) {
    if (YearMonth.from(d) == YearMonth.from(currentLocalDate)) {
      fgc.foreground = Color.BLACK
    } else {
      fgc.foreground = Color.GRAY
    }
    bgc.background = getDayOfWeekColor(d.dayOfWeek)
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFFDCDC)
    DayOfWeek.SATURDAY -> Color(0xDCDCFF)
    else -> Color.WHITE
  }
}

fun makeUI(): Component {
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

private class CalendarViewTableModel(date: LocalDate) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())
  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String =
    weekFields.firstDayOfWeek.plus(column.toLong())
      .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 5

  override fun getColumnCount() = 7

  override fun getValueAt(row: Int, column: Int): Any =
    startDate.plusDays(row.toLong() * columnCount + column)

  override fun isCellEditable(row: Int, column: Int) = false

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }
}

private class DiagonallySplitCellLayerUI : LayerUI<JPanel>() {
  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
