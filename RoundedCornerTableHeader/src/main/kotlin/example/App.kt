package example

import java.awt.*
import java.awt.geom.Path2D
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

  override fun doLayout() {
    super.doLayout()
    (SwingUtilities.getAncestorOfClass(JViewport::class.java, this) as? JViewport)?.also {
      updateRowsHeight(it)
    }
  }
}
private val monthThemeColor = listOf(
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

fun makeUI(): Component {
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
    it.columnHeader = object : JViewport() {
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.height = 24
        return d
      }
    }
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
  val color = monthThemeColor[localDate.monthValue - 1]
  val fmt = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
  monthLabel.text = localDate.format(fmt)
  monthLabel.foreground = color
  monthTable.model = CalendarViewTableModel(localDate)
  monthTable.tableHeader.background = color
}

private class CalendarTableRenderer : DefaultTableCellRenderer() {
  private val p = JPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    selected: Boolean,
    focused: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, false, false, row, column)
    if (value is LocalDate && c is JLabel) {
      c.text = value.dayOfMonth.toString()
      c.verticalAlignment = TOP
      c.horizontalAlignment = CENTER
      updateCellWeekColor(value, c)
      val nextWeekDay = value.plusDays(7)
      val isLastRow = row == table.model.rowCount - 1
      if (isLastRow && YearMonth.from(nextWeekDay) == YearMonth.from(currentLocalDate)) {
        val sub = JLabel(nextWeekDay.dayOfMonth.toString()).also {
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
  private val firstLabel = object : JLabel() {
    public override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = background
      val r = 8.0
      val x = 0.0
      val y = 0.0
      val w = width.toDouble()
      val h = height.toDouble()
      val p = Path2D.Double()
      p.moveTo(x, y + r)
      p.quadTo(x, y, x + r, y)
      p.lineTo(x + w, y)
      p.lineTo(x + w, y + h)
      p.lineTo(x + r, y + h)
      p.quadTo(x, y + h, x, y + h - r)
      p.closePath()
      g2.fill(p)
      g2.dispose()
      super.paintComponent(g)
    }
  }
  private val lastLabel = object : JLabel() {
    public override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = background
      val r = 8.0
      val x = 0.0
      val y = 0.0
      val w = width.toDouble()
      val h = height.toDouble()
      val p = Path2D.Double()
      p.moveTo(x, y)
      p.lineTo(x + w - r, y)
      p.quadTo(x + w, y, x + w, y + r)
      p.lineTo(x + w, y + h - r)
      p.quadTo(x + w, y + h, x + w - r, y + h)
      p.lineTo(x, y + h)
      p.closePath()
      g2.fill(p)
      g2.dispose()
      super.paintComponent(g)
    }
  }

  init {
    firstLabel.isOpaque = false
    lastLabel.isOpaque = false
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    firstLabel.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
    lastLabel.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  }

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
    val l = when (column) {
      0 -> firstLabel
      table.columnCount - 1 -> lastLabel
      else -> c as? JLabel ?: JLabel()
    }
    l.font = table.font
    l.text = value?.toString() ?: ""
    l.foreground = table.tableHeader.foreground
    l.background = table.tableHeader.background
    l.horizontalAlignment = CENTER
    return l
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

  override fun getRowCount() = 5

  override fun getColumnCount() = 7

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
    if (c is JLayer<*>) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
