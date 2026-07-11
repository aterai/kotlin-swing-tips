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
  private var secondRot = 0.0
  private var minuteRot = 0.0
  private var hourRot = 0.0
  private val timer = Timer(200) {
    val time = LocalTime.now(ZoneId.systemDefault())
    secondRot = time.second * Math.PI / 30.0
    minuteRot = time.minute * Math.PI / 30.0 + secondRot / 60.0
    hourRot = time.hour * Math.PI / 6.0 + minuteRot / 12.0
    repaint()
  }
  private var listener: HierarchyListener? = null

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

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    initRenderingHints(g2)
    val rect = SwingUtilities.calculateInnerArea(this, null)
    paintBackground(g2, rect)
    val radius = min(rect.width, rect.height) / 2.0 - 10.0
    g2.translate(rect.centerX, rect.centerY)

    paintHourMarkers(g2, radius)
    paintHourHand(g2, radius, hourRot)
    paintMinuteHand(g2, radius, minuteRot)
    paintSecondHand(g2, radius, secondRot)

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
    val hourMarkerLen = radius / 6.0 - 10.0
    val hourMarker = Line2D.Double(0.0, hourMarkerLen - radius, 0.0, -radius)
    val minuteMarker = Line2D.Double(0.0, hourMarkerLen / 2.0 - radius, 0.0, -radius)
    val at = AffineTransform.getRotateInstance(0.0)
    g2.stroke = BasicStroke(2f)
    g2.color = Color.WHITE
    for (i in 0..59) {
      if (i % 5 == 0) {
        g2.draw(at.createTransformedShape(hourMarker))
      } else {
        g2.draw(at.createTransformedShape(minuteMarker))
      }
      at.rotate(Math.PI / 30.0)
    }
  }

  private fun paintHourHand(g2: Graphics2D, radius: Double, hourRot: Double) {
    val hourHandLen = radius / 2.0
    val hourHand = Line2D.Double(0.0, 0.0, 0.0, -hourHandLen)
    paintHand(g2, hourHand, 8f, Color.LIGHT_GRAY, hourRot)
  }

  private fun paintMinuteHand(g2: Graphics2D, radius: Double, minuteRot: Double) {
    val minuteHandLen = 5.0 * radius / 6.0
    val minuteHand = Line2D.Double(0.0, 0.0, 0.0, -minuteHandLen)
    paintHand(g2, minuteHand, 4f, Color.WHITE, minuteRot)
  }

  private fun paintSecondHand(g2: Graphics2D, radius: Double, secondRot: Double) {
    val r = radius / 6.0
    val secondHandLen = radius - r
    val secondHand = Line2D.Double(0.0, r, 0.0, -secondHandLen)
    paintHand(g2, secondHand, 1f, Color.RED, secondRot)
    g2.fill(Ellipse2D.Double(-r / 4.0, -r / 4.0, r / 2.0, r / 2.0))
  }

  private fun paintHand(
    g2: Graphics2D,
    hand: Shape,
    strokeWidth: Float,
    color: Color,
    rot: Double,
  ) {
    g2.stroke = BasicStroke(strokeWidth)
    g2.paint = color
    g2.draw(AffineTransform.getRotateInstance(rot).createTransformedShape(hand))
  }
}
