package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val img = runCatching { ImageIO.read(javaClass.getResource("CRW_3857_JFR.jpg")) }
      .onFailure { it.printStackTrace() }
      .getOrNull() ?: makeMissingImage()
    add(JScrollPane(ZoomAndPanePanel(img)))
    setPreferredSize(Dimension(320, 240))
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
}

class ZoomAndPanePanel(@field:Transient private val img: Image) : JPanel() {
  private val zoomTransform = AffineTransform()
  private val imageRect = Rectangle(img.getWidth(this), img.getHeight(this))
  @Transient
  private var handler: ZoomHandler? = null
  @Transient
  private var listener: DragScrollListener? = null

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(Color(0x55_FF_00_00, true))
    val r = Rectangle(500, 140, 150, 150)
    g2.drawImage(img, zoomTransform, this)
    g2.fill(zoomTransform.createTransformedShape(r))
    g2.dispose()
  }

  override fun getPreferredSize(): Dimension {
    val r = zoomTransform.createTransformedShape(imageRect).getBounds()
    return Dimension(r.width, r.height)
  }

  override fun updateUI() {
    removeMouseListener(listener)
    removeMouseMotionListener(listener)
    removeMouseWheelListener(handler)
    super.updateUI()
    listener = DragScrollListener()
    addMouseListener(listener)
    addMouseMotionListener(listener)
    handler = ZoomHandler()
    addMouseWheelListener(handler)
  }

  private inner class ZoomHandler : MouseAdapter() {
    private val zoomRange: BoundedRangeModel = DefaultBoundedRangeModel(
      0,
      EXTENT,
      MIN_ZOOM,
      MAX_ZOOM + EXTENT
    )

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      val dir = e.getWheelRotation()
      val z = zoomRange.getValue()
      zoomRange.setValue(z + EXTENT * if (dir > 0) -1 else 1)
      if (z != zoomRange.value) {
        val c = e.getComponent()
        val p = SwingUtilities.getAncestorOfClass(JViewport::class.java, c)
        if (p is JViewport) {
          val ovr = p.getViewRect()
          val s = if (dir > 0) 1.0 / ZOOM_MULTIPLICATION_FACTOR else ZOOM_MULTIPLICATION_FACTOR
          zoomTransform.scale(s, s)
          val nvr = AffineTransform.getScaleInstance(s, s).createTransformedShape(ovr).getBounds()
          val vp = nvr.getLocation()
          vp.translate((nvr.width - ovr.width) / 2, (nvr.height - ovr.height) / 2)
          p.setViewPosition(vp)
          c.revalidate()
          c.repaint()
        }
      }
    }
  }

  companion object {
    private const val ZOOM_MULTIPLICATION_FACTOR = 1.2
    private const val MIN_ZOOM = -10
    private const val MAX_ZOOM = 10
    private const val EXTENT = 1
  }
}

class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()
  override fun mouseDragged(e: MouseEvent) {
    val c = e.getComponent()
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      val cp = SwingUtilities.convertPoint(c, e.point, p)
      val vp = p.getViewPosition()
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, p.getSize()))
      pp.setLocation(cp)
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.getComponent()
    c.setCursor(hndCursor)
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      pp.setLocation(SwingUtilities.convertPoint(c, e.getPoint(), p))
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.getComponent().setCursor(defCursor)
  }
}

class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = getIconWidth()
    val h = getIconHeight()
    val gap = w / 5
    g2.setPaint(Color.WHITE)
    g2.fillRect(x, y, w, h)
    g2.setPaint(Color.RED)
    g2.setStroke(BasicStroke(w / 8f))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
