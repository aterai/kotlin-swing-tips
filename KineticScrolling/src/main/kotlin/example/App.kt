package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.abs

fun makeUI(): Component {
  val scroll = JScrollPane().also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }

  // val viewport = scroll.getViewport() // Java 6
  val viewport = object : JViewport() { // Java 7
    val middleWeight = false
    private var adjusting = false

    override fun revalidate() {
      if (!middleWeight && adjusting) {
        return
      }
      super.revalidate()
    }

    override fun setViewPosition(p: Point) {
      if (middleWeight) {
        super.setViewPosition(p)
      } else {
        adjusting = true
        super.setViewPosition(p)
        adjusting = false
      }
    }
  }
  scroll.viewport = viewport

  val icon = Thread
    .currentThread()
    .contextClassLoader
    .getResource("example/GIANT_TCR1_2013.jpg")
    ?.openStream()
    ?.use(ImageIO::read)
    ?.let { ImageIcon(it) }
    ?: MissingIcon()
  val label = JLabel(icon)
  viewport.add(label)
  val l1 = KineticScrollingListener1(label)
  val l2 = KineticScrollingListener2(label)

  val r1 = JRadioButton("scrollRectToVisible", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      viewport.removeMouseListener(l2)
      viewport.removeMouseMotionListener(l2)
      viewport.removeHierarchyListener(l2)
      viewport.addMouseMotionListener(l1)
      viewport.addMouseListener(l1)
      viewport.addHierarchyListener(l1)
    }
  }

  val r2 = JRadioButton("setViewPosition")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      viewport.removeMouseListener(l1)
      viewport.removeMouseMotionListener(l1)
      viewport.removeHierarchyListener(l1)
      viewport.addMouseMotionListener(l2)
      viewport.addMouseListener(l2)
      viewport.addHierarchyListener(l2)
    }
  }

  val box = Box.createHorizontalBox()
  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    box.add(it)
    bg.add(it)
  }

  viewport.addMouseMotionListener(l1)
  viewport.addMouseListener(l1)
  viewport.addHierarchyListener(l1)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(box, BorderLayout.NORTH)
    scroll.preferredSize = Dimension(320, 240)
  }
}

private class KineticScrollingListener1(
  private val label: JComponent,
) : MouseAdapter(),
  HierarchyListener {
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
    val vp = viewport.viewPosition // SwingUtilities.convertPoint(vp, 0, 0, lbl)
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

private class KineticScrollingListener2(
  private val label: JComponent,
) : MouseAdapter(),
  HierarchyListener {
  private val dc = label.cursor
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val startPt = Point()
  private val delta = Point()
  private val inside = Timer(DELAY) { e ->
    val c = SwingUtilities.getUnwrappedParent(label)
    val viewport = c as? JViewport
    val vp = viewport?.viewPosition ?: Point()
    vp.translate(-delta.x, -delta.y)
    viewport?.viewPosition = vp
    if (abs(delta.x) > 0 || abs(delta.y) > 0) {
      delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      // Outside
      if (vp.x < 0 || vp.x + c.width - label.width > 0) {
        delta.x = (delta.x * D).toInt()
      }
      if (vp.y < 0 || vp.y + c.height - label.height > 0) {
        delta.y = (delta.y * D).toInt()
      }
    } else {
      (e.source as? Timer)?.stop() // inside.stop()
      if (viewport != null && !isInside(viewport, label)) {
        outside.start()
      }
    }
  }
  private val outside = Timer(DELAY) { e ->
    val viewport = SwingUtilities.getUnwrappedParent(label)
    val vp = (viewport as? JViewport)?.viewPosition ?: Point()
    if (vp.x < 0) {
      vp.x = (vp.x * D).toInt()
    }
    if (vp.y < 0) {
      vp.y = (vp.y * D).toInt()
    }
    if (vp.x + viewport.width - label.width > 0) {
      vp.x = (vp.x - (vp.x + viewport.width - label.width) * (1.0 - D)).toInt()
    }
    if (vp.y + viewport.height > label.height) {
      vp.y = (vp.y - (vp.y + viewport.height - label.height) * (1.0 - D)).toInt()
    }
    (viewport as? JViewport)?.viewPosition = vp
    if (viewport is JViewport && isInside(viewport, label)) {
      (e.source as? Timer)?.stop() // outside.stop()
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hc
    startPt.location = e.point
    inside.stop()
    outside.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val viewport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return
    val vp = viewport.viewPosition
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    viewport.viewPosition = vp
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    startPt.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = dc
    val viewport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return
    if (isInside(viewport, label)) {
      inside.start()
    } else {
      outside.start()
    }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      inside.stop()
      outside.stop()
    }
  }

  private fun isInside(
    viewport: JViewport,
    c: JComponent,
  ): Boolean {
    val pt = viewport.viewPosition
    val ww = pt.x >= 0 && pt.x + viewport.width - c.width <= 0
    val hh = pt.y >= 0 && pt.y + viewport.height - c.height <= 0
    return ww && hh
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private const val D = .8
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

  override fun getIconWidth() = 2014

  override fun getIconHeight() = 2014
}
