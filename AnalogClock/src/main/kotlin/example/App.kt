package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      // defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

private class AnalogClock : JPanel() {
  private var listener: HierarchyListener? = null
  private var time = LocalTime.now(ZoneId.systemDefault())
  private var timer = Timer(200) {
    time = LocalTime.now(ZoneId.systemDefault())
    repaint()
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

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val rect = SwingUtilities.calculateInnerArea(this, null)
    g2.color = Color.BLACK
    g2.fill(rect)
    val radius = rect.width.coerceAtMost(rect.height) / 2f - 10f
    g2.translate(rect.centerX, rect.centerY)

    // Drawing the hour markers
    val hourMarkerLen = radius / 6f - 10f
    val hourMarker = Line2D.Float(0f, hourMarkerLen - radius, 0f, -radius)
    val minuteMarker = Line2D.Float(0f, hourMarkerLen / 2f - radius, 0f, -radius)
    val at = AffineTransform.getRotateInstance(0.0)
    g2.stroke = BasicStroke(2f)
    g2.color = Color.WHITE
    for (i in 0 until 60) {
      if (i % 5 == 0) {
        g2.draw(at.createTransformedShape(hourMarker))
      } else {
        g2.draw(at.createTransformedShape(minuteMarker))
      }
      at.rotate(Math.PI / 30.0)
    }

    // Drawing the hour hand
    val hourHandLen = 1.5f * radius / 3f
    val hourHand = Line2D.Float(0f, 0f, 0f, -hourHandLen)
    val minuteRot = time.minute * Math.PI / 30.0
    val hourRot = time.hour * Math.PI / 6.0 + minuteRot / 12.0
    g2.stroke = BasicStroke(8f)
    g2.paint = Color.LIGHT_GRAY
    g2.draw(AffineTransform.getRotateInstance(hourRot).createTransformedShape(hourHand))

    // Drawing the minute hand
    val minuteHandLen = 5f * radius / 6f
    val minuteHand = Line2D.Float(0f, 0f, 0f, -minuteHandLen)
    g2.stroke = BasicStroke(4f)
    g2.paint = Color.WHITE
    g2.draw(AffineTransform.getRotateInstance(minuteRot).createTransformedShape(minuteHand))

    // Drawing the second hand
    val r = radius / 6f
    val secondHandLen = radius - r
    val secondHand = Line2D.Float(0f, r, 0f, -secondHandLen)
    val secondRot = time.second * Math.PI / 30.0
    g2.paint = Color.RED
    g2.stroke = BasicStroke(1f)
    g2.draw(AffineTransform.getRotateInstance(secondRot).createTransformedShape(secondHand))
    g2.fill(Ellipse2D.Float(-r / 4f, -r / 4f, r / 2f, r / 2f))
    g2.dispose()
  }
}
