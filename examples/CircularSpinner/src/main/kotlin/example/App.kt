package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import javax.swing.*
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.pow

fun createUI(): Component {
  val size = 32f
  val stroke = 4f
  return JPanel(GridLayout(2, 2)).also {
    it.add(SimpleStrokeSpinner(size, stroke))
    it.add(SimpleAreaSpinner(size, stroke))
    it.add(MaterialStrokeSpinner(size, stroke))
    it.add(MaterialAreaSpinner(size, stroke))
    it.preferredSize = Dimension(320, 240)
  }
}

// A value object that only holds the starting angle
// and sweep angle of the arc for one frame.
private data class ArcAngles(
  val startAngle: Float,
  val sweepAngle: Float,
)

// An abstract class that integrates size management
// and timer management common to the four spinners.
private abstract class AbstractCircularSpinner(
  protected val size: Float,
  protected val stroke: Float,
) : JComponent() {
  protected val timer: Timer = Timer(16) { repaint() }
  protected val startTime = System.currentTimeMillis()

  init {
    this.timer.start()
  }

  override fun getPreferredSize(): Dimension {
    val totalSize = ceil((size + stroke).toDouble()).toInt()
    return Dimension(totalSize, totalSize)
  }

  override fun removeNotify() {
    super.removeNotify()
    timer.stop()
  }

  // Template method body
  // Only two functions, 'finding the angle' and 'drawing based on the angle',
  // are delegated to the subclass.
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = createValidatedGraphics2D(g)
    val x = (getWidth() - size) / 2f
    val y = (getHeight() - size) / 2f
    val elapsed = System.currentTimeMillis() - startTime
    val arc = computeArcAngles(elapsed)
    paintArc(g2, x, y, arc)
    g2.dispose()
  }

  // Template method 1:
  // Find the angle of the arc from the elapsed time
  abstract fun computeArcAngles(elapsedMillis: Long): ArcAngles

  // Template method 2:
  // Actual drawing after determining the angle
  abstract fun paintArc(g2: Graphics2D, x: Float, y: Float, arc: ArcAngles)

  // Common Graphics2D initialization process
  fun createValidatedGraphics2D(g: Graphics): Graphics2D {
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_RENDERING,
      RenderingHints.VALUE_RENDER_QUALITY,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
    return g2
  }

  // Common ring generation used in Area drawing system
  protected fun createRing(x: Float, y: Float): Area {
    val outer = Area(Ellipse2D.Float(x, y, size, size))
    val innerSize = size - stroke * 2f
    val innerOffset = (size - innerSize) / 2f
    val ax = x + innerOffset
    val ay = y + innerOffset
    val inner = Area(Ellipse2D.Float(ax, ay, innerSize, innerSize))
    outer.subtract(inner)
    return outer
  }

  protected fun createArcArea(
    ring: Area,
    startAngle: Float,
    sweepAngle: Float,
  ): Area {
    val x = (getWidth() - size) / 2f
    val y = (getHeight() - size) / 2f
    val cx = x + size / 2f
    val cy = y + size / 2f
    val r = size / 2f + 1f
    val arc = Arc2D.Float(
      cx - r,
      cy - r,
      r * 2f,
      r * 2f,
      startAngle,
      sweepAngle,
      Arc2D.PIE,
    )
    val arcSector = Area(arc)
    val arcArea = Area(ring)
    arcArea.intersect(arcSector)
    return arcArea
  }
}

// Intermediate abstract class that summarizes angle calculations
// for simple systems (uniform rotation)
private abstract class SimpleSpinner(
  size: Float,
  stroke: Float,
) : AbstractCircularSpinner(size, stroke) {
  override fun computeArcAngles(elapsedMillis: Long): ArcAngles {
    val angle = elapsedMillis / 16f * 10f % 360f
    return ArcAngles(-angle, 90f)
  }
}

