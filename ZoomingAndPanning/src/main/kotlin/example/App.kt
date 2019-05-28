package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  @Transient
  private var zoomAndPanHandler: ZoomAndPanHandler? = null
  private val icon: ImageIcon

  init {
    icon = ImageIcon(javaClass.getResource("CRW_3857_JFR.jpg"))
    setPreferredSize(Dimension(320, 240))
  }

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

  protected override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as Graphics2D
    g2.setTransform(zoomAndPanHandler?.coordAndZoomTransform)
    icon.paintIcon(this, g2, 0, 0)
    g2.dispose()
  }
}

internal class ZoomAndPanHandler : MouseAdapter() {
  private val zoomRange = DefaultBoundedRangeModel(0, EXTENT, MIN_ZOOM, MAX_ZOOM + EXTENT)
  val coordAndZoomTransform = AffineTransform()
  private val dragStartPoint = Point()

  override fun mousePressed(e: MouseEvent) {
    dragStartPoint.setLocation(e.getPoint())
  }

  override fun mouseDragged(e: MouseEvent) {
    val dragEndPoint = e.getPoint()
    val dragStart = transformPoint(dragStartPoint)
    val dragEnd = transformPoint(dragEndPoint)
    coordAndZoomTransform.translate(dragEnd.getX() - dragStart.getX(), dragEnd.getY() - dragStart.getY())
    dragStartPoint.setLocation(dragEndPoint)
    e.getComponent().repaint()
  }

  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val dir = e.getWheelRotation()
    val z = zoomRange.getValue()
    zoomRange.setValue(z + EXTENT * if (dir > 0) -1 else 1)
    if (z == zoomRange.getValue()) {
      return
    }
    val c = e.getComponent()
    val r = c.getBounds()
    // Point p = e.getPoint();
    val p = Point(r.x + r.width / 2, r.y + r.height / 2)
    val p1 = transformPoint(p)
    val scale = if (dir > 0) 1 / ZOOM_MULTIPLICATION_FACTOR else ZOOM_MULTIPLICATION_FACTOR
    coordAndZoomTransform.scale(scale, scale)
    val p2 = transformPoint(p)
    coordAndZoomTransform.translate(p2.getX() - p1.getX(), p2.getY() - p1.getY())
    c.repaint()
  }

  // https://community.oracle.com/thread/1263955
  // How to implement Zoom & Pan in Java using Graphics2D
  private fun transformPoint(p1: Point): Point {
//    var inverse = coordAndZoomTransform
//    val hasInverse = coordAndZoomTransform.getDeterminant() != 0.0
//    if (hasInverse) {
//      try {
//        inverse = coordAndZoomTransform.createInverse()
//      } catch (ex: NoninvertibleTransformException) {
//        // should never happen
//        assert(false)
//      }
//    }
    val inverse = runCatching { coordAndZoomTransform.createInverse() }
      .getOrNull() ?: coordAndZoomTransform
    val p2 = Point()
    inverse.transform(p1, p2)
    return p2
  }

  companion object {
    private const val ZOOM_MULTIPLICATION_FACTOR = 1.2
    private const val MIN_ZOOM = -10
    private const val MAX_ZOOM = 10
    private const val EXTENT = 1
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
