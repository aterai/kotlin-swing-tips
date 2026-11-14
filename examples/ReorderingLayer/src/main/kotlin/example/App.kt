package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.roundToInt

fun createUI(): Component {
  val box = ScrollableBox(BoxLayout.Y_AXIS)
  val borderColor = UIManager.getColor("controlShadow")
  box.border = BorderFactory.createMatteBorder(10, 5, 5, 5, borderColor)
  listOf<Component>(
    JLabel("<html>000<br>00<br>00"),
    JButton("1"),
    JCheckBox("2"),
    JTextField("3"),
    JComboBox(arrayOf("4", "44", "444")),
    JSpinner(),
    JSlider(),
    JProgressBar(0, 100),
  ).forEach {
    addDraggablePanel(box, it)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JLayer(box, ReorderingLayerUI<JComponent>())))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addDraggablePanel(
  parent: Container,
  c: Component,
) {
  val l = JLabel(DragHandleIcon())
  l.isOpaque = true
  l.background = UIManager.getColor("controlHighlight")
  l.foreground = UIManager.getColor("controlDkShadow")
  l.border = BorderFactory.createEmptyBorder(2, 6, 2, 6)
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(5, 5, 5, 5),
    BorderFactory.createLineBorder(UIManager.getColor("controlShadow")),
  )
  p.add(l, BorderLayout.WEST)
  p.add(c)
  p.isOpaque = false
  parent.add(p)
}

// A Box that fits the width of the JViewport
// JLayer delegates its Scrollable methods to the view
private class ScrollableBox(
  axis: Int,
) : Box(axis),
  Scrollable {
  override fun getPreferredScrollableViewportSize(): Dimension = preferredSize

  override fun getScrollableUnitIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = UNIT_INCREMENT

  override fun getScrollableBlockIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation ==
    SwingConstants.VERTICAL
  ) {
    visibleRect.height
  } else {
    visibleRect.width
  }

  override fun getScrollableTracksViewportWidth() = true

  override fun getScrollableTracksViewportHeight() = false

  companion object {
    private const val UNIT_INCREMENT = 16
  }
}

// A grip icon made of two columns of dots
private class DragHandleIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = c.foreground
    for (row in 0..<ROWS) {
      for (col in 0..<COLS) {
        g2.fillRect(col * (DOT + GAP), row * (DOT + GAP), DOT, DOT)
      }
    }
    g2.dispose()
  }

  override fun getIconWidth() = COLS * DOT + (COLS - 1) * GAP

  override fun getIconHeight() = ROWS * DOT + (ROWS - 1) * GAP

  companion object {
    private const val DOT = 2
    private const val GAP = 2
    private const val COLS = 2
    private const val ROWS = 3
  }
}

private class ReorderingLayerUI<V : JComponent> : LayerUI<V>() {
  private val innerRect = Rectangle()
  private val prevRect = Rectangle()
  private val draggingRect = Rectangle()
  private val startPt = Point()
  private val dragOffset = Point()
  private val canvas = JPanel()
  private val dragThreshold = DragSource.getDragThreshold()
  private var isDragging = false
  private var draggingComponent: Component? = null
  private var fillerComponent: Component? = null

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    if (c is JLayer<*> && isDragging) {
      SwingUtilities.paintComponent(g, draggingComponent, canvas, draggingRect)
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

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    val parent = l.view
    when (e.id) {
      // ignore the events of the child components such as JButton
      MouseEvent.MOUSE_PRESSED -> if (e.component is JLayer<*>) {
        startPt.location = e.point
      }

      MouseEvent.MOUSE_RELEASED -> if (isDragging) {
        // replace the temporary filler with the dragging panel
        val idx = parent.getComponentZOrder(fillerComponent)
        replaceComponent(parent, fillerComponent, draggingComponent, idx)
        isDragging = false
      }
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    if (e.id == MouseEvent.MOUSE_DRAGGED && e.component is JLayer<*>) {
      mouseDragged(l.view, e.point)
    }
  }

  private fun mouseDragged(
    parent: JComponent,
    pt: Point,
  ) {
    if (isDragging) {
      // keep the filler while the pointer stays in the half checked last time
      if (!prevRect.contains(pt)) {
        updateFillerLocation(parent, pt)
      }
      updateDraggingPanelLocation(parent, pt)
      // scroll the JViewport so that the dragging panel stays visible
      parent.scrollRectToVisible(draggingRect)
      parent.repaint()
    } else if (startPt.distance(pt) > dragThreshold) {
      startDragging(parent, pt)
    }
  }

  private fun startDragging(
    parent: JComponent,
    pt: Point,
  ) {
    // drag the panel under the pressed point, not the one under the current point
    val c = parent.getComponentAt(startPt) ?: return
    val index = parent.getComponentZOrder(c)
    if (index >= 0) {
      draggingComponent = c
      isDragging = true

      // The dragging panel is removed from the parent,
      // so its size is saved here to paint it in the JLayer
      val r = c.bounds
      draggingRect.bounds = r
      dragOffset.setLocation(startPt.x - r.x, startPt.y - r.y)
      prevRect.setBounds(0, 0, 0, 0)

      val filler = Box.createRigidArea(r.size)
      fillerComponent = filler
      replaceComponent(parent, c, filler, index)

      updateDraggingPanelLocation(parent, pt)
    }
  }

  private fun updateDraggingPanelLocation(
    parent: JComponent,
    pt: Point,
  ) {
    val r = SwingUtilities.calculateInnerArea(parent, innerRect)
    val top = r.y
    val bottom = r.y + r.height - draggingRect.height
    draggingRect.setLocation(r.x, (pt.y - dragOffset.y).coerceIn(top, maxOf(top, bottom)))
  }

  private fun updateFillerLocation(
    parent: Container,
    pt: Point,
  ) {
    val c = parent.getComponentAt(pt) ?: return
    val idx = parent.getComponentZOrder(c)
    if (idx >= 0 && c != fillerComponent) {
      // remember the half of the panel under the pointer
      // to avoid moving the filler back and forth after the layout changes
      val r = c.bounds
      val half = (r.height / 2f).roundToInt()
      val y = if (pt.y < r.y + half) r.y else r.y + half
      prevRect.setBounds(r.x, y, r.width, half)
      // move the temporary filler to the location of the panel
      replaceComponent(parent, fillerComponent, fillerComponent, idx)
    }
  }

  private fun replaceComponent(
    parent: Container,
    remove: Component?,
    add: Component?,
    i: Int,
  ) {
    parent.remove(remove)
    if (i >= 0 && add != null) {
      parent.add(add, i)
    }
    parent.revalidate()
    parent.repaint()
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