// Intermediate abstract class that summarizes angle calculations
// for Material system (stretch/contract/rotate)
private abstract class MaterialSpinner(
  size: Float,
  stroke: Float,
) : AbstractCircularSpinner(size, stroke) {
  override fun computeArcAngles(elapsedMillis: Long): ArcAngles {
    val totalCycles = elapsedMillis / CYCLE_DURATION
    val cycleIndex = floor(totalCycles.toDouble()).toInt()
    val t = totalCycles - cycleIndex
    val sweepRange = MAX_SWEEP - MIN_SWEEP
    val cycleOffset = cycleIndex * sweepRange

    val head: Float
    val tail: Float
    val isFirstHalf = t < .5f
    if (isFirstHalf) {
      head = cycleOffset + MIN_SWEEP + (sweepRange * easeInOutCubic(t / .5f))
      tail = cycleOffset
    } else {
      head = cycleOffset + MAX_SWEEP
      tail = cycleOffset + (sweepRange * easeInOutCubic((t - .5f) / .5f))
    }

    val baseSpin = -360f * (elapsedMillis / 3000f)
    val startAngle = baseSpin - tail
    val sweepAngle = tail - head
    return ArcAngles(startAngle, sweepAngle)
  }

  fun easeInOutCubic(x: Float) = if (x < .5f) {
    4f * x * x * x
  } else {
    1f - (-2f * x + 2f).toDouble().pow(3.0).toFloat() / 2f
  }

  companion object {
    private const val CYCLE_DURATION = 1332f
    private const val MAX_SWEEP = 270f
    private const val MIN_SWEEP = 15f
  }
}

// 1. Constant speed rotation × line drawing
private class SimpleStrokeSpinner(
  size: Float,
  stroke: Float,
) : SimpleSpinner(size, stroke) {
  override fun paintArc(g2: Graphics2D, x: Float, y: Float, arc: ArcAngles) {
    g2.stroke = BasicStroke(
      stroke,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_ROUND,
    )
    g2.color = Color(0x34_98_DB)
    g2.draw(
      Arc2D.Float(
        x,
        y,
        size,
        size,
        arc.startAngle,
        arc.sweepAngle,
        Arc2D.OPEN,
      ),
    )
  }
}

// 2. Constant speed rotation × Area drawing
private class SimpleAreaSpinner(
  size: Float,
  stroke: Float,
) : SimpleSpinner(size, stroke) {
  override fun paintArc(g2: Graphics2D, x: Float, y: Float, arc: ArcAngles) {
    val ring = createRing(x, y)
    g2.color = Color.LIGHT_GRAY
    g2.fill(ring)
    g2.color = Color(0x34_98_DB)
    g2.fill(createArcArea(ring, arc.startAngle, arc.sweepAngle))
  }
}

// 3. Stretch/contract rotation × line drawing
private class MaterialStrokeSpinner(
  size: Float,
  stroke: Float,
) : MaterialSpinner(size, stroke) {
  override fun paintArc(g2: Graphics2D, x: Float, y: Float, arc: ArcAngles) {
    g2.stroke = BasicStroke(
      stroke,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_ROUND,
    )
    g2.color = Color(0xFF_00_00)
    g2.draw(
      Arc2D.Float(
        x,
        y,
        size,
        size,
        arc.startAngle,
        arc.sweepAngle,
        Arc2D.OPEN,
      ),
    )
  }
}

// 4. Stretch/contract rotation × Area drawing
private class MaterialAreaSpinner(
  size: Float,
  stroke: Float,
) : MaterialSpinner(size, stroke) {
  override fun paintArc(g2: Graphics2D, x: Float, y: Float, arc: ArcAngles) {
    val ring = createRing(x, y)
    g2.color = Color.LIGHT_GRAY
    g2.fill(ring)
    g2.color = Color(0xFF_00_00)
    g2.fill(createArcArea(ring, arc.startAngle, arc.sweepAngle))
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
