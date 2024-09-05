package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder

fun makeUI(): Component {
  val arc8 = 8
  val scroll = object : JScrollPane(JTable(8, 5)) {
    override fun paintComponent(g: Graphics) {
      val b = border
      val g2 = g.create()
      if (!isOpaque && b is PlaqueBorder && g2 is Graphics2D) {
        g2.paint = background
        val w = width - 1
        val h = height - 1
        g2.fill(b.getBorderShape(0.0, 0.0, w.toDouble(), h.toDouble(), arc8.toDouble()))
      }
      g2.dispose()
      super.paintComponent(g)
    }

    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      background = Color.WHITE
      getViewport().background = background
      val b = object : PlaqueBorder(arc8) {
        override fun getBorderShape(
          x: Double,
          y: Double,
          w: Double,
          h: Double,
          r: Double,
        ): Shape {
          val rr = r * .5522
          val path = Path2D.Double()
          path.moveTo(x, y + r)
          path.curveTo(x + rr, y + r, x + r, y + rr, x + r, y)
          path.lineTo(x + w - r, y)
          path.curveTo(x + w - r, y + rr, x + w - rr, y + r, x + w, y + r)
          path.lineTo(x + w, y + h - r)
          path.curveTo(x + w - rr, y + h - r, x + w - r, y + h - rr, x + w - r, y + h)
          path.lineTo(x + r, y + h)
          path.curveTo(x + r, y + h - rr, x + rr, y + h - r, x, y + h - r)
          // path.lineTo(x, y + r)
          path.closePath()
          return path
        }
      }
      setBorder(b)
    }
  }

  val arc4 = 4
  val field = object : JTextField("JTextField") {
    override fun paintComponent(g: Graphics) {
      val b = border
      val g2 = g.create()
      if (!isOpaque && b is PlaqueBorder && g2 is Graphics2D) {
        g2.paint = background
        val w = width - 1
        val h = height - 1
        g2.fill(b.getBorderShape(0.0, 0.0, w.toDouble(), h.toDouble(), arc4.toDouble()))
      }
      g2.dispose()
      super.paintComponent(g)
    }

    override fun updateUI() {
      super.updateUI()
      isOpaque = false
      border = PlaqueBorder(arc4)
    }
  }

  return JPanel(BorderLayout(5, 5)).also {
    it.add(scroll)
    it.add(field, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class PlaqueBorder(
  arc: Int,
) : EmptyBorder(arc, arc, arc, arc) {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = Color.GRAY
    val arc = getBorderInsets(c).top.toDouble()
    g2.draw(getBorderShape(x.toDouble(), y.toDouble(), width - 1.0, height - 1.0, arc))
    g2.dispose()
  }

  open fun getBorderShape(
    x: Double,
    y: Double,
    w: Double,
    h: Double,
    r: Double,
  ): Shape {
    val rect = Area(Rectangle2D.Double(x, y, w, h))
    rect.subtract(Area(Ellipse2D.Double(x - r, y - r, r + r, r + r)))
    rect.subtract(Area(Ellipse2D.Double(x + w - r, y - r, r + r, r + r)))
    rect.subtract(Area(Ellipse2D.Double(x - r, y + h - r, r + r, r + r)))
    rect.subtract(Area(Ellipse2D.Double(x + w - r, y + h - r, r + r, r + r)))
    return rect
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
