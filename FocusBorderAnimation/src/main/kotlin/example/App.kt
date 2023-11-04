package example

import java.awt.*
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import javax.swing.*
import javax.swing.border.EmptyBorder

fun makeUI(): Component {
  val field1 = makeTextField("Animated Border 1")
  field1.border = AnimatedBorder(field1)

  val field2 = makeTextField("Animated Border 2")
  field2.border = AnimatedBorder(field2)

  val field3 = makeTextField("Animated Border 3")
  field3.isEnabled = false
  field3.border = AnimatedBorder(field3)

  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.add(makeTextField("Default Border"))
    it.add(Box.createVerticalStrut(30))
    it.add(field1)
    it.add(Box.createVerticalStrut(10))
    it.add(field2)
    it.add(Box.createVerticalStrut(10))
    it.add(field3)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTextField(text: String) = JTextField(text, 32).also {
  it.isOpaque = false
}

private class AnimatedBorder(
  c: JComponent,
) : EmptyBorder(BORDER, BORDER, BORDER + BOTTOM_SPACE, BORDER) {
  private val animator = Timer(10, null)
  private val stroke = BasicStroke(BORDER.toFloat())
  private val bottomStroke = BasicStroke(BORDER / 2f)
  private var startTime = -1L
  private val pos = Point2D.Double()
  private val points = mutableListOf<Point2D>()
  private val borderPath = Path2D.Double()

  init {
    animator.addActionListener { e ->
      if (startTime < 0) {
        startTime = System.currentTimeMillis()
      }
      val playTime = System.currentTimeMillis() - startTime
      val progress = playTime / PLAY_TIME
      val stop = progress > 1.0 || points.isEmpty()
      if (stop) {
        startTime = -1L
        (e.source as? Timer)?.stop()
      } else {
        pos.setLocation(points[0])
        borderPath.reset()
        borderPath.moveTo(pos.x, pos.y)
        val idx = (points.size * progress).toInt().coerceIn(0, points.size - 1)
        for (i in 0..idx) {
          pos.setLocation(points[i])
          borderPath.lineTo(pos.x, pos.y)
          borderPath.moveTo(pos.x, pos.y)
        }
        borderPath.closePath()
      }
      c.repaint()
    }
    c.addFocusListener(object : FocusListener {
      override fun focusGained(e: FocusEvent) {
        val r = c.bounds
        r.height -= BOTTOM_SPACE + 1
        val p = Path2D.Double()
        p.moveTo(r.getWidth(), r.getHeight())
        p.lineTo(r.getWidth(), 0.0)
        p.lineTo(0.0, 0.0)
        p.lineTo(0.0, r.getHeight())
        p.closePath()
        makePointList(p, points)
        animator.start()
      }

      override fun focusLost(e: FocusEvent) {
        points.clear()
        borderPath.reset()
        c.repaint()
      }
    })
  }

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
  ) {
    super.paintBorder(c, g, x, y, w, h)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = if (c.isEnabled) Color.ORANGE else Color.GRAY
    g2.translate(x, y)
    g2.stroke = bottomStroke
    g2.drawLine(0, h - BOTTOM_SPACE, w - 1, h - BOTTOM_SPACE)
    g2.stroke = stroke
    g2.draw(borderPath)
    g2.dispose()
  }

  private fun makePointList(
    shape: Shape,
    points: MutableList<Point2D>,
  ) {
    points.clear()
    val pi = shape.getPathIterator(null, 0.01)
    val prev = Point2D.Double()
    val threshold = 2.0
    while (!pi.isDone) {
      val ary = DoubleArray(6)
      val segment = pi.currentSegment(ary)
      val pt = Point2D.Double(ary[0], ary[1])
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
    delta: Double = 0.02,
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

  private fun interpolate(
    start: Point2D,
    end: Point2D,
    fraction: Double,
  ): Point2D {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val nx = start.x + dx * fraction
    val ny = start.y + dy * fraction
    return Point2D.Double(nx, ny)
  }

  companion object {
    private const val PLAY_TIME = 300.0
    private const val BOTTOM_SPACE = 20
    private const val BORDER = 4
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
