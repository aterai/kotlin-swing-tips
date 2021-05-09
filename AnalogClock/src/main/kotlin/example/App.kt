package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports

var time: LocalTime = LocalTime.now(ZoneId.systemDefault())

fun makeUI(): Component {
  val clock = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      val rect = SwingUtilities.calculateInnerArea(this, null)
      g2.color = Color.BLACK
      g2.fill(rect)
      g2.color = Color.WHITE
      g2.translate(rect.centerX, rect.centerY)
      val radius = rect.width.coerceAtMost(rect.height) / 2f - 10f

      // Drawing the hour markers
      val hourMarkerLen = radius / 6f - 10f
      val hourMarker = Line2D.Float(0f, hourMarkerLen - radius, 0f, -radius)
      val at = AffineTransform.getRotateInstance(0.0)
      repeat(12) {
        g2.draw(at.createTransformedShape(hourMarker))
        at.rotate(Math.PI / 6.0)
      }

      // Drawing the minute hand
      val minuteHandLen = 5f * radius / 6f
      val minuteHand = Line2D.Float(0f, 0f, 0f, -minuteHandLen)
      val at2 = AffineTransform.getRotateInstance(time.minute * Math.PI / 30.0)
      g2.stroke = BasicStroke(4f)
      g2.paint = Color.WHITE
      g2.draw(at2.createTransformedShape(minuteHand))

      // Drawing the hour hand
      val hourHandLen = radius / 3f
      val hourHand = Line2D.Float(0f, 0f, 0f, -hourHandLen)
      val at3 = AffineTransform.getRotateInstance(time.hour * Math.PI / 6.0)
      g2.stroke = BasicStroke(8f)
      g2.draw(at3.createTransformedShape(hourHand))

      // Drawing the second hand
      val r = radius / 6f
      val secondHandLen = radius - r
      val secondHand = Line2D.Float(0f, r, 0f, -secondHandLen)
      val at1 = AffineTransform.getRotateInstance(time.second * Math.PI / 30.0)
      g2.paint = Color.RED
      g2.stroke = BasicStroke(1f)
      g2.draw(at1.createTransformedShape(secondHand))
      g2.fill(Ellipse2D.Float(-r / 4f, -r / 4f, r / 2f, r / 2f))
      g2.dispose()
    }
  }
  Timer(200) {
    time = LocalTime.now(ZoneId.systemDefault())
    clock.repaint()
  }.start()
  return JPanel(BorderLayout()).also {
    it.add(clock)
    it.preferredSize = Dimension(320, 240)
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
