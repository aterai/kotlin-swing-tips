package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.ImageObserver
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.atan2

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val di = DraggableImageMouseListener(ImageIcon(cl.getResource("example/test.png")))
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = GradientPaint(50f, 0f, Color.GRAY, width.toFloat(), height.toFloat(), Color.DARK_GRAY, true)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      di.paint(g, this)
    }
  }
  p.addMouseListener(di)
  p.addMouseMotionListener(di)
  p.preferredSize = Dimension(320, 240)
  return p
}

private class DraggableImageMouseListener(ii: ImageIcon) : MouseAdapter() {
  private val border: Shape
  private val polaroid: Shape
  private val inner = Ellipse2D.Double(0.0, 0.0, IR, IR)
  private val outer = Ellipse2D.Double(0.0, 0.0, OR, OR)
  private val startPt = Point2D.Double() // drag start point
  private val centerPt = Point2D.Double(100.0, 100.0) // center of Image
  private val imageSz: Dimension
  private val image = ii.image
  private var radian = 45.0 * (Math.PI / 180.0)
  private var startRadian = 0.0 // drag start radian
  private var moverHover = false
  private var rotatorHover = false

  init {
    val width = ii.iconWidth
    val height = ii.iconHeight
    imageSz = Dimension(width, height)
    border = RoundRectangle2D.Double(0.0, 0.0, width.toDouble(), height.toDouble(), 10.0, 10.0)
    polaroid = Rectangle2D.Double(-2.0, -2.0, width + 4.0, height + 20.0)
    setCirclesLocation(centerPt)
  }

  private fun setCirclesLocation(center: Point2D) {
    val cx = center.x
    val cy = center.y
    inner.setFrameFromCenter(cx, cy, cx + IR / 2.0, cy - IR / 2.0)
    outer.setFrameFromCenter(cx, cy, cx + OR / 2.0, cy - OR / 2.0)
  }

  fun paint(g: Graphics, observer: ImageObserver?) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val w2 = imageSz.width / 2.0
    val h2 = imageSz.height / 2.0
    val at = AffineTransform.getTranslateInstance(centerPt.x - w2, centerPt.y - h2)
    at.rotate(radian, w2, h2)
    g2.paint = BORDER_COLOR
    g2.stroke = BORDER_STROKE
    val s = at.createTransformedShape(polaroid)
    g2.fill(s)
    g2.draw(s)
    g2.drawImage(image, at, observer)
    if (rotatorHover) {
      val donut = Area(outer)
      donut.subtract(Area(inner))
      g2.paint = HOVER_COLOR
      g2.fill(donut)
    } else if (moverHover) {
      g2.paint = HOVER_COLOR
      g2.fill(inner)
    }
    g2.paint = BORDER_COLOR
    g2.stroke = BORDER_STROKE
    g2.draw(at.createTransformedShape(border))
    g2.dispose()
  }

  override fun mouseMoved(e: MouseEvent) {
    val dx = e.x.toDouble()
    val dy = e.y.toDouble()
    if (outer.contains(dx, dy) && !inner.contains(dx, dy)) {
      moverHover = false
      rotatorHover = true
    } else if (inner.contains(dx, dy)) {
      moverHover = true
      rotatorHover = false
    } else {
      moverHover = false
      rotatorHover = false
    }
    e.component.repaint()
  }

  override fun mouseReleased(e: MouseEvent) {
    rotatorHover = false
    moverHover = false
    e.component.repaint()
  }

  override fun mousePressed(e: MouseEvent) {
    val dx = e.x.toDouble()
    val dy = e.y.toDouble()
    if (outer.contains(dx, dy) && !inner.contains(dx, dy)) {
      rotatorHover = true
      startRadian = radian - atan2(e.y - centerPt.y, e.x - centerPt.x)
      e.component.repaint()
    } else if (inner.contains(dx, dy)) {
      moverHover = true
      startPt.setLocation(e.point)
      e.component.repaint()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (rotatorHover) {
      radian = startRadian + atan2(e.y - centerPt.y, e.x - centerPt.x)
      e.component.repaint()
    } else if (moverHover) {
      centerPt.setLocation(centerPt.x + e.x - startPt.x, centerPt.y + e.y - startPt.y)
      setCirclesLocation(centerPt)
      startPt.setLocation(e.point)
      e.component.repaint()
    }
  }

  companion object {
    private val BORDER_STROKE = BasicStroke(4f)
    private val BORDER_COLOR = Color.WHITE
    private val HOVER_COLOR = Color(0x64_64_FF_C8, true)
    private const val IR = 40.0
    private const val OR = IR * 3.0
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
