package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createMatteBorder(10, 5, 5, 5, Color.GREEN))
    listOf<Component>(
      JLabel("<html>000<br>00<br>00"), JButton("1"), JCheckBox("2"), JTextField("3")
    ).forEach { addDraggablePanel(box, it) }
    add(JLayer(box, ReorderingLayerUI<JComponent>()), BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun addDraggablePanel(parent: Container, c: Component) {
    val idx = parent.getComponentCount()
    val l = JLabel(" %04d ".format(idx))
    l.setOpaque(true)
    l.setBackground(Color.RED)
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createCompoundBorder(
      BorderFactory.createEmptyBorder(5, 5, 5, 5),
      BorderFactory.createLineBorder(Color.BLUE, 2)
    ))
    p.add(l, BorderLayout.WEST)
    p.add(c)
    p.setOpaque(false)
    parent.add(p)
  }
}

class ReorderingLayerUI<V : JComponent> : LayerUI<V>() {
  private val startPt = Point()
  private val dragOffset = Point()
  private val canvas = JPanel()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var fillerComponent: Component? = null

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*> && draggingComponent != null) {
      SwingUtilities.paintComponent(g, draggingComponent, canvas, DRAGGING_RECT)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.setLayerEventMask(AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK)
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.setLayerEventMask(0)
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    val parent = l.getView()
    when (e.getID()) {
      MouseEvent.MOUSE_PRESSED -> if (parent.getComponentCount() > 0) {
        startPt.setLocation(e.getPoint())
        l.repaint()
      }
      MouseEvent.MOUSE_RELEASED -> if (draggingComponent != null) {
        // swap the dragging panel and the dummy filler
        val idx = parent.getComponentZOrder(fillerComponent)
        replaceComponents(parent, fillerComponent, draggingComponent, idx)
        draggingComponent = null
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
      val parent = l.getView()
      val pt = e.getPoint()
      if (draggingComponent == null) { // MotionThreshold
        if (startPt.distance(pt) > gestureMotionThreshold) {
          startDragging(parent, pt)
        }
        return
      }
      // update the filler panel location
      if (!PREV_RECT.contains(pt)) {
        updateFillerLocation(parent, fillerComponent, pt)
      }
      // update the dragging panel location
      updateDraggingPanelLocation(parent, pt, dragOffset)
      l.repaint()
    }
  }

  private fun updateDraggingPanelLocation(parent: JComponent, pt: Point, dragOffset: Point) {
    val i = parent.getInsets()
    val r = SwingUtilities.calculateInnerArea(parent, INNER_RECT)
    val x = r.x
    val y = pt.y - dragOffset.y
    val h = DRAGGING_RECT.height
    val yy: Int
    yy = if (y < i.top) {
      i.top
    } else {
      if (r.contains(x, y + h)) y else r.height + i.top - h
    }
    DRAGGING_RECT.setLocation(x, yy)
  }

  private fun updateFillerLocation(parent: Container, filler: Component?, pt: Point) {
    // change the dummy filler location
    for (i in 0 until parent.getComponentCount()) {
      val c = parent.getComponent(i)
      val r = c.getBounds()
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

  private fun startDragging(parent: JComponent, pt: Point) {
    val c = parent.getComponentAt(pt)
    val index = parent.getComponentZOrder(c)
    if (c == parent || index < 0) {
      return
    }
    draggingComponent = c
    val r = c.getBounds()
    DRAGGING_RECT.bounds = r // save draggingComponent size
    dragOffset.setLocation(pt.x - r.x, pt.y - r.y)
    fillerComponent = Box.createRigidArea(r.getSize())
    replaceComponents(parent, c, fillerComponent, index)
    updateDraggingPanelLocation(parent, pt, dragOffset)
  }

  private fun getTargetIndex(r: Rectangle, pt: Point, i: Int): Int {
    val ht2 = (.5 + r.height * .5).toInt()
    TOP_HALF_RECT.setBounds(r.x, r.y, r.width, ht2)
    BOTTOM_HALF_RECT.setBounds(r.x, r.y + ht2, r.width, ht2)
    return when {
      TOP_HALF_RECT.contains(pt) -> {
        PREV_RECT.setBounds(TOP_HALF_RECT)
        if (i > 1) i else 0
      }
      BOTTOM_HALF_RECT.contains(pt) -> {
        PREV_RECT.setBounds(BOTTOM_HALF_RECT)
        i
      }
      else -> -1
    }
  }

  private fun replaceComponents(parent: Container, remove: Component?, insert: Component?, idx: Int) {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
