package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val path = "example/CRW_3857_JFR.jpg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) } ?: MissingIcon()
  val p = object : JPanel(BorderLayout()) {
    var zoomAndPanHandler: ZoomAndPanHandler? = null
    override fun updateUI() {
      removeMouseListener(zoomAndPanHandler)
      removeMouseMotionListener(zoomAndPanHandler)
      removeMouseWheelListener(zoomAndPanHandler)
      super.updateUI()
      zoomAndPanHandler = ZoomAndPanHandler()
      addMouseListener(zoomAndPanHandler)
      addMouseMotionListener(zoomAndPanHandler)
      addMouseWheelListener(zoomAndPanHandler)
    }

    override fun getPreferredSize() = Dimension(320, 240)

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.clearRect(0, 0, width, height)
      val g2 = g.create() as? Graphics2D ?: return
      g2.transform = zoomAndPanHandler?.coordAndZoomAtf
      icon.paintIcon(this, g2, 0, 0)
      g2.dispose()
    }
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class ZoomAndPanHandler : MouseAdapter() {
  private val zoomRange = DefaultBoundedRangeModel(0, EXTENT, MIN_ZOOM, MAX_ZOOM + EXTENT)
  val coordAndZoomAtf = AffineTransform()
  private val dragStartPoint = Point()

  override fun mousePressed(e: MouseEvent) {
    dragStartPoint.location = e.point
  }

  override fun mouseDragged(e: MouseEvent) {
    val dragEndPoint = e.point
    val dragStart = transformPoint(dragStartPoint)
    val dragEnd = transformPoint(dragEndPoint)
    coordAndZoomAtf.translate(dragEnd.getX() - dragStart.getX(), dragEnd.getY() - dragStart.getY())
    dragStartPoint.location = dragEndPoint
    e.component.repaint()
  }

  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val dir = e.wheelRotation
    val z = zoomRange.value
    zoomRange.value = z + EXTENT * if (dir > 0) -1 else 1
    if (z != zoomRange.value) {
      val pt = if (e.isControlDown) {
        val r = e.component.bounds
        Point2D.Double(r.centerX, r.centerY)
      } else {
        transformPoint(e.point)
      }
      val scale = if (dir > 0) 1 / ZOOM_FACTOR else ZOOM_FACTOR
      coordAndZoomAtf.translate(pt.x, pt.y)
      coordAndZoomAtf.scale(scale, scale)
      coordAndZoomAtf.translate(-pt.x, -pt.y)
      e.component.repaint()
    }
  }

  // https://community.oracle.com/thread/1263955
  // How to implement Zoom & Pan in Java using Graphics2D
  private fun transformPoint(p1: Point): Point {
    val inverse = runCatching { coordAndZoomAtf.createInverse() }.getOrNull() ?: coordAndZoomAtf
    val p2 = Point()
    inverse.transform(p1, p2)
    return p2
  }

  companion object {
    private const val ZOOM_FACTOR = 1.2
    private const val MIN_ZOOM = -10
    private const val MAX_ZOOM = 10
    private const val EXTENT = 1
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.paint = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 1000

  override fun getIconHeight() = 1000
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
