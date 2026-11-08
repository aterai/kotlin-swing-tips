package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import javax.swing.*
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

fun createUI(): Component {
  // A single BoundedRangeModel is shared by the dial and the slider,
  // so they stay in sync without any extra listener code.
  val model = DefaultBoundedRangeModel(3, 0, 0, 10)

  val slider = JSlider(model)
  slider.setMajorTickSpacing(1)
  slider.setPaintTicks(true)
  slider.setPaintLabels(true)
  slider.setSnapToTicks(true)

  val p = JPanel(BorderLayout())
  p.add(slider)
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(VolumeDial(model, "VOLUME"))
    it.preferredSize = Dimension(320, 240)
  }
}

// A volume-knob style dial: the model range is spread over a 270-degree arc
// that starts at the lower left (minimum) and sweeps clockwise to the lower
// right (maximum). The knob never wraps around: turning it counterclockwise
// at the minimum or clockwise at the maximum leaves the value unchanged.
// An LED-style dot sits at every tick outside the ring; the dots up to the
// current value are lit.
private class VolumeDial(
  val model: BoundedRangeModel,
  private val title: String,
) : JPanel() {
  private var listener: MouseAdapter? = null
  private var labelColor: Color? = null
  private var dialHovered = false

  // Rotation of the knob in degrees within [0, SWEEP_ANGLE], accumulated
  // from pointer movement while dragging so it cannot jump across the gap
  // at the bottom of the dial.
  private var dragAngle = 0.0
  private var prevPointerAngle = 0.0

  private val degreesPerStep: Double
    get() = SWEEP_ANGLE / (model.maximum - model.minimum)

  // Current knob rotation from the minimum position in degrees, within [0, SWEEP_ANGLE].
  private val knobAngle: Double
    get() = (model.value - model.minimum) * degreesPerStep

  init {
    model.addChangeListener { repaint() }
  }

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    removeMouseWheelListener(listener)
    super.updateUI()
    // Follow the current LookAndFeel (e.g. dark themes) for the text color.
    labelColor = JLabel().foreground
    listener = DragMouseListener().also {
      addMouseListener(it)
      addMouseMotionListener(it)
      addMouseWheelListener(it)
    }
  }

  override fun getPreferredSize() = Dimension(240, 240)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g) // clears the background with the theme color
    // The background is painted by super.paintComponent(...) so the dial
    // follows light/dark themes; derive the other colors from it.
    val bg = getBackground()
    val fg = labelColor ?: return
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
    val rect = SwingUtilities.calculateInnerArea(this, null)
    g2.translate(rect.centerX, rect.centerY)
    val radius = getRadius(rect)

    // Ring: a filled disc behind the knob
    g2.color = mix(bg, fg, .55)
    g2.fill(createCircle(Point2D.Double(), radius))

    paintScale(g2, radius, bg, fg)
    paintKnob(g2, radius, bg, fg)
    g2.dispose()
  }

  // LED-style dots, value labels and the title around the ring.
  private fun paintScale(g2: Graphics2D, radius: Double, bg: Color, fg: Color) {
    // LED-style dots outside the ring, one per step; the dots from the
    // minimum up to the current value are lit, the rest are dimmed.
    val ledDistance = radius + LED_OFFSET
    val unlitColor = mix(bg, fg, .25)
    for (v in model.minimum..model.maximum) {
      g2.color = if (v <= model.value) LED_COLOR else unlitColor
      g2.fill(createCircle(toPoint(toAngle(v), ledDistance), LED_RADIUS))
    }

    // Value labels outside the LEDs, one per step; labels up to the current
    // value share the lit LED color.
    val frc = g2.fontRenderContext
    val labelDistance = ledDistance + LED_RADIUS
    for (v in model.minimum..model.maximum) {
      val deg = toAngle(v)
      g2.color = if (v <= model.value) LED_COLOR else fg
      val s = TextLayout(v.toString(), g2.font, frc).getOutline(null)
      g2.fill(createShapeAtPolarPosition(s, deg, labelDistance + getLabelOffset(s, deg)))
    }

    // Title: centered just below the rim, in the gap between the first and
    // the last label.
    g2.color = fg
    val ts = TextLayout(title, g2.font, frc).getOutline(null)
    g2.fill(createShapeAtPolarPosition(ts, 180.0, radius + getLabelOffset(ts, 180.0)))
  }

  // The indicator mark on the ring and the knob on top of it.
  private fun paintKnob(g2: Graphics2D, radius: Double, bg: Color, fg: Color) {
    // Indicator: a line from the center toward the rim; the knob painted
    // next covers the inner part, leaving a short mark on the ring.
    val tip = toPoint(toAngle(model.value), radius - INDICATOR_GAP)
    g2.color = bg
    g2.stroke =
      BasicStroke(INDICATOR_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND)
    g2.draw(Line2D.Double(0.0, 0.0, tip.x, tip.y))

    // Knob: a lighter disc with a border in the background color
    val knob = createCircle(Point2D.Double(), radius * KNOB_RATIO)
    g2.color = mix(bg, fg, if (dialHovered) .2 else .3)
    g2.fill(knob)
    g2.color = bg
    g2.stroke = BasicStroke(KNOB_BORDER_WIDTH)
    g2.draw(knob)
  }

  // Direction of the given model value: degrees clockwise from 12 o'clock.
  private fun toAngle(value: Int) = START_ANGLE + (value - model.minimum) * degreesPerStep

  // Point relative to the component's inner-area center
  // (used for hit-testing and angle calc).
  private fun toCenterRelativePoint(p: Point): Point2D {
    val rect = SwingUtilities.calculateInnerArea(this, null)
    return Point2D.Double(p.getX() - rect.centerX, p.getY() - rect.centerY)
  }

  // True if the center-relative point lies on the ring or the knob.
  private fun isInsideDial(pt: Point2D): Boolean {
    val rect = SwingUtilities.calculateInnerArea(this, null)
    return pt.distance(0.0, 0.0) <= getRadius(rect)
  }

  private inner class DragMouseListener : MouseAdapter() {
    override fun mouseMoved(e: MouseEvent) {
      updateHover(e.component, isInsideDial(toCenterRelativePoint(e.point)))
    }

    override fun mousePressed(e: MouseEvent) {
      val pt = toCenterRelativePoint(e.point)
      if (isInsideDial(pt)) {
        updateHover(e.component, true)
        // Start from the current knob rotation and follow the pointer's
        // relative movement so the knob does not jump to the pointer.
        dragAngle = knobAngle
        prevPointerAngle = toDegrees(pt)
        model.valueIsAdjusting = true
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (model.valueIsAdjusting) {
        val deg = toDegrees(toCenterRelativePoint(e.point))
        val delta = wrapDelta(deg - prevPointerAngle)
        prevPointerAngle = deg
        // Clamping here is what stops the knob at both ends; the value can
        // never wrap from the minimum to the maximum or vice versa.
        dragAngle = (dragAngle + delta).coerceIn(0.0, SWEEP_ANGLE)
        // Snap to the nearest tick.
        val step = (dragAngle / degreesPerStep).roundToInt()
        model.value = model.minimum + step
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      model.valueIsAdjusting = false
      mouseMoved(e)
    }

    override fun mouseExited(e: MouseEvent) {
      if (!model.valueIsAdjusting) {
        updateHover(e.component, false)
      }
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      model.value = model.value - e.wheelRotation
      if (model.valueIsAdjusting) {
        // Keep an ongoing drag in step with the wheel; otherwise the next
        // drag event would move the knob back to the stale dragAngle.
        dragAngle = knobAngle
      }
    }

    // Repaints and updates the cursor only when the hover state changes,
    // not on every mouse move.
    private fun updateHover(c: Component, hovered: Boolean) {
      if (dialHovered != hovered) {
        dialHovered = hovered
        c.cursor = if (hovered) {
          Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        } else {
          Cursor.getDefaultCursor()
        }
        c.repaint()
      }
    }
  }

  companion object {
    // Direction of the minimum value: degrees clockwise from 12 o'clock.
    private const val START_ANGLE = 225.0
    private const val SWEEP_ANGLE = 270.0
    private const val MARGIN = 36.0 // room for the LEDs and labels outside the ring
    private const val KNOB_RATIO = .62 // knob radius relative to the ring radius
    private const val KNOB_BORDER_WIDTH = 3f
    private const val INDICATOR_WIDTH = 4f
    private const val INDICATOR_GAP = 4.0 // gap between the indicator tip and the rim
    private const val LED_OFFSET = 7.0 // distance from the rim to the LED center
    private const val LED_RADIUS = 1.75
    private const val LABEL_GAP = 4.0 // gap between the LEDs and the label bounds
    private val LED_COLOR = Color(0x3C_A0_FF) // light blue

    private fun getRadius(rect: Rectangle) = min(rect.width, rect.height) / 2.0 - MARGIN

    // Linear interpolation between two colors: t == 0 -> c1, t == 1 -> c2.
    private fun mix(c1: Color, c2: Color, t: Double): Color {
      val r = (c1.red + (c2.red - c1.red) * t).roundToInt()
      val g = (c1.green + (c2.green - c1.green) * t).roundToInt()
      val b = (c1.blue + (c2.blue - c1.blue) * t).roundToInt()
      return Color(r, g, b)
    }

    private fun createCircle(center: Point2D, r: Double) =
      Ellipse2D.Double(center.x - r, center.y - r, 2.0 * r, 2.0 * r)

    // Distance from the LED edge to the label center so that the label's inner
    // edge clears the LEDs in the direction of `deg`.
    private fun getLabelOffset(s: Shape, deg: Double): Double {
      val r = s.bounds2D
      val rad = Math.toRadians(deg)
      val half = max(abs(cos(rad)) * r.height, abs(sin(rad)) * r.width) / 2.0
      return LABEL_GAP + half
    }

    // Polar (degrees, 0 == straight up, clockwise) to Cartesian, origin at the center.
    private fun toPoint(degrees: Double, distance: Double): Point2D {
      val rad = Math.toRadians(degrees)
      return Point2D.Double(distance * sin(rad), -distance * cos(rad))
    }

    // Cartesian (origin at the center) to degrees in [0, 360).
    private fun toDegrees(pt: Point2D) = normalize(Math.toDegrees(atan2(pt.x, -pt.y)))

    private fun normalize(degrees: Double): Double {
      val d = degrees % 360.0
      return if (d < 0.0) d + 360.0 else d
    }

    // Wraps a difference of two angles into [-180, 180) so that the pointer
    // crossing 12 o'clock does not produce a 360-degree jump.
    private fun wrapDelta(degrees: Double): Double {
      val d = normalize(degrees)
      return if (d >= 180.0) d - 360.0 else d
    }

    // Translates shape so that its bounding-box center lands on the polar position.
    private fun createShapeAtPolarPosition(
      s: Shape,
      deg: Double,
      distance: Double,
    ): Shape {
      val r = s.bounds2D
      val pt = toPoint(deg, distance)
      val dx = pt.x - r.centerX
      val dy = pt.y - r.centerY
      return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(s)
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
