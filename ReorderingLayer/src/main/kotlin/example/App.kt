package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.roundToInt

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createMatteBorder(10, 5, 5, 5, Color.GREEN)
  listOf<Component>(
    JLabel("<html>000<br>00<br>00"),
    JButton("1"),
    JCheckBox("2"),
    JTextField("3"),
  ).forEach {
    addDraggablePanel(box, it)
  }
  return JPanel(BorderLayout()).also {
    it.add(JLayer(box, ReorderingLayerUI<JComponent>()), BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addDraggablePanel(
  parent: Container,
  c: Component,
) {
  val l = JLabel(" %04d ".format(parent.componentCount))
  l.isOpaque = true
  l.background = Color.RED
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createCompoundBorder(
    BorderFactory.createEmptyBorder(5, 5, 5, 5),
    BorderFactory.createLineBorder(Color.BLUE, 2),
  )
  p.add(l, BorderLayout.WEST)
  p.add(c)
  p.isOpaque = false
  parent.add(p)
}

private class ReorderingLayerUI<V : JComponent> : LayerUI<V>() {
  private val startPt = Point()
  private val dragOffset = Point()
  private val canvas = JPanel()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var fillerComponent: Component? = null

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    if (c is JLayer<*> && draggingComponent != null) {
      SwingUtilities.paintComponent(g, draggingComponent, canvas, DRAGGING_RECT)
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
    val c = e.component
    when (e.id) {
      MouseEvent.MOUSE_PRESSED -> if (parent.componentCount > 0 && c is JLayer<*>) {
        startPt.location = e.point
        parent.repaint()
      }

      MouseEvent.MOUSE_RELEASED -> if (draggingComponent != null) {
        // swap the dragging panel and the temporary filler
        val idx = parent.getComponentZOrder(fillerComponent)
        replaceComponents(parent, fillerComponent, draggingComponent, idx)
        draggingComponent = null
      }
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out V>,
  ) {
    if (e.id == MouseEvent.MOUSE_DRAGGED && e.component is JLayer<*>) {
      val parent = l.view
      val pt = e.point
      if (draggingComponent == null) { // MotionThreshold
        if (startPt.distance(pt) > gestureMotionThreshold) {
          startDragging(parent, e)
        }
        return
      }
      // update the filler panel location
      if (!PREV_RECT.contains(pt)) {
        updateFillerLocation(parent, fillerComponent, pt)
      }

      // update the dragging panel location
      updateDraggingPanelLocation(parent, e, dragOffset)
      parent.repaint()
    }
  }

  private fun updateDraggingPanelLocation(
    parent: JComponent,
    e: MouseEvent,
    dragOffset: Point,
  ) {
    val pt = SwingUtilities.convertPoint(e.component, e.point, parent)
    val r = SwingUtilities.calculateInnerArea(parent, INNER_RECT)
    val y = (pt.y - dragOffset.y).coerceIn(r.y, r.y + r.height - DRAGGING_RECT.height)
    DRAGGING_RECT.setLocation(r.x, y)
  }

  private fun updateFillerLocation(
    parent: Container,
    filler: Component?,
    pt: Point,
  ) {
    // change the temporary filler location
    for (i in 0..<parent.componentCount) {
      val c = parent.getComponent(i)
      val r = c.bounds
      if (c == filler && r.contains(pt)) {
        return
      }
      val tgt = getTargetIndex(r, pt, i)
      if (tgt >= 0) {
        replaceComponents(parent, filler, filler, tgt)
        return
      }
    }
  }

  private fun startDragging(
    parent: JComponent,
    e: MouseEvent,
  ) {
    val pt = e.point
    val c = parent.getComponentAt(pt)
    val index = parent.getComponentZOrder(c)
    if (c == parent || index < 0) {
      return
    }
    draggingComponent = c
    val r = c.bounds
    DRAGGING_RECT.bounds = r // save draggingComponent size
    dragOffset.setLocation(pt.x - r.x, pt.y - r.y)
    fillerComponent = Box.createRigidArea(r.size)
    replaceComponents(parent, c, fillerComponent, index)
    updateDraggingPanelLocation(parent, e, dragOffset)
  }

  private fun getTargetIndex(
    r: Rectangle,
    pt: Point,
    i: Int,
  ): Int {
    val ht2 = (r.height / 2f).roundToInt()
    TOP_HALF_RECT.setBounds(r.x, r.y, r.width, ht2)
    BOTTOM_HALF_RECT.setBounds(r.x, r.y + ht2, r.width, ht2)
    return when {
      TOP_HALF_RECT.contains(pt) -> i.also {
        PREV_RECT.bounds = TOP_HALF_RECT
      }.takeIf { it > 1 } ?: 0

      BOTTOM_HALF_RECT.contains(pt) -> i.also {
        PREV_RECT.bounds = BOTTOM_HALF_RECT
      }

      else -> -1
    }
  }

  private fun replaceComponents(
    parent: Container,
    remove: Component?,
    insert: Component?,
    idx: Int,
  ) {
    if (insert == null) {
      return
    }
    parent.remove(remove)
    parent.add(insert, idx)
    parent.revalidate()
    parent.repaint()
  }

  companion object {
    private val TOP_HALF_RECT = Rectangle()
    private val BOTTOM_HALF_RECT = Rectangle()
    private val INNER_RECT = Rectangle()
    private val PREV_RECT = Rectangle()
    private val DRAGGING_RECT = Rectangle()
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
