package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import kotlin.math.abs

fun makeUI(): Component {
  val label = JLabel(ImageIcon(makeMissingImage()))
  val scroll = JScrollPane(label)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  val viewport = scroll.viewport
  val l1 = KineticScrollingListener(label)
  viewport.addMouseMotionListener(l1)
  viewport.addMouseListener(l1)
  viewport.addHierarchyListener(l1)
  return JPanel(BorderLayout()).also {
    it.add(JLayer(scroll, OverscrollEdgeEffectLayerUI()))
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

private class KineticScrollingListener(
  private val label: JComponent
) : MouseAdapter(), HierarchyListener {
  private val dc = label.cursor
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scroller: Timer
  private val startPt = Point()
  private val delta = Point()

  init {
    this.scroller = Timer(DELAY) { e ->
      val src = e.source
      val viewport = SwingUtilities.getUnwrappedParent(label) as? JViewport
      val vp = viewport?.viewPosition ?: Point()
      vp.translate(-delta.x, -delta.y)
      label.scrollRectToVisible(Rectangle(vp, viewport?.size ?: Dimension()))
      if (abs(delta.x) > 0 || abs(delta.y) > 0) {
        delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      } else if (src is Timer) {
        src.stop()
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hc
    startPt.location = e.point
    scroller.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val viewport = e.component as? JViewport ?: return
    val vp = viewport.viewPosition
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    label.scrollRectToVisible(Rectangle(vp, viewport.size))
    startPt.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = dc
    scroller.start()
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      scroller.stop()
    }
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private const val D = .8
  }
}

private class OverscrollEdgeEffectLayerUI : LayerUI<JScrollPane>() {
  private val color = Color(0xAA_AA_EE_FF.toInt(), true)
  private val mousePt = Point()
  private val animator = Timer(20, null)
  private val oval: Ellipse2D = Ellipse2D.Double()
  private var ovalHeight = 0.0
  private var delta = 0

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*> && ovalHeight > 0.0) {
      val rh = (c.view as? JScrollPane)?.viewport?.viewRect?.height ?: 0
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = color
      if (oval.y < 0) {
        oval.setFrame(oval.x, -ovalHeight, oval.width, ovalHeight * 2.0)
      } else {
        oval.setFrame(oval.x, rh - ovalHeight, oval.width, ovalHeight * 2.0)
      }
      g2.fill(oval)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    if (e.component is JViewport) {
      val id = e.id
      if (id == MouseEvent.MOUSE_PRESSED) {
        mousePt.location = e.point
      } else if (ovalHeight > 0.0 && id == MouseEvent.MOUSE_RELEASED) {
        ovalShrinking(l)
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    if (c is JViewport && e.id == MouseEvent.MOUSE_DRAGGED && !animator.isRunning) {
      val viewport = l.view.viewport
      val d = viewport.view.size
      val r = viewport.viewRect
      val p = SwingUtilities.convertPoint(c, e.point, l.view)
      val ow = p.getX().coerceAtLeast(r.getWidth() - p.getX())
      val ox = p.getX() - ow
      val dy = e.point.y - mousePt.y
      if (isDragReversed(dy)) {
        // The y-axis drag direction has been reversed
        ovalShrinking(l)
      } else if (r.y == 0 && dy >= 0) {
        // top edge
        ovalHeight = (r.getHeight() / 8.0).coerceAtMost(p.getY() / 8.0)
        oval.setFrame(ox, -ovalHeight, ow * 2.2, ovalHeight * 2.0)
      } else if (d.height == r.y + r.height && dy <= 0) {
        // bottom edge
        ovalHeight = (r.getHeight() / 8.0).coerceAtMost((r.getHeight() - p.getY()) / 8.0)
        oval.setFrame(ox, r.getHeight() - ovalHeight, ow * 2.2, ovalHeight * 2.0)
      }
      mousePt.location = e.point
      delta = dy
      l.repaint()
    }
  }

  private fun isDragReversed(dy: Int): Boolean {
    val b1 = delta > 0 && dy < 0
    val b2 = delta < 0 && dy > 0
    return b1 || b2
  }

  private fun ovalShrinking(l: JLayer<out JScrollPane>) {
    if (ovalHeight > 0.0 && !animator.isRunning) {
      val handler = ActionListener {
        if (ovalHeight > 0.0 && animator.isRunning) {
          ovalHeight = (ovalHeight * .67 - .5).coerceAtLeast(0.0)
          l.repaint()
        } else {
          animator.stop()
          for (a in animator.actionListeners) {
            animator.removeActionListener(a)
          }
        }
      }
      animator.addActionListener(handler)
      animator.start()
    }
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
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

  override fun getIconWidth() = 316

  override fun getIconHeight() = 1024
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
