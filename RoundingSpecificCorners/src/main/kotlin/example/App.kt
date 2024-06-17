package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.util.EnumSet
import javax.swing.*
import kotlin.math.sqrt

fun makeUI(): Component {
  val corners = EnumSet.allOf(Corner::class.java)
  val types = EnumSet.allOf(Type::class.java)
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

      val x = width * .1
      val y = height * .1
      val w = width * .8
      val h = height * .8
      val aw = w * .4
      val ah = h * .4
      val rect: Rectangle2D = Rectangle2D.Double(x, y, w, h)

      if (types.contains(Type.ROUND_RECTANGLE2D)) {
        g2.color = Color.BLACK
        g2.draw(RoundRectangle2D.Double(x, y, w, h, aw, ah))
      }

      if (types.contains(Type.SUBTRACT)) {
        g2.color = Color.RED
        val shape0 = makeRoundedRect0(rect, aw, ah, corners)
        g2.draw(shape0)
      }

      if (types.contains(Type.PATH2D1)) {
        g2.color = Color.GREEN
        val shape1: Shape = makeRoundedRect1(rect, aw, ah, corners)
        g2.draw(shape1)
      }

      if (types.contains(Type.PATH2D2)) {
        g2.color = Color.BLUE
        val shape2 = makeRoundedRect2(rect, aw, ah, corners)
        g2.draw(shape2)
      }

      g2.dispose()
    }
  }
  p.componentPopupMenu = makePopupMenu(p, corners, types)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePopupMenu(
  p: JPanel,
  corners: EnumSet<Corner>,
  types: EnumSet<Type>,
): JPopupMenu {
  val popup = JPopupMenu()
  Corner.entries.forEach {
    val check = JCheckBoxMenuItem(it.name, true)
    check.addActionListener { e ->
      if ((e.source as? AbstractButton)?.isSelected == true) {
        corners.add(it)
      } else {
        corners.remove(it)
      }
      p.repaint()
    }
    popup.add(check)
  }
  popup.addSeparator()
  Type.entries.forEach {
    val check = JCheckBoxMenuItem(it.name, true)
    check.addActionListener { e ->
      if ((e.source as? AbstractButton)?.isSelected == true) {
        types.add(it)
      } else {
        types.remove(it)
      }
      p.repaint()
    }
    popup.add(check)
  }
  return popup
}

fun makeRoundedRect0(rect: Rectangle2D, aw: Double, ah: Double, corners: Set<Corner>): Shape {
  val x = rect.x
  val y = rect.y
  val w = rect.width
  val h = rect.height
  val arw = aw * .5
  val arh = ah * .5
  val r = Area(rect)
  if (corners.contains(Corner.TOP_LEFT)) {
    val tl = Area(Rectangle2D.Double(x, y, arw, arh))
    tl.subtract(Area(Ellipse2D.Double(x, y, aw, ah)))
    r.subtract(tl)
  }
  if (corners.contains(Corner.TOP_RIGHT)) {
    val tr = Area(Rectangle2D.Double(x + w - arw, y, arw, arh))
    tr.subtract(Area(Ellipse2D.Double(x + w - aw, y, aw, ah)))
    r.subtract(tr)
  }
  if (corners.contains(Corner.BOTTOM_LEFT)) {
    val bl = Area(Rectangle2D.Double(x, y + h - arh, arw, arh))
    bl.subtract(Area(Ellipse2D.Double(x, y + h - ah, aw, ah)))
    r.subtract(bl)
  }
  if (corners.contains(Corner.BOTTOM_RIGHT)) {
    val br = Area(Rectangle2D.Double(x + w - arw, y + h - arh, arw, arh))
    br.subtract(Area(Ellipse2D.Double(x + w - aw, y + h - ah, aw, ah)))
    r.subtract(br)
  }
  // r.transform(AffineTransform.getTranslateInstance(x, y));
  return r
}

fun makeRoundedRect2(r: Rectangle2D, aw: Double, ah: Double, corners: Set<Corner>): Shape {
  val x = r.x
  val y = r.y
  val w = r.width
  val h = r.height
  val arh = ah * .5
  val arw = aw * .5
  val kappa = 4.0 * (sqrt(2.0) - 1.0) / 3.0
  val akw = arw * kappa
  val akh = arh * kappa
  val p = Path2D.Double()
  if (corners.contains(Corner.TOP_LEFT)) {
    p.moveTo(x, y + arh)
    p.curveTo(x, y + arh - akh, x + arw - akw, y, x + arw, y)
  } else {
    p.moveTo(x, y)
  }
  if (corners.contains(Corner.TOP_RIGHT)) {
    p.lineTo(x + w - arw, y)
    p.curveTo(x + w - arw + akw, y, x + w, y + arh - akh, x + w, y + arh)
  } else {
    p.lineTo(x + w, y)
  }
  if (corners.contains(Corner.BOTTOM_RIGHT)) {
    p.lineTo(x + w, y + h - arh)
    p.curveTo(x + w, y + h - arh + akh, x + w - arw + akw, y + h, x + w - arw, y + h)
  } else {
    p.lineTo(x + w, y + h)
  }
  if (corners.contains(Corner.BOTTOM_LEFT)) {
    p.lineTo(x + arw, y + h)
    p.curveTo(x + arw - akw, y + h, x, y + h - arh + akh, x, y + h - arh)
  } else {
    p.lineTo(x, y + h)
  }
  p.closePath()
  return p
}

fun makeRoundedRect1(r: Rectangle2D, aw: Double, ah: Double, corners: Set<Corner>): Path2D {
  val x = r.x
  val y = r.y
  val w = r.width
  val h = r.height
  val arw = aw * .5
  val arh = ah * .5
  val path = Path2D.Double()
  if (corners.contains(Corner.TOP_LEFT)) {
    path.moveTo(x, y + arh)
    path.curveTo(x, y + arh, x, y, x + arw, y)
  } else {
    path.moveTo(x, y)
  }
  if (corners.contains(Corner.TOP_RIGHT)) {
    path.lineTo(x + w - arw, y)
    path.curveTo(x + w - arw, y, x + w, y, x + w, y + arh)
  } else {
    path.lineTo(x + w, y)
  }
  if (corners.contains(Corner.BOTTOM_RIGHT)) {
    path.lineTo(x + w, y + h - arh)
    path.curveTo(x + w, y + h - arh, x + w, y + h, x + w - arw, y + h)
  } else {
    path.lineTo(x + w, y + h)
  }
  if (corners.contains(Corner.BOTTOM_LEFT)) {
    path.lineTo(x + arw, y + h)
    path.curveTo(x + arw, y + h, x, y + h, x, y + h - arh)
  } else {
    path.lineTo(x, y + h)
  }
  path.closePath()
  return path
}

enum class Corner {
  TOP_LEFT,
  TOP_RIGHT,
  BOTTOM_LEFT,
  BOTTOM_RIGHT,
}

enum class Type {
  ROUND_RECTANGLE2D,
  SUBTRACT,
  PATH2D1,
  PATH2D2,
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
