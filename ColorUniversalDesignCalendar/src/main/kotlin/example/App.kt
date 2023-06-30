package example

import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

val realLocalDate: LocalDate = LocalDate.now(ZoneId.systemDefault())
private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  private fun updateRowsHeight(viewPort: JViewport) {
    val height = viewPort.extentSize.height
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
var currentLocalDate: LocalDate? = null
  private set

fun updateMonthView(localDate: LocalDate?) {
  currentLocalDate = localDate?.also {
    val formatter = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
    monthLabel.text = it.format(formatter)
    monthTable.model = CalendarViewTableModel(it)
  }
}

private class CalendarTableRenderer : TableCellRenderer {
  private val renderer = JPanel(FlowLayout(FlowLayout.LEADING))
  private val label = object : JLabel("", SwingConstants.CENTER) {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.width = 18
    }

    override fun paintComponent(g: Graphics) {
      if (background != Color.WHITE) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.paint = background
        g2.fill(getShape())
        g2.dispose()
      }
      super.paintComponent(g)
    }

    private fun getShape(): Shape = if (background == Color.BLUE) {
      Ellipse2D.Double(0.0, 0.0, size.width - 1.0, size.height - 1.0)
    } else {
      RoundRectangle2D.Double(0.0, 0.0, size.width - 1.0, size.height - 1.0, 5.0, 5.0)
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    selected: Boolean,
    focused: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.isOpaque = true
    renderer.removeAll()
    renderer.add(label)
    label.isOpaque = false
    if (value is LocalDate) {
      label.text = "<html><b>" + value.dayOfMonth.toString()
      label.border = BorderFactory.createEmptyBorder(2, 0, 3, 1)
      val isThisMonth = YearMonth.from(value) == YearMonth.from(currentLocalDate)
      if (isThisMonth && value.dayOfWeek == DayOfWeek.SUNDAY) {
        label.foreground = Color.WHITE
        label.background = Color.BLACK
      } else if (isThisMonth && value.dayOfWeek == DayOfWeek.SATURDAY) {
        label.foreground = Color.WHITE
        label.background = Color.BLUE
      } else if (isThisMonth) {
        label.background = Color.WHITE
        label.foreground = Color.BLACK
      } else {
        label.background = Color.WHITE
        label.foreground = Color.GRAY
        label.text = value.dayOfMonth.toString()
      }
      when {
        selected -> renderer.background = table.selectionBackground
        value.isEqual(realLocalDate) -> renderer.background = Color(0xDC_FF_DC)
        else -> renderer.background = getDayOfWeekColor(value.dayOfWeek)
      }
    }
    return renderer
  }

  private fun getDayOfWeekColor(dow: DayOfWeek) = when (dow) {
    DayOfWeek.SUNDAY -> Color(0xFF_DC_DC)
    DayOfWeek.SATURDAY -> Color(0xDC_DC_FF)
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
  updateMonthView(realLocalDate)
  val prev = JButton("<")
  prev.addActionListener {
    updateMonthView(currentLocalDate?.minusMonths(1))
  }
  val next = JButton(">")
  next.addActionListener {
    updateMonthView(currentLocalDate?.plusMonths(1))
  }
  val p = JPanel(BorderLayout())
  p.add(monthLabel)
  p.add(prev, BorderLayout.WEST)
  p.add(next, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(monthTable))
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CalendarViewTableModel(date: LocalDate) : DefaultTableModel() {
  private val startDate: LocalDate
  private val weekFields = WeekFields.of(Locale.getDefault())

  init {
    val firstDayOfMonth = YearMonth.from(date).atDay(1)
    val v = firstDayOfMonth[weekFields.dayOfWeek()] - 1
    startDate = firstDayOfMonth.minusDays(v.toLong())
  }

  override fun getColumnClass(column: Int) = LocalDate::class.java

  override fun getColumnName(column: Int): String =
    weekFields.firstDayOfWeek.plus(column.toLong())
      .getDisplayName(TextStyle.SHORT_STANDALONE, Locale.getDefault())

  override fun getRowCount() = 6

  override fun getColumnCount() = 7

  override fun getValueAt(row: Int, column: Int): Any =
    startDate.plusDays(row.toLong() * columnCount + column)

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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
