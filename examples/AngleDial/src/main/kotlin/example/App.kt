package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
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
  val model = DefaultBoundedRangeModel(90, 0, 0, 360)
  val dial = AngleDial(model)

  val slider = JSlider(model)
  slider.setMajorTickSpacing(90)
  slider.setMinorTickSpacing(30)
  slider.setPaintTicks(true)
  slider.setPaintLabels(true)

  val spinnerModel = SpinnerNumberModel(
    model.value,
    model.minimum,
    model.maximum,
    1,
  )
  val spinner = JSpinner(spinnerModel)
  // Bridge the BoundedRangeModel and the SpinnerNumberModel in both directions.
  // Setting an unchanged value does not fire an event, so this never loops.
  model.addChangeListener { spinnerModel.value = model.value }
  spinnerModel.addChangeListener { model.value = spinnerModel.number.toInt() }

  val controlPanel = JPanel(BorderLayout(5, 5))
  controlPanel.add(slider)
  controlPanel.add(spinner, BorderLayout.EAST)
  controlPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  return JPanel(BorderLayout()).also {
    it.add(controlPanel, BorderLayout.NORTH)
    it.add(dial)
    it.preferredSize = Dimension(320, 240)
  }
}

// A dial that selects an angle in degrees (0 at the top, clockwise).
// The value is stored in a BoundedRangeModel so it can be shared with a JSlider.
private class AngleDial(
  val model: BoundedRangeModel,
) : JPanel() {
  private var mouseListener: MouseAdapter? = null
  private var labelColor: Color? = null
  private var handleHovered = false
  private var dragging = false
  private var dragOffset = 0.0 // degrees: model value minus pointer angle at mousePressed

  init {
    model.addChangeListener { repaint() }
  }

  override fun updateUI() {
    if (mouseListener != null) {
      removeMouseListener(mouseListener)
      removeMouseMotionListener(mouseListener)
      removeMouseWheelListener(mouseListener)
    }
    super.updateUI()
    // Follow the current LookAndFeel (e.g. dark themes) for the text color.
    labelColor = UIManager.getColor("Label.foreground")
    mouseListener = DialMouseListener()
    addMouseListener(mouseListener)
    addMouseMotionListener(mouseListener)
    addMouseWheelListener(mouseListener)
  }

  override fun getPreferredSize() = Dimension(240, 240)

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g) // clears the background with the theme color
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
    val innerArea = SwingUtilities.calculateInnerArea(this, null)
    // The background is painted by super.paintComponent(...) so the dial
    // follows light/dark themes; derive the other colors from it.
    val background = getBackground()
    val foreground = labelColor ?: return
    val dialColor = interpolateColor(background, foreground, .5)

    g2.translate(innerArea.centerX, innerArea.centerY)
    val radius = calculateDialRadius(innerArea)

    // Dial face: a filled disc in the mid-color
    g2.color = dialColor
    g2.fill(createCircle(0.0, 0.0, radius))

    // Tick marks: dots outside the ring, larger at 0/90/180/270
    g2.color = foreground
    run {
      var degrees = 0
      while (degrees < 360) {
        val dotRadius = if (degrees % LABEL_STEP == 0) {
          MAJOR_DOT_RADIUS
        } else {
          MINOR_DOT_RADIUS
        }
        val center = polarToCartesian(degrees.toDouble(), radius + DOT_GAP)
        g2.fill(createCircle(center.x, center.y, dotRadius))
        degrees += TICK_STEP
      }
    }

    // Degree labels outside the dots
    val frc = g2.fontRenderContext
    var degrees = 0
    while (degrees < 360) {
      val labelShape = TextLayout("$degrees°", g2.font, frc).getOutline(null)
      g2.fill(
        positionShapeAtAngle(
          labelShape,
          degrees.toDouble(),
          radius + calculateLabelOffset(labelShape, degrees.toDouble()),
        ),
      )
      degrees += LABEL_STEP
    }

    // Center dot
    g2.color = background
    g2.fill(createCircle(0.0, 0.0, CENTER_DOT_RADIUS))

    // Handle: a dimple pressed into the dial face, shadow at the top-left,
    // highlight at the bottom-right.
    val handleShape = createHandleShape(innerArea)
    val handleBounds = handleShape.bounds2D
    g2.paint = LinearGradientPaint(
      Point2D.Double(handleBounds.minX, handleBounds.minY),
      Point2D.Double(handleBounds.maxX, handleBounds.maxY),
      floatArrayOf(0f, .5f, 1f),
      arrayOf(
        interpolateColor(dialColor, Color.BLACK, .45),
        dialColor,
        interpolateColor(dialColor, Color.WHITE, .45),
      ),
    )
    g2.fill(handleShape)
    g2.paint = if (handleHovered) {
      foreground
    } else {
      interpolateColor(dialColor, Color.BLACK, .3)
    }
    g2.draw(handleShape)

    g2.dispose()
  }

  // Handle circle for the current model value, relative to the inner-area center.
  // Built on demand so painting and hit-testing never see a stale shape.
  private fun createHandleShape(innerArea: Rectangle): Ellipse2D {
    val distance = calculateDialRadius(innerArea) - HANDLE_GAP - HANDLE_RADIUS
    val center = polarToCartesian(model.value.toDouble(), distance)
    return createCircle(center.x, center.y, HANDLE_RADIUS)
  }

  // Hit-test with a point relative to the inner-area center.
  private fun isOnHandle(point: Point2D?): Boolean {
    val innerArea = SwingUtilities.calculateInnerArea(this, null)
    return createHandleShape(innerArea).contains(point)
  }

  // Point relative to the component's inner-area center
  // (used for hit-testing and angle calc).
  private fun toCenterRelative(point: Point): Point2D {
    val innerArea = SwingUtilities.calculateInnerArea(this, null)
    return Point2D.Double(
      point.getX() - innerArea.centerX,
      point.getY() - innerArea.centerY,
    )
  }

  // Updates the hover state, cursor and painting only when the state changes.
  private fun setHandleHovered(hovered: Boolean) {
    if (handleHovered != hovered) {
      handleHovered = hovered
      setCursor(
        if (hovered) {
          Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        } else {
          Cursor.getDefaultCursor()
        },
      )
      repaint()
    }
  }

  private inner class DialMouseListener : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      mouseMoved(e)
    }

    override fun mouseMoved(e: MouseEvent) {
      setHandleHovered(isOnHandle(toCenterRelative(e.getPoint())))
    }

    override fun mousePressed(e: MouseEvent) {
      val point = toCenterRelative(e.getPoint())
      if (SwingUtilities.isLeftMouseButton(e) && isOnHandle(point)) {
        dragging = true
        setHandleHovered(true)
        // Remember the offset so the handle does not jump to the pointer.
        dragOffset = model.value - cartesianToDegrees(point)
        model.valueIsAdjusting = true
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (dragging) {
        val point = toCenterRelative(e.getPoint())
        val degrees = normalizeDegrees(cartesianToDegrees(point) + dragOffset)
        model.value = degrees.roundToInt()
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      if (dragging && SwingUtilities.isLeftMouseButton(e)) {
        dragging = false
        model.valueIsAdjusting = false
      }
      mouseMoved(e)
    }

    override fun mouseExited(e: MouseEvent?) {
      if (!dragging) {
        setHandleHovered(false)
      }
    }

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      // Wrap around like dragging does: 359 -> 0 and 0 -> 359.
      val degrees = normalizeDegrees((model.value - e.getWheelRotation()).toDouble())
      model.value = degrees.toInt()
    }
  }

  companion object {
    private const val DIAL_MARGIN = 40.0 // room for the dots and labels outside the ring
    private const val HANDLE_RADIUS = 8.0
    private const val HANDLE_GAP = 4.0 // gap between the ring and the handle
    private const val DOT_GAP = 6.0 // distance from the ring to the dot centers
    private const val MAJOR_DOT_RADIUS = 3.0
    private const val MINOR_DOT_RADIUS = 1.5
    private const val LABEL_GAP = 4.0 // gap between the major dots and the labels
    private const val CENTER_DOT_RADIUS = 2.0
    private const val TICK_STEP = 30
    private const val LABEL_STEP = 90

    private fun calculateDialRadius(innerArea: Rectangle) =
      min(innerArea.width, innerArea.height) / 2.0 - DIAL_MARGIN

    private fun createCircle(cx: Double, cy: Double, radius: Double) =
      Ellipse2D.Double(cx - radius, cy - radius, 2.0 * radius, 2.0 * radius)

    // Linear interpolation between two colors: ratio == 0 -> first, ratio == 1 -> second.
    private fun interpolateColor(first: Color, second: Color, ratio: Double): Color {
      val red = (first.red + (second.red - first.red) * ratio).roundToInt()
      val green = (first.green + (second.green - first.green) * ratio).roundToInt()
      val blue = (first.blue + (second.blue - first.blue) * ratio).roundToInt()
      return Color(red, green, blue)
    }

    // Distance from the ring to the label center so that the label's inner edge
    // clears the tick dots in the direction of `degrees`.
    private fun calculateLabelOffset(labelShape: Shape, degrees: Double): Double {
      val bounds = labelShape.bounds2D
      val radians = Math.toRadians(degrees)
      val a = abs(cos(radians)) * bounds.height
      val b = abs(sin(radians)) * bounds.width
      val half = max(a, b) / 2.0
      return DOT_GAP + MAJOR_DOT_RADIUS + LABEL_GAP + half
    }

    // Polar (degrees, 0 == straight up, clockwise) to Cartesian, origin at the center.
    private fun polarToCartesian(degrees: Double, distance: Double): Point2D {
      val radians = Math.toRadians(degrees)
      return Point2D.Double(distance * sin(radians), -distance * cos(radians))
    }

    // Cartesian (origin at the center) to degrees in [0, 360).
    private fun cartesianToDegrees(point: Point2D): Double {
      val degrees = Math.toDegrees(atan2(point.x, -point.y))
      return normalizeDegrees(degrees)
    }

    private fun normalizeDegrees(degrees: Double): Double {
      val normalized = degrees % 360.0
      return if (normalized < 0.0) normalized + 360.0 else normalized
    }

    // Translates shape so that its bounding-box center lands on the polar position.
    private fun positionShapeAtAngle(
      shape: Shape,
      degrees: Double,
      distance: Double,
    ): Shape {
      val bounds = shape.bounds2D
      val point = polarToCartesian(degrees, distance)
      val dx = point.x - bounds.centerX
      val dy = point.y - bounds.centerY
      return AffineTransform.getTranslateInstance(dx, dy).createTransformedShape(shape)
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
