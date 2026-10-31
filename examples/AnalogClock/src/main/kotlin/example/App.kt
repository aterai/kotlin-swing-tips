package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*
import kotlin.math.max
import kotlin.math.min

fun createUI() = JPanel(BorderLayout()).also {
  it.add(AnalogClock())
  it.preferredSize = Dimension(320, 240)
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

private class AnalogClock : JPanel() {
  private var secondOfDay = -1
  private var secondAngle = 0.0
  private var minuteAngle = 0.0
  private var hourAngle = 0.0
  private val timer = Timer(200) { updateTime() }
  private var listener: HierarchyListener? = null

  init {
    // Update the hands immediately on start instead of waiting for the first delay
    timer.initialDelay = 0
  }

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        if (e.component.isShowing) {
          timer.start()
        } else {
          timer.stop()
        }
      }
    }
    addHierarchyListener(listener)
  }

  private fun updateTime() {
    val sec = LocalTime.now(ZoneId.systemDefault()).toSecondOfDay()
    // The hands only move once per second, so skip repainting
    // if the time has not changed since the previous Timer tick
    if (sec != secondOfDay) {
      secondOfDay = sec
      // Calculate the angle of rotation
      secondAngle = sec % 60 * RAD_PER_MIN
      minuteAngle = sec / 60 % 60 * RAD_PER_MIN + secondAngle / 60.0
      hourAngle = sec / 3600 % 12 * Math.PI / 6.0 + minuteAngle / 12.0
      repaint()
    }
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    initRenderingHints(g2)
    val rect = SwingUtilities.calculateInnerArea(this, null)
    paintBackground(g2, rect)
    val radius = min(rect.width, rect.height) / 2.0 - 10.0
    // Skip drawing the dial if the panel is too small to contain it
    if (radius >= MIN_RADIUS) {
      g2.translate(rect.centerX, rect.centerY)
      paintHourMarkers(g2, radius)
      paintHourHand(g2, radius, hourAngle)
      paintMinuteHand(g2, radius, minuteAngle)
      paintSecondHand(g2, radius, secondAngle)
    }

    g2.dispose()
  }

  private fun initRenderingHints(g2: Graphics2D) {
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
  }

  private fun paintBackground(g2: Graphics2D, rect: Rectangle) {
    g2.color = Color.DARK_GRAY
    g2.fill(rect)
  }

  private fun paintHourMarkers(g2: Graphics2D, radius: Double) {
    // Clamp to zero so that the markers do not stick out of the dial
    val hourMarkerLen = max(0.0, radius / 6.0 - 10.0)
    val hourMarker = Line2D.Double(0.0, hourMarkerLen - radius, 0.0, -radius)
    val minuteMarker = Line2D.Double(0.0, hourMarkerLen / 2.0 - radius, 0.0, -radius)
    val at = AffineTransform.getRotateInstance(0.0)
    g2.stroke = BasicStroke(2f)
    g2.color = Color.WHITE
    for (i in 0..<MINUTES_PER_HOUR) {
      if (i % MIN_PER_MARKER == 0) {
        g2.draw(at.createTransformedShape(hourMarker))
      } else {
        g2.draw(at.createTransformedShape(minuteMarker))
      }
      at.rotate(RAD_PER_MIN)
    }
  }

  private fun paintHourHand(g2: Graphics2D, radius: Double, hourAngle: Double) {
    val hourHandLen = radius / 2.0
    val hourHand = Line2D.Double(0.0, 0.0, 0.0, -hourHandLen)
    paintHand(g2, hourHand, 8f, Color.LIGHT_GRAY, hourAngle)
  }

  private fun paintMinuteHand(g2: Graphics2D, radius: Double, minuteAngle: Double) {
    val minuteHandLen = 5.0 * radius / 6.0
    val minuteHand = Line2D.Double(0.0, 0.0, 0.0, -minuteHandLen)
    paintHand(g2, minuteHand, 4f, Color.WHITE, minuteAngle)
  }

  private fun paintSecondHand(g2: Graphics2D, radius: Double, secondAngle: Double) {
    val r = radius / 6.0
    val secondHandLen = radius - r
    val secondHand = Line2D.Double(0.0, r, 0.0, -secondHandLen)
    paintHand(g2, secondHand, 1f, Color.RED, secondAngle)
    g2.fill(Ellipse2D.Double(-r / 4.0, -r / 4.0, r / 2.0, r / 2.0))
  }

  private fun paintHand(
    g2: Graphics2D,
    hand: Shape,
    strokeWidth: Float,
    color: Color,
    angle: Double,
  ) {
    g2.stroke = BasicStroke(strokeWidth)
    g2.paint = color
    g2.draw(AffineTransform.getRotateInstance(angle).createTransformedShape(hand))
  }

  companion object {
    private const val MINUTES_PER_HOUR = 60
    private const val MIN_PER_MARKER = 5
    private const val RAD_PER_MIN = 2.0 * Math.PI / MINUTES_PER_HOUR
    private const val MIN_RADIUS = 1.0
  }
}
