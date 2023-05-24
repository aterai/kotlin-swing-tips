package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Ellipse2D
import java.awt.geom.FlatteningPathIterator
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun makeUI(): Component {
  val circle = Ellipse2D.Double(0.0, 0.0, 100.0, 100.0)
  val ellipse = Ellipse2D.Double(0.0, 0.0, 128.0, 100.0)
  val p = JPanel().also {
    it.add(makeLabel("Ellipse2D", circle))
    it.add(makeLabel("Polygon", convertEllipse2Polygon(ellipse)))
    it.add(makeLabel("Polygon", convertEllipse2Polygon(circle)))
    it.add(makeLabel("FlatteningPathIterator", convertEllipse2Polygon(ellipse)))
  }
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

fun convertEllipse2Polygon(e: Ellipse2D): Polygon {
  val b = e.bounds
  val r1 = b.width / 2.0
  val r2 = b.height / 2.0
  val x0 = b.x + r1
  val y0 = b.y + r2
  val v = 60
  var a = 0.0
  val d = 2 * PI / v
  val polygon = Polygon()
  for (i in 0 until v) {
    polygon.addPoint((r1 * cos(a) + x0).toInt(), (r2 * sin(a) + y0).toInt())
    a += d
  }
  return polygon
}

private fun makeLabel(t: String, s: Shape) = JLabel(t, ShapeIcon(s), SwingConstants.CENTER).also {
  it.horizontalTextPosition = SwingConstants.CENTER
}

private class ShapeIcon(private val shape: Shape) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.draw(shape)
    g2.paint = Color.RED
    val i = FlatteningPathIterator(shape.getPathIterator(null), 1.0)
    val coords = DoubleArray(6)
    while (!i.isDone) {
      i.currentSegment(coords)
      g2.fillRect((coords[0] - .5).toInt(), (coords[1] - .5).toInt(), 2, 2)
      i.next()
    }
    g2.dispose()
  }

  override fun getIconWidth() = shape.bounds.width + 1

  override fun getIconHeight() = shape.bounds.height + 1
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
