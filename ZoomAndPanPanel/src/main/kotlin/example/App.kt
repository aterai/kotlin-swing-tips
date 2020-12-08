package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val path = "example/CRW_3857_JFR.jpg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(ZoomAndPanePanel(img)))
    it.preferredSize = Dimension(320, 240)
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

private class ZoomAndPanePanel(@field:Transient private val img: Image) : JPanel() {
  private val zoomTransform = AffineTransform()
  private val imageRect = Rectangle(img.getWidth(this), img.getHeight(this))

  @Transient
  private var handler: ZoomHandler? = null

  @Transient
  private var listener: DragScrollListener? = null

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color(0x55_FF_00_00, true)
    val r = Rectangle(500, 140, 150, 150)
    g2.drawImage(img, zoomTransform, this)
    g2.fill(zoomTransform.createTransformedShape(r))
    g2.dispose()
  }

  override fun getPreferredSize(): Dimension {
    val r = zoomTransform.createTransformedShape(imageRect).bounds
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
    private val zoomRange = DefaultBoundedRangeModel(
      0,
      EXTENT,
      MIN_ZOOM,
      MAX_ZOOM + EXTENT
    )

    override fun mouseWheelMoved(e: MouseWheelEvent) {
      val dir = e.wheelRotation
      val z = zoomRange.value
      zoomRange.value = z + EXTENT * if (dir > 0) -1 else 1
      if (z != zoomRange.value) {
        val c = e.component
        val p = SwingUtilities.getAncestorOfClass(JViewport::class.java, c)
        if (p is JViewport) {
          val ovr = p.viewRect
          val s = if (dir > 0) 1.0 / ZOOM_MULTIPLICATION_FACTOR else ZOOM_MULTIPLICATION_FACTOR
          zoomTransform.scale(s, s)
          val nvr = AffineTransform.getScaleInstance(s, s).createTransformedShape(ovr).bounds
          val vp = nvr.location
          vp.translate((nvr.width - ovr.width) / 2, (nvr.height - ovr.height) / 2)
          p.viewPosition = vp
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

private class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()
  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      val cp = SwingUtilities.convertPoint(c, e.point, p)
      val vp = p.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, p.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      pp.location = SwingUtilities.convertPoint(c, e.point, p)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
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
