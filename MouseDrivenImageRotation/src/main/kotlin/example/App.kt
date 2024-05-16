package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.PI
import kotlin.math.atan2

fun makeUI(): Component {
  val path = "example/test.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val di = DraggableImageMouseListener(ImageIcon(img))
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val w = width.toFloat()
      val h = height.toFloat()
      g2.paint = GradientPaint(50f, 0f, Color.GRAY, w, h, Color.DARK_GRAY, true)
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
  private var radian = 45.0 / 180.0 * PI // Math.toRadians(45.0)
  private var startRadian = 0.0 // drag start radian
  private var moverHover = false
  private var rotatorHover = false

  init {
    val width = ii.iconWidth
    val height = ii.iconHeight
    imageSz = Dimension(width, height)
    border = RoundRectangle2D.Double(
      0.0,
      0.0,
      width.toDouble(),
      height.toDouble(),
      10.0,
      10.0,
    )
    polaroid = Rectangle2D.Double(-2.0, -2.0, width + 4.0, height + 20.0)
    setCirclesLocation(centerPt)
  }

  private fun setCirclesLocation(center: Point2D) {
    val cx = center.x
    val cy = center.y
    inner.setFrameFromCenter(cx, cy, cx + IR / 2.0, cy - IR / 2.0)
    outer.setFrameFromCenter(cx, cy, cx + OR / 2.0, cy - OR / 2.0)
  }

  fun paint(
    g: Graphics,
    observer: ImageObserver?,
  ) {
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
    val pt = e.point
    if (outer.contains(pt) && !inner.contains(pt)) {
      moverHover = false
      rotatorHover = true
    } else if (inner.contains(pt)) {
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
    val pt = e.point
    if (outer.contains(pt) && !inner.contains(pt)) {
      rotatorHover = true
      startRadian = radian - atan2(e.y - centerPt.y, e.x - centerPt.x)
      e.component.repaint()
    } else if (inner.contains(pt)) {
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

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 240

  override fun getIconHeight() = 160
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
