package example

import java.awt.*
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.geom.Arc2D
import java.awt.geom.FlatteningPathIterator
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import java.awt.geom.Point2D
import javax.swing.*
import kotlin.math.atan2
import kotlin.math.hypot

fun makeUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as Graphics2D
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val txt = "protected void paint"
      val font = g2.font.deriveFont(32f)
      val gv = font.createGlyphVector(g2.fontRenderContext, txt)
      if (gv.numGlyphs > 0) {
        val path = Arc2D.Double(50.0, 50.0, 200.0, 150.0, 180.0, -180.0, Arc2D.OPEN)
        g2.paint = Color.RED
        g2.draw(path)
        g2.paint = Color.BLACK
        g2.fill(createTextOnPath(path, gv))
      }
      g2.dispose()
    }
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

@Suppress("NestedBlockDepth")
fun createTextOnPath(shape: Shape, gv: GlyphVector): Shape {
  val points = DoubleArray(6)
  val prevPt = Point2D.Double()
  var nextAdvance = 0.0
  var next = 0.0
  val result = Path2D.Double()
  val length = gv.numGlyphs
  var idx = 0
  val pi = FlatteningPathIterator(shape.getPathIterator(null), 1.0)
  while (idx < length && !pi.isDone) {
    when (pi.currentSegment(points)) {
      PathIterator.SEG_MOVETO -> {
        result.moveTo(points[0], points[1])
        prevPt.setLocation(points[0], points[1])
        nextAdvance = gv.getGlyphMetrics(idx).advance * .5
        next = nextAdvance
      }

      PathIterator.SEG_LINETO -> {
        val dx = points[0] - prevPt.x
        val dy = points[1] - prevPt.y
        val distance = hypot(dx, dy)
        if (distance >= next) {
          val r = 1.0 / distance
          val angle = atan2(dy, dx)
          while (idx < length && distance >= next) {
            val x = prevPt.x + next * dx * r
            val y = prevPt.y + next * dy * r
            val advance = nextAdvance
            nextAdvance = getNextAdvance(gv, idx, length)
            val at = AffineTransform.getTranslateInstance(x, y)
            at.rotate(angle)
            val pt = gv.getGlyphPosition(idx)
            at.translate(-pt.x - advance, -pt.y)
            result.append(at.createTransformedShape(gv.getGlyphOutline(idx)), false)
            next += advance + nextAdvance
            idx++
          }
        }
        next -= distance
        prevPt.setLocation(points[0], points[1])
      }
    }
    pi.next()
  }
  return result
}

private fun getNextAdvance(gv: GlyphVector, idx: Int, length: Int) =
  if (idx < length - 1) gv.getGlyphMetrics(idx + 1).advance * .5 else 0.0

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
