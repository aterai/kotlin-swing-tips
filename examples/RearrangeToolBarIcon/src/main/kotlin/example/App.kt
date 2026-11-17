package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

private const val PATH = "toolbarButtonGraphics/general/"
private val log = JTextArea()

fun createUI(): Component {
  val toolBar = JToolBar("ToolBarButton")
  toolBar.isFloatable = false
  toolBar.border = BorderFactory.createEmptyBorder(2, 2, 2, 0)
  listOf(
    "Copy24.gif",
    "Cut24.gif",
    "Paste24.gif",
    "Delete24.gif",
    "Undo24.gif",
    "Redo24.gif",
    "Help24.gif",
    "Open24.gif",
    "Save24.gif",
  ).map { createToolBarButton(it) }.forEach { toolBar.add(it) }

  log.isEditable = false
  return JPanel(BorderLayout()).also {
    it.add(JLayer(toolBar, ReorderingLayerUI()), BorderLayout.NORTH)
    it.add(JScrollPane(log))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBarButton(name: String): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource(PATH + name)
  val icon = url?.let { ImageIcon(it) } ?: UIManager.getIcon("html.missingImage")
  val button = JButton(icon)
  button.isFocusable = false
  button.toolTipText = name
  // Fired only by a click, not by a drag that exceeds the drag threshold
  button.addActionListener { log.append("$name clicked\n") }
  return button
}

private class ReorderingLayerUI<V : Container> : LayerUI<V>() {
  private val startPt = Point()
  private val dragOffset = Point()
  private val ghostPt = Point()
  private val dragThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var isDragging = false

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val dc = draggingComponent
    if (isDragging && dc != null) {
      val parent = dc.parent
      val g2 = g.create() as? Graphics2D ?: return
      // Hide the original button to make it look like a gap
      val r = SwingUtilities.convertRectangle(parent, dc.bounds, c)
      g2.paint = parent.background
      g2.fill(r)
      // Paint the ghost of the dragging button
      g2.translate(ghostPt.x, ghostPt.y)
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .5f)
      dc.paint(g2)
      g2.dispose()
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out V>) {
    val parent = l.view
    val c = e.component
    when (e.id) {
      MouseEvent.MOUSE_PRESSED -> {
        val isChild = c != null && c.parent == parent
        if (SwingUtilities.isLeftMouseButton(e) && isChild) {
          // The candidate for dragging; the drag starts when the threshold is exceeded
          draggingComponent = c
          dragOffset.location = e.point
          startPt.location = SwingUtilities.convertPoint(c, e.point, parent)
        }
      }

      MouseEvent.MOUSE_RELEASED -> if (isDragging) {
        isDragging = false
        l.repaint()
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    val c = e.component
    val isLeftDragged =
      e.id == MouseEvent.MOUSE_DRAGGED && SwingUtilities.isLeftMouseButton(e)
    // The dragged events are dispatched to the component that received the press
    if (isLeftDragged && c != null && c == draggingComponent) {
      val parent = l.view
      val pt = SwingUtilities.convertPoint(c, e.point, parent)
      if (!isDragging && startPt.distance(pt) > dragThreshold) {
        startDragging(c)
      }
      if (isDragging) {
        rearrange(parent, c, pt)
        val p = Point(pt.x - dragOffset.x, pt.y - dragOffset.y)
        ghostPt.location = SwingUtilities.convertPoint(parent, p, l)
        l.repaint()
      }
    }
  }

  private fun startDragging(c: Component) {
    isDragging = true
    if (c is AbstractButton) {
      // Cancel the click: DefaultButtonModel#setPressed(false) fires an ActionEvent
      // if the model is still armed, so the armed state must be cleared first.
      val m = c.model
      m.isArmed = false
      m.isPressed = false
      m.isRollover = false
    }
  }

  private fun rearrange(parent: Container, dc: Component, pt: Point) {
    val cur = parent.getComponentZOrder(dc)
    for (i in 0..<parent.componentCount) {
      val c = parent.getComponent(i)
      val r = c.bounds
      if (c != dc && r.x <= pt.x && pt.x < r.x + r.width) {
        // Swap when the cursor passes the center of the adjacent component
        val isPrevArea = pt.x < r.x + r.width / 2
        if (if (isPrevArea) cur > i else cur < i) {
          // Container#setComponentZOrder(...) does not call removeNotify(),
          // so the button keeps receiving the mouse dragged events.
          parent.setComponentZOrder(dc, i)
          parent.revalidate()
        }
        break
      }
    }
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
