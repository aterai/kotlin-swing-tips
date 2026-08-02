package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Dimension2D
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

fun createUI(): Component {
  val label = JLabel(ImageIcon(createMissingImage()))

  val scroll = object : JScrollPane(label) {
    override fun updateUI() {
      super.updateUI()
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
    }
  }

  val viewport = scroll.getViewport()
  val kineticListener = KineticScrollingListener(label)
  viewport.addMouseMotionListener(kineticListener)
  viewport.addMouseListener(kineticListener)
  viewport.addHierarchyListener(kineticListener)

  val layerUi = OverscrollEdgeEffectLayerUI()
  val layer = JLayer<JScrollPane>(scroll, layerUi)

  val orientationCombo = JComboBox(EdgeOrientation.entries.toTypedArray())
  orientationCombo.addItemListener { e ->
    val item = e.getItem()
    if (e.getStateChange() == ItemEvent.SELECTED && item is EdgeOrientation) {
      layerUi.setEdgeOrientation(item)
      layer.repaint()
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(JLabel("EdgeOrientation: "))
  box.add(Box.createHorizontalStrut(2))
  box.add(orientationCombo)
  box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(layer)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private enum class EdgeOrientation {
  VERTICAL,
  HORIZONTAL,
}

private class KineticScrollingListener(
  private val view: JComponent,
) : MouseAdapter(),
  HierarchyListener {
  private val defaultCursor = view.getCursor()
  private val dragCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scrollTimer = Timer(DELAY) { this.scroll(it) }
  private val dragStartPoint = Point()
  private val scrollVelocity = Point()

  private fun scroll(e: ActionEvent) {
    val viewport = SwingUtilities.getUnwrappedParent(view) as JViewport
    val rect = viewport.viewRect
    rect.translate(-scrollVelocity.x, -scrollVelocity.y)
    view.scrollRectToVisible(rect)
    if (abs(scrollVelocity.x) > 0 || abs(scrollVelocity.y) > 0) {
      val dx = scrollVelocity.x * DECELERATION
      val dy = scrollVelocity.y * DECELERATION
      scrollVelocity.setLocation(dx.toInt(), dy.toInt())
    } else {
      (e.getSource() as? Timer)?.stop()
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.setCursor(dragCursor)
    dragStartPoint.location = e.getPoint()
    scrollTimer.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.getPoint()
    val sx = SPEED * (pt.x - dragStartPoint.x)
    val sy = SPEED * (pt.y - dragStartPoint.y)
    scrollVelocity.setLocation(sx, sy)
    val viewport = e.component as? JViewport ?: return
    val rect = viewport.viewRect
    rect.translate(dragStartPoint.x - pt.x, dragStartPoint.y - pt.y)
    view.scrollRectToVisible(rect)
    dragStartPoint.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.setCursor(defaultCursor)
    scrollTimer.start()
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      scrollTimer.stop()
    }
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private const val DECELERATION = .8
  }
}

private class OverscrollEdgeEffectLayerUI : LayerUI<JScrollPane?>() {
  private val overscrollColor = Color(-0x55551101, true)
  private val dragPoint = Point()
  private val shrinkTimer = Timer(20, null)
  private val overscrollOval: Ellipse2D = Ellipse2D.Double()
  private var edgeOrientation: EdgeOrientation? = EdgeOrientation.VERTICAL
  private var atLeadingEdge = false
  private var overscrollSize = 0.0
  private var prevDelta = 0

  fun setEdgeOrientation(orientation: EdgeOrientation?) {
    this.edgeOrientation = orientation
    overscrollSize = 0.0
    prevDelta = 0
  }

  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    if (c is JLayer<*> && overscrollSize > 0.0) {
      val scroll = c.getView() as JScrollPane
      val r = scroll.getViewport().viewRect
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = overscrollColor
      val vertical = edgeOrientation == EdgeOrientation.VERTICAL
      val bound = if (vertical) r.getHeight() else r.getWidth()
      val pos = if (atLeadingEdge) -overscrollSize else bound - overscrollSize
      val pt: Point2D = Point()
      val dim: Dimension2D = Dimension()
      if (vertical) {
        pt.setLocation(overscrollOval.x, pos)
        dim.setSize(overscrollOval.width, overscrollSize * 2.0)
      } else {
        pt.setLocation(pos, overscrollOval.y)
        dim.setSize(overscrollSize * 2.0, overscrollOval.height)
      }
      overscrollOval.setFrame(pt, dim)
      g2.fill(overscrollOval)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
      )
    }
  }

  override fun uninstallUI(c: JComponent?) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    if (e.component is JViewport) {
      val id = e.getID()
      if (id == MouseEvent.MOUSE_PRESSED) {
        dragPoint.location = e.getPoint()
      } else if (overscrollSize > 0.0 && id == MouseEvent.MOUSE_RELEASED) {
        shrinkOverscroll(l)
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    val isDragged = e.getID() == MouseEvent.MOUSE_DRAGGED
    if (c is JViewport && isDragged && !shrinkTimer.isRunning) {
      val viewport = l.getView().getViewport()
      val viewSize = viewport.view.size
      val viewRect = viewport.viewRect
      val p = SwingUtilities.convertPoint(c, e.getPoint(), l.getView())
      updateOverscroll(e, p, viewRect, viewSize, l)
    }
  }

  // Handles both VERTICAL (top/bottom) and HORIZONTAL (left/right) edges by
  // treating the scroll axis as the "primary" axis and the perpendicular one
  // as the "secondary" axis, then swapping them back when painting the oval.
  private fun updateOverscroll(
    e: MouseEvent,
    p: Point,
    r: Rectangle,
    viewSize: Dimension,
    l: JLayer<out JScrollPane>,
  ) {
    val vertical = edgeOrientation == EdgeOrientation.VERTICAL
    val delta = if (vertical) {
      e.getPoint().y - dragPoint.y
    } else {
      e.getPoint().x - dragPoint.x
    }
    val axis = OverscrollAxis(vertical, p, r, viewSize)
    if (isDragReversed(delta)) {
      // The primary-axis drag direction has been reversed
      shrinkOverscroll(l)
    } else if (axis.primaryPos == 0.0 && delta >= 0) {
      // leading edge (top or left)
      atLeadingEdge = true
      overscrollSize = min(axis.primarySize, axis.primary) * OVERSCROLL_RATIO
      val secondaryStart = axis.secondaryStart
      val secondaryLen = axis.secondaryLen
      val primaryLen = overscrollSize * 2.0
      setOvalFrame(
        vertical,
        secondaryStart,
        -overscrollSize,
        secondaryLen,
        primaryLen,
      )
    } else if (axis.primaryBound == axis.primaryPos + axis.primarySize &&
      delta <= 0
    ) {
      // trailing edge (bottom or right)
      atLeadingEdge = false
      val remaining = axis.primarySize - axis.primary
      overscrollSize = min(axis.primarySize, remaining) * OVERSCROLL_RATIO
      val primaryStart = axis.primarySize - overscrollSize
      val secondaryStart = axis.secondaryStart
      val secondaryLen = axis.secondaryLen
      val primaryLen = overscrollSize * 2.0
      setOvalFrame(vertical, secondaryStart, primaryStart, secondaryLen, primaryLen)
    }
    dragPoint.location = e.getPoint()
    prevDelta = delta
    l.repaint()
  }

  private fun setOvalFrame(
    vertical: Boolean,
    secondaryPos: Double,
    primaryPos: Double,
    secondaryLen: Double,
    primaryLen: Double,
  ) {
    if (vertical) {
      overscrollOval.setFrame(secondaryPos, primaryPos, secondaryLen, primaryLen)
    } else {
      overscrollOval.setFrame(primaryPos, secondaryPos, primaryLen, secondaryLen)
    }
  }

  private class OverscrollAxis(
    vertical: Boolean,
    p: Point,
    r: Rectangle,
    viewSize: Dimension,
  ) {
    val primary = if (vertical) p.getY() else p.getX()
    val primarySize = if (vertical) r.getHeight() else r.getWidth()
    val primaryPos = (if (vertical) r.y else r.x).toDouble()
    val primaryBound = (if (vertical) viewSize.height else viewSize.width).toDouble()
    val secondaryStart: Double
    val secondaryLen: Double

    init {
      val secondary = if (vertical) p.getX() else p.getY()
      val secondarySize = if (vertical) r.getWidth() else r.getHeight()
      val halfSecondary = max(secondary, secondarySize - secondary)
      secondaryStart = secondary - halfSecondary
      secondaryLen = halfSecondary * OVAL_ASPECT_RATIO
    }
  }

  private fun isDragReversed(delta: Int): Boolean {
    val b1 = prevDelta > 0 && delta < 0
    val b2 = prevDelta < 0 && delta > 0
    return b1 || b2
  }

  private fun shrinkOverscroll(l: JLayer<out JScrollPane?>) {
    if (overscrollSize > 0.0 && !shrinkTimer.isRunning) {
      shrinkTimer.addActionListener { shrinkAnimation(l) }
      shrinkTimer.start()
    }
  }

  private fun shrinkAnimation(l: JLayer<out JScrollPane?>) {
    if (overscrollSize > 0.0 && shrinkTimer.isRunning) {
      overscrollSize = max(overscrollSize * SHRINK_RATIO - SHRINK_STEP, 0.0)
      l.repaint()
    } else {
      shrinkTimer.stop()
      for (a in shrinkTimer.actionListeners) {
        shrinkTimer.removeActionListener(a)
      }
    }
  }

  companion object {
    private const val OVERSCROLL_RATIO = 1.0 / 8.0
    private const val OVAL_ASPECT_RATIO = 2.2
    private const val SHRINK_RATIO = .67
    private const val SHRINK_STEP = .5
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.translate(x, y)
    g2.fillRect(0, 0, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(gap, gap, w - gap, h - gap)
    g2.drawLine(gap, h - gap, w - gap, gap)
    g2.dispose()
  }

  override fun getIconWidth() = 640

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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
