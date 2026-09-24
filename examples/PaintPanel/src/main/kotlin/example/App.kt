package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import kotlin.math.ceil

fun createUI() = JPanel(BorderLayout()).also {
  it.add(PaintPanel())
  it.preferredSize = Dimension(320, 240)
}

private class PaintPanel : JPanel() {
  private var handler: MouseInputListener? = null
  private val list = mutableListOf<Shape>()

  override fun updateUI() {
    removeMouseMotionListener(handler)
    removeMouseListener(handler)
    super.updateUI()
    handler = MouseHandler()
    addMouseMotionListener(handler)
    addMouseListener(handler)
  }

  // Repaints only the area covered by the stroke of the segment from p0 to p1
  private fun repaintSegment(p0: Point, p1: Point) {
    val r = Rectangle(p0)
    r.add(p1)
    r.grow(PADDING, PADDING)
    repaint(r)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.BLACK
    g2.stroke = STROKE
    list.forEach { g2.draw(it) }
    g2.dispose()
  }

  private inner class MouseHandler : MouseInputAdapter() {
    private val prevPoint = Point()
    private var path: Path2D? = null

    override fun mousePressed(e: MouseEvent) {
      val pt = e.point
      path = Path2D.Double().also {
        it.moveTo(pt.getX(), pt.getY())
        // A zero-length segment is needed to draw a dot with a round cap
        it.lineTo(pt.getX(), pt.getY())
        list.add(it)
      }
      prevPoint.location = pt
      repaintSegment(pt, pt)
    }

    override fun mouseDragged(e: MouseEvent) {
      path?.also {
        val pt = e.point
        it.lineTo(pt.getX(), pt.getY())
        repaintSegment(prevPoint, pt)
        prevPoint.location = pt
      }
    }
  }

  companion object {
    private const val STROKE_WIDTH = 3f

    // Half of the stroke width plus a margin for rounding
    private val PADDING = ceil(STROKE_WIDTH / 2f).toInt() + 1
    private val STROKE = BasicStroke(
      STROKE_WIDTH,
      BasicStroke.CAP_ROUND,
      BasicStroke.JOIN_ROUND,
    )
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
