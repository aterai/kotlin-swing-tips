package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.imageio.ImageIO
import javax.swing.*

fun createUI(): Component {
  val scroll = JScrollPane().also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }

  // val viewport = scroll.getViewport() // Java 6
  val viewport = OverscrollViewport() // Java 7
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
  val l1 = ScrollRectToVisibleListener(label)
  val l2 = SetViewPositionListener(label)
  l1.install(viewport)

  val r1 = JRadioButton("scrollRectToVisible", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      l2.uninstall(viewport)
      l1.install(viewport)
    }
  }

  val r2 = JRadioButton("setViewPosition")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      l1.uninstall(viewport)
      l2.install(viewport)
    }
  }

  val box = Box.createHorizontalBox()
  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    box.add(it)
    bg.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(box, BorderLayout.NORTH)
    scroll.preferredSize = Dimension(320, 240)
  }
}

// JViewport#setViewPosition(Point) calls revalidate() since JDK 1.7.0 (to keep
// heavyweight/lightweight mixing consistent), and ViewportLayout then clamps
// the view position back inside the view bounds. Skip that revalidate() while
// the position is being set so the view can be scrolled beyond its edges.
private class OverscrollViewport : JViewport() {
  private var adjusting = false

  override fun revalidate() {
    if (WEIGHT_MIXING || !adjusting) {
      super.revalidate()
    }
  }

  override fun setViewPosition(p: Point) {
    adjusting = true
    super.setViewPosition(p)
    adjusting = false
  }

  companion object {
    private const val WEIGHT_MIXING = false
  }
}

private abstract class KineticScrollingListener(
  protected val view: JComponent,
) : MouseAdapter(),
  HierarchyListener {
  // Velocity of the view position in pixels per timer tick
  protected val velocity = Point()
  private val defaultCursor = view.cursor
  private val handCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val prevPt = Point()

  fun install(c: JComponent) {
    c.addMouseListener(this)
    c.addMouseMotionListener(this)
    c.addHierarchyListener(this)
  }

  fun uninstall(c: JComponent) {
    c.removeMouseListener(this)
    c.removeMouseMotionListener(this)
    c.removeHierarchyListener(this)
  }

  protected fun getViewport() = SwingUtilities.getUnwrappedParent(view) as? JViewport

  // Returns true when the velocity has decayed to zero
  protected fun decelerate(): Boolean {
    velocity.setLocation((velocity.x * DAMPING).toInt(), (velocity.y * DAMPING).toInt())
    return velocity.x == 0 && velocity.y == 0
  }

  protected abstract fun drag(
    viewport: JViewport,
    dx: Int,
    dy: Int,
  )

  protected abstract fun startScrolling(viewport: JViewport)

  protected abstract fun stopScrolling()

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = handCursor
    prevPt.location = e.point
    velocity.setLocation(0, 0)
    stopScrolling()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val dx = prevPt.x - pt.x
    val dy = prevPt.y - pt.y
    (e.component as? JViewport)?.also { drag(it, dx, dy) }
    velocity.setLocation(SPEED * dx, SPEED * dy)
    prevPt.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defaultCursor
    (e.component as? JViewport)?.also { startScrolling(it) }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      stopScrolling()
    }
  }

  companion object {
    const val SPEED = 4
    const val DELAY = 10
    const val DAMPING = .8
  }
}

private class ScrollRectToVisibleListener(
  view: JComponent,
) : KineticScrollingListener(view) {
  private val scrollTimer = Timer(DELAY) { scroll() }

  private fun scroll() {
    getViewport()?.also { drag(it, velocity.x, velocity.y) }
    if (decelerate()) {
      scrollTimer.stop()
    }
  }

  override fun drag(
    viewport: JViewport,
    dx: Int,
    dy: Int,
  ) {
    val rect = viewport.viewRect
    rect.translate(dx, dy)
    view.scrollRectToVisible(rect)
  }

  override fun startScrolling(viewport: JViewport) {
    scrollTimer.start()
  }

  override fun stopScrolling() {
    scrollTimer.stop()
  }
}

private class SetViewPositionListener(
  view: JComponent,
) : KineticScrollingListener(view) {
  private val scrollTimer = Timer(DELAY) { scroll() }
  private val springBackTimer = Timer(DELAY) { springBack() }

  // Returns the view position nearest to vp that keeps the viewport
  // within the view bounds
  private fun getNearestInsidePosition(
    viewport: JViewport,
    vp: Point,
  ): Point {
    val maxX = maxOf(0, view.width - viewport.width)
    val maxY = maxOf(0, view.height - viewport.height)
    return Point(vp.x.coerceIn(0, maxX), vp.y.coerceIn(0, maxY))
  }

  private fun isInside(viewport: JViewport): Boolean {
    val vp = viewport.viewPosition
    return vp == getNearestInsidePosition(viewport, vp)
  }

  private fun scroll() {
    val viewport = getViewport() ?: return
    drag(viewport, velocity.x, velocity.y)
    val vp = viewport.viewPosition
    val inside = getNearestInsidePosition(viewport, vp)
    // Decelerate faster while the viewport is outside the view bounds
    if (vp.x != inside.x) {
      velocity.x = (velocity.x * DAMPING).toInt()
    }
    if (vp.y != inside.y) {
      velocity.y = (velocity.y * DAMPING).toInt()
    }
    if (decelerate()) {
      scrollTimer.stop()
      if (vp != inside) {
        springBackTimer.start()
      }
    }
  }

  private fun springBack() {
    val viewport = getViewport() ?: return
    val vp = viewport.viewPosition
    val inside = getNearestInsidePosition(viewport, vp)
    // Ease the view position back toward the nearest inside position;
    // toInt() truncates toward zero, so it always reaches the target
    vp.x = inside.x + ((vp.x - inside.x) * DAMPING).toInt()
    vp.y = inside.y + ((vp.y - inside.y) * DAMPING).toInt()
    viewport.viewPosition = vp
    if (vp == inside) {
      springBackTimer.stop()
    }
  }

  override fun drag(
    viewport: JViewport,
    dx: Int,
    dy: Int,
  ) {
    val vp = viewport.viewPosition
    vp.translate(dx, dy)
    viewport.viewPosition = vp
  }

  override fun startScrolling(viewport: JViewport) {
    if (isInside(viewport)) {
      scrollTimer.start()
    } else {
      springBackTimer.start()
    }
  }

  override fun stopScrolling() {
    scrollTimer.stop()
    springBackTimer.stop()
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
      contentPane.add(createUI())
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
