package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
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
import javax.swing.table.TableCellRenderer

private val monthLabel = JLabel("", SwingConstants.CENTER)
private val monthTable = object : JTable() {
  private val pt = Point(-1000, -1000)
  private var listener: MouseAdapter? = null

  private fun updateRowsHeight(vp: JViewport) {
    val height = vp.extentSize.height
    val rowCount = model.rowCount
    val defaultRowHeight = height / rowCount
    var remainder = height % rowCount
    for (i in 0..<rowCount) {
      val a = 1.coerceAtMost(0.coerceAtLeast(remainder--))
      setRowHeight(i, 1.coerceAtLeast(defaultRowHeight + a))
    }
  }

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    super.updateUI()
    fillsViewportHeight = true
    background = Color.WHITE
    setShowGrid(false)
    intercellSpacing = Dimension(2, 2)
    font = font.deriveFont(Font.BOLD)
    setOpaque(false)
    setDefaultRenderer(LocalDate::class.java, CalendarTableRenderer())
    val header = getTableHeader()
    val cm = getColumnModel()
    val r = CenterAlignmentHeaderRenderer()
    EventQueue.invokeLater {
      for (i in 0..<cm.columnCount) {
        cm.getColumn(i).setHeaderRenderer(r)
      }
    }
    header.setResizingAllowed(false)
    header.setReorderingAllowed(false)
    listener = SpotlightListener().also {
      addMouseListener(it)
      addMouseMotionListener(it)
    }
  }

  override fun doLayout() {
    super.doLayout()
    val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, this)
    if (c is JViewport) {
      updateRowsHeight(c)
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.composite = AlphaComposite.Src
    val center = Point2D.Float(pt.x.toFloat(), pt.y.toFloat())
    val dist = floatArrayOf(0.0f, 0.5f, 1.0f)
    val colors = arrayOf(Color.GRAY, Color.LIGHT_GRAY, Color.WHITE)
    val cr = getCellRect(0, 0, true)
    val r = (cr.width.coerceAtLeast(cr.height) * 2).toFloat()
    g2.paint = RadialGradientPaint(center, r, dist, colors)
    val r2 = r + r
    g2.fill(Ellipse2D.Float(pt.x - r, pt.y - r, r2, r2))
    g2.dispose()
    super.paintComponent(g)
  }

  inner class SpotlightListener : MouseAdapter() {
    override fun mouseExited(e: MouseEvent) {
      pt.setLocation(-1000, -1000)
      repaint()
    }

    override fun mouseEntered(e: MouseEvent) {
      update(e)
    }

    override fun mouseDragged(e: MouseEvent) {
      update(e)
    }

    override fun mouseMoved(e: MouseEvent) {
      update(e)
    }

    private fun update(e: MouseEvent) {
      pt.location = e.getPoint()
      var cr = getCellRect(0, 0, true)
      var r = cr.width.coerceAtLeast(cr.height) * 2
      var r2 = r + r
      repaint(Rectangle(pt.x - r, pt.y - r, r2, r2))
    }
  }
}
var currentLocalDate: LocalDate = LocalDate.of(2021, 6, 21)
  private set

fun makeUI(): Component {
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

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun updateMonthView(localDate: LocalDate) {
  currentLocalDate = localDate
  val fmt = DateTimeFormatter.ofPattern("yyyy / MM").withLocale(Locale.getDefault())
  monthLabel.text = localDate.format(fmt)
  monthTable.model = CalendarViewTableModel(localDate)
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
      val nextWeekDay = value.plusDays(7)
      val isLastRow = row == table.model.rowCount - 1
      val m1 = YearMonth.from(nextWeekDay).monthValue
      val m2 = YearMonth.from(currentLocalDate).monthValue
      if (isLastRow && m1 == m2) {
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
    val m1 = YearMonth.from(d).monthValue
    val m2 = YearMonth.from(currentLocalDate).monthValue
    if (m1 == m2) {
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

private class CenterAlignmentHeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      c.horizontalAlignment = SwingConstants.CENTER
    }
    return c
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
