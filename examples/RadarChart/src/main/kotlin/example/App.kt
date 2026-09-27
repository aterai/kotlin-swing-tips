package example

import java.awt.*
import java.awt.geom.Line2D
import java.awt.geom.Path2D
import java.awt.geom.Point2D
import javax.swing.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

fun createUI(): Component {
  val chart = RadarChartPanel()
  chart.background = Color.WHITE
  chart.componentPopupMenu = createPopupMenu(chart)
  return JPanel(BorderLayout()).also {
    it.add(chart)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createPopupMenu(chart: RadarChartPanel): JPopupMenu {
  val popup = JPopupMenu()
  popup.add(createSidesMenu(chart))
  popup.add(createGridMenu(chart))
  val check = JCheckBoxMenuItem("Show scale labels", chart.isScaleLabelVisible)
  check.addActionListener { chart.isScaleLabelVisible = check.isSelected }
  popup.add(check)
  popup.addSeparator()
  popup.add("Add series").addActionListener { chart.addSeries() }
  popup.add("Remove series").addActionListener { chart.removeSeries() }
  popup.add("Randomize values").addActionListener { chart.randomize() }
  return popup
}

private fun createSidesMenu(chart: RadarChartPanel): JMenu {
  val menu = JMenu("Sides")
  val bg = ButtonGroup()
  listOf(5, 6, 7, 8, 10, 12).forEach { n ->
    val item = JRadioButtonMenuItem(n.toString(), n == chart.sides)
    item.addActionListener { chart.sides = n }
    bg.add(item)
    menu.add(item)
  }
  return menu
}

private fun createGridMenu(chart: RadarChartPanel): JMenu {
  val menu = JMenu("Grid")
  val bg = ButtonGroup()
  GridStyle.entries.forEach { style ->
    val item = JRadioButtonMenuItem(style.toString(), style == chart.gridStyle)
    item.addActionListener { chart.gridStyle = style }
    bg.add(item)
    menu.add(item)
  }
  return menu
}

private enum class GridStyle(
  private val label: String,
) {
  NONE("None"),
  TICK("Tick marks"),
  POLYGON("Polygons"),
  ;

  override fun toString() = label
}

private class RadarChartPanel : JPanel() {
  private val series = mutableListOf<DoubleArray>()

  var sides = 6
    set(value) {
      if (field != value) {
        field = value
        // The number of values per series changes, so regenerate all series
        randomize()
      }
    }

  var gridStyle = GridStyle.POLYGON
    set(value) {
      field = value
      repaint()
    }

  var isScaleLabelVisible = true
    set(value) {
      field = value
      repaint()
    }

  init {
    repeat(3) {
      series.add(createRandomValues())
    }
  }

  fun addSeries() {
    if (series.size < MAX_SERIES) {
      series.add(createRandomValues())
      repaint()
    }
  }

  fun removeSeries() {
    if (series.isNotEmpty()) {
      series.removeAt(series.lastIndex)
      repaint()
    }
  }

  fun randomize() {
    series.replaceAll { createRandomValues() }
    repaint()
  }

  private fun createRandomValues() = DoubleArray(sides) { 20.0 + Random.nextInt(81) }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val r = SwingUtilities.calculateInnerArea(this, null)
    if (r.isEmpty) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
    // Draw the chart in a 1000x1000 space and fit it into the panel,
    // keeping the aspect ratio and centering it
    val scale = min(r.getWidth(), r.getHeight()) / RadarChart.SIZE
    g2.translate(r.centerX, r.centerY)
    g2.scale(scale, scale)
    g2.translate(-RadarChart.SIZE / 2.0, -RadarChart.SIZE / 2.0)
    RadarChart.drawGrid(g2, sides, gridStyle)
    if (isScaleLabelVisible) {
      RadarChart.drawScaleLabels(g2)
    }
    series.forEachIndexed { i, values ->
      RadarChart.drawSeries(g2, values, RadarChart.getSeriesColor(i))
    }
    g2.dispose()
  }

  companion object {
    private const val MAX_SERIES = 8
  }
}

private object RadarChart {
  const val SIZE = 1000.0
  private const val CENTER = SIZE / 2.0
  private const val RADIUS = 420.0
  private const val MAX_VALUE = 100.0
  private const val DIVISIONS = 10
  private const val TICK_LENGTH = 12.0
  private const val LABEL_OFFSET = 8.0
  private const val FILL_ALPHA = 0x4D // .3
  private const val LINE_ALPHA = 0xCC // .8
  private val GRID_COLOR = Color(0x99_99_99)
  private val AXIS_COLOR = Color(0x66_66_66)
  private val GRID_STROKE = BasicStroke(3f)
  private val SERIES_STROKE = BasicStroke(4f)
  private val LABEL_FONT = Font(Font.SANS_SERIF, Font.PLAIN, 28)

  // The first vertex is at the 12 o'clock position
  private fun getAngle(idx: Int, sides: Int) = -PI / 2.0 + 2.0 * PI * idx / sides

  private fun getPoint(idx: Int, sides: Int, value: Double): Point2D {
    val angle = getAngle(idx, sides)
    val r = RADIUS * value / MAX_VALUE
    return Point2D.Double(CENTER + r * cos(angle), CENTER + r * sin(angle))
  }

  private fun createPolygon(values: DoubleArray): Path2D {
    val sides = values.size
    val path = Path2D.Double()
    values.forEachIndexed { i, v ->
      val pt = getPoint(i, sides, v)
      if (i == 0) {
        path.moveTo(pt.x, pt.y)
      } else {
        path.lineTo(pt.x, pt.y)
      }
    }
    path.closePath()
    return path
  }

  private fun createRegularPolygon(sides: Int, value: Double) =
    createPolygon(DoubleArray(sides) { value })

  fun drawGrid(g2: Graphics2D, sides: Int, style: GridStyle) {
    g2.stroke = GRID_STROKE
    if (style == GridStyle.POLYGON) {
      g2.color = GRID_COLOR
      for (i in 1..<DIVISIONS) {
        g2.draw(createRegularPolygon(sides, MAX_VALUE * i / DIVISIONS))
      }
    }
    // Axes and the outer frame are always drawn
    g2.color = AXIS_COLOR
    val axis = Line2D.Double()
    for (i in 0..<sides) {
      val pt = getPoint(i, sides, MAX_VALUE)
      axis.setLine(CENTER, CENTER, pt.x, pt.y)
      g2.draw(axis)
      if (style == GridStyle.TICK) {
        drawTicks(g2, i, sides)
      }
    }
    g2.draw(createRegularPolygon(sides, MAX_VALUE))
  }

  // Short lines perpendicular to the axis at each division
  private fun drawTicks(g2: Graphics2D, idx: Int, sides: Int) {
    val angle = getAngle(idx, sides)
    val dx = -sin(angle) * TICK_LENGTH / 2.0
    val dy = cos(angle) * TICK_LENGTH / 2.0
    val tick = Line2D.Double()
    for (i in 1..<DIVISIONS) {
      val pt = getPoint(idx, sides, MAX_VALUE * i / DIVISIONS)
      tick.setLine(pt.x - dx, pt.y - dy, pt.x + dx, pt.y + dy)
      g2.draw(tick)
    }
  }

  // Draw the scale values to the right of the 12 o'clock axis (0 is omitted)
  fun drawScaleLabels(g2: Graphics2D) {
    g2.font = LABEL_FONT
    g2.color = AXIS_COLOR
    val fm = g2.fontMetrics
    // Offset from the tick position to the baseline to center the text vertically
    val baseline = (fm.ascent - fm.descent) / 2.0
    val tx = (CENTER + LABEL_OFFSET).toFloat()
    for (i in 1..DIVISIONS) {
      val y = CENTER - RADIUS * i / DIVISIONS
      val label = (MAX_VALUE * i / DIVISIONS).toInt().toString()
      g2.drawString(label, tx, (y + baseline).toFloat())
    }
  }

  fun drawSeries(g2: Graphics2D, values: DoubleArray, color: Color) {
    val path = createPolygon(values)
    g2.color = withAlpha(color, FILL_ALPHA)
    g2.fill(path)
    g2.stroke = SERIES_STROKE
    g2.color = withAlpha(color, LINE_ALPHA)
    g2.draw(path)
  }

  fun getSeriesColor(idx: Int): Color {
    // Golden ratio steps keep neighboring hues well separated
    val hue = (idx * .618 % 1.0).toFloat()
    return Color.getHSBColor(hue, .8f, .85f)
  }

  private fun withAlpha(c: Color, alpha: Int) = Color(c.red, c.green, c.blue, alpha)
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
