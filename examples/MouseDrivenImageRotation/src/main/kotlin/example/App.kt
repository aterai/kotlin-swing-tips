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
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.atan2

fun createUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val image = url?.let(::readImage) ?: makeMissingImage()
  val listener = DraggableImageMouseListener(image)
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      val w = width.toFloat()
      val h = height.toFloat()
      g2.paint = GradientPaint(50f, 0f, Color.GRAY, w, h, Color.DARK_GRAY, true)
      g2.fillRect(0, 0, width, height)
      g2.dispose()
      listener.paint(g)
    }
  }
  p.addMouseListener(listener)
  p.addMouseMotionListener(listener)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun readImage(url: URL) = runCatching {
  url.openStream().use(ImageIO::read)
}.getOrNull() ?: makeMissingImage()

private enum class Handle {
  NONE,
  MOVER,
  ROTATOR,
}

private class DraggableImageMouseListener(
  private val image: BufferedImage,
) : MouseAdapter() {
  private val imageBorder: Shape
  private val polaroid: Shape
  private val innerCircle = Ellipse2D.Double()
  private val outerCircle = Ellipse2D.Double()
  private val dragStart = Point2D.Double()
  private val center = Point2D.Double(100.0, 100.0) // center of the image
  private var angle = Math.toRadians(45.0) // rotation angle in radians
  private var angleOffset = 0.0 // angle - pointer angle at the start of a rotation drag
  private var activeHandle = Handle.NONE
  private var dragging = false

  init {
    val width = image.width
    val height = image.height
    imageBorder = RoundRectangle2D.Double(
      0.0,
      0.0,
      width.toDouble(),
      height.toDouble(),
      10.0,
      10.0,
    )
    polaroid = Rectangle2D.Double(-2.0, -2.0, width + 4.0, height + 20.0)
    setCirclesCenter(center)
  }

  private fun setCirclesCenter(pt: Point2D) {
    val cx = pt.x
    val cy = pt.y
    innerCircle.setFrameFromCenter(cx, cy, cx + INNER_RADIUS, cy + INNER_RADIUS)
    outerCircle.setFrameFromCenter(cx, cy, cx + OUTER_RADIUS, cy + OUTER_RADIUS)
  }

  private fun getHandleAt(pt: Point2D) = when {
    innerCircle.contains(pt) -> Handle.MOVER
    outerCircle.contains(pt) -> Handle.ROTATOR
    else -> Handle.NONE
  }

  private fun setActiveHandle(
    handle: Handle,
    c: Component,
  ) {
    if (activeHandle != handle) {
      activeHandle = handle
      c.cursor = Cursor.getPredefinedCursor(getCursorType(handle))
      c.repaint()
    }
  }

  private fun getCursorType(handle: Handle) = when (handle) {
    Handle.MOVER -> Cursor.MOVE_CURSOR
    Handle.ROTATOR -> Cursor.HAND_CURSOR
    Handle.NONE -> Cursor.DEFAULT_CURSOR
  }

  private fun getPointerAngle(e: MouseEvent) = atan2(e.y - center.y, e.x - center.x)

  fun paint(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val w2 = image.width / 2.0
    val h2 = image.height / 2.0
    val at = AffineTransform.getTranslateInstance(center.x - w2, center.y - h2)
    at.rotate(angle, w2, h2)
    g2.paint = BORDER_COLOR
    g2.stroke = BORDER_STROKE
    val s = at.createTransformedShape(polaroid)
    g2.fill(s)
    g2.draw(s)
    g2.drawImage(image, at, null)
    if (activeHandle == Handle.ROTATOR) {
      val donut = Area(outerCircle)
      donut.subtract(Area(innerCircle))
      g2.paint = HOVER_COLOR
      g2.fill(donut)
    } else if (activeHandle == Handle.MOVER) {
      g2.paint = HOVER_COLOR
      g2.fill(innerCircle)
    }
    g2.paint = BORDER_COLOR
    g2.stroke = BORDER_STROKE
    g2.draw(at.createTransformedShape(imageBorder))
    g2.dispose()
  }

  override fun mouseMoved(e: MouseEvent) {
    setActiveHandle(getHandleAt(e.point), e.component)
  }

  override fun mouseExited(e: MouseEvent) {
    if (!dragging) {
      setActiveHandle(Handle.NONE, e.component)
    }
  }

  override fun mousePressed(e: MouseEvent) {
    if (!SwingUtilities.isLeftMouseButton(e)) {
      return
    }
    val handle = getHandleAt(e.point)
    setActiveHandle(handle, e.component)
    dragging = handle != Handle.NONE
    if (handle == Handle.ROTATOR) {
      angleOffset = angle - getPointerAngle(e)
    } else if (handle == Handle.MOVER) {
      dragStart.setLocation(e.point)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (!dragging) {
      return
    }
    if (activeHandle == Handle.ROTATOR) {
      angle = angleOffset + getPointerAngle(e)
    } else if (activeHandle == Handle.MOVER) {
      val dx = e.x - dragStart.x
      val dy = e.y - dragStart.y
      center.setLocation(center.x + dx, center.y + dy)
      setCirclesCenter(center)
      dragStart.setLocation(e.point)
    }
    e.component.repaint()
  }

  override fun mouseReleased(e: MouseEvent) {
    if (dragging && SwingUtilities.isLeftMouseButton(e)) {
      dragging = false
      setActiveHandle(getHandleAt(e.point), e.component)
    }
  }

  companion object {
    private val BORDER_STROKE = BasicStroke(4f)
    private val BORDER_COLOR = Color.WHITE
    private val HOVER_COLOR = Color(0x64_64_FF_C8, true)
    private const val INNER_RADIUS = 20.0
    private const val OUTER_RADIUS = INNER_RADIUS * 3.0
  }
}

private fun makeMissingImage(): BufferedImage {
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
