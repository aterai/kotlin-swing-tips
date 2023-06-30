package example

import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*

fun makeUI(): Component {
  return MotionPathAnimationPanel().also {
    it.preferredSize = Dimension(320, 240)
  }
}

class MotionPathAnimationPanel : JPanel(BorderLayout()) {
  private var startTime = -1L
  private val shape: Shape
  private val pos = Point2D.Double()
  private val points = mutableListOf<Point2D>()
  private val button = JButton("start")

  init {
    shape = RoundRectangle2D.Double(0.0, 0.0, 100.0, 80.0, 50.0, 30.0)
    makePointList(shape, points)
    pos.setLocation(points[0])
    val timer = Timer(50) { e ->
      startTime = if (startTime >= 0) startTime else System.currentTimeMillis()
      val playTime = System.currentTimeMillis() - startTime
      var progress = playTime / PLAY_TIME
      if (progress > 1.0) {
        progress = 1.0
        (e.source as? Timer)?.stop()
        startTime = -1
        button.isEnabled = true
      }
      val index = (points.size * progress).toInt().coerceIn(0, points.size - 1)
      pos.setLocation(points[index])
      repaint()
    }
    button.addActionListener {
      if (!timer.isRunning) {
        timer.start()
        button.isEnabled = false
      }
    }
    add(button, BorderLayout.SOUTH)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE)
    val x = (width - shape.bounds.width) / 2
    val y = (height - shape.bounds.height) / 2
    g2.translate(x, y)
    g2.draw(shape)
    var i = 0
    while (i < points.size) {
      val p = points[i]
      g2.draw(Ellipse2D.Double(p.x - 2.0, p.y - 2.0, 4.0, 4.0))
      i += 10
    }
    g2.color = Color.RED
    g2.draw(Ellipse2D.Double(pos.x - 4, pos.y - 4, 8.0, 8.0))
    g2.dispose()
  }

  private fun makePointList(shape: Shape, points: MutableList<Point2D>) {
    val pi = shape.getPathIterator(null, 0.01)
    val prev = Point2D.Double()
    val threshold = 2.0
    while (!pi.isDone) {
      val coords = DoubleArray(6)
      val segment = pi.currentSegment(coords)
      val pt = Point2D.Double(coords[0], coords[1])
      if (segment == PathIterator.SEG_MOVETO) {
        points.add(pt)
        prev.setLocation(pt)
      } else if (segment == PathIterator.SEG_LINETO) {
        val distance = prev.distance(pt)
        if (distance > threshold) {
          ticking(points, prev, pt)
        } else {
          points.add(pt)
        }
        prev.setLocation(pt)
      }
      pi.next()
    }
  }

  private fun ticking(
    points: MutableList<Point2D>,
    prev: Point2D,
    pt: Point2D,
    delta: Double = 0.02
  ) {
    var fraction = delta
    val distance = prev.distance(pt)
    var p = interpolate(prev, pt, fraction)
    while (distance > prev.distance(p)) {
      points.add(p)
      fraction += delta
      p = interpolate(prev, pt, fraction)
    }
  }

  private fun interpolate(start: Point2D, end: Point2D, fraction: Double): Point2D {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val nx = start.x + dx * fraction
    val ny = start.y + dy * fraction
    return Point2D.Double(nx, ny)
  }

  companion object {
    private const val PLAY_TIME = 5000.0
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
