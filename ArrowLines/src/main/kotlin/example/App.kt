package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.*

fun makeUI(): Component {
  val arrows = listOf(
    Arrow(Point(50, 50), Point(100, 150)),
    Arrow(Point(250, 50), Point(150, 50)),
  )
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.stroke = BasicStroke(4f)
      g2.color = Color.BLACK
      for (a in arrows) {
        a.draw(g2)
      }
      g2.dispose()
    }
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class Arrow(start: Point, end: Point) {
  private val start = Point()
  private val end = Point()
  private val arrowHead: Path2D

  init {
    this.start.location = start
    this.end.location = end
    arrowHead = makeArrowHead(Dimension(8, 8))
  }

  fun makeArrowHead(size: Dimension): Path2D {
    val path: Path2D = Path2D.Double()
    val t = size.height.toDouble()
    val w = size.width * .5
    path.moveTo(0.0, -w)
    path.lineTo(t, 0.0)
    path.lineTo(0.0, w)
    path.closePath()
    return path
  }

  fun draw(g2: Graphics2D) {
    g2.drawLine(start.x, start.y, end.x, end.y)
    val at = AffineTransform.getTranslateInstance(end.getX(), end.getY())
    at.rotate(end.getX() - start.getX(), end.getY() - start.getY())
    arrowHead.transform(at)
    g2.fill(arrowHead)
    g2.draw(arrowHead)
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
