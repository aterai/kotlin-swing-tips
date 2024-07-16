package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

private const val PATH = "toolbarButtonGraphics/general/"

fun makeUI(): Component {
  val toolBar = JToolBar("ToolBarButton")
  toolBar.isFloatable = false
  val dh = DragHandler()
  toolBar.addMouseListener(dh)
  toolBar.addMouseMotionListener(dh)
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

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBarButton(name: String): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource(PATH + name)
  val icon = url?.let { ImageIcon(it) } ?: UIManager.getIcon("html.missingImage")
  val b = JLabel(icon)
  b.isOpaque = false
  return b
}

private class DragHandler : MouseAdapter() {
  private val window = JWindow()
  private val startPt = Point()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var gap: Component? = null
  private var index = -1

  override fun mousePressed(e: MouseEvent) {
    val parent = e.component as? Container ?: return
    if (parent.componentCount > 0) {
      startPt.location = e.point
      window.background = Color(0x0, true)
    }
  }

  private fun startDragging(
    parent: Container,
    pt: Point,
  ) {
    val c = parent.getComponentAt(pt)
    index = parent.getComponentZOrder(c)
    if (c == null || c == parent || index < 0) {
      return
    }
    draggingComponent = c
    gap = Box.createHorizontalStrut(c.width)
    swapComponent(parent, c, gap, index)
    window.add(c)
    window.pack()
    val d = c.preferredSize
    val p = Point(pt.x - d.width / 2, pt.y - d.height / 2)
    SwingUtilities.convertPointToScreen(p, parent)
    window.location = p
    window.isVisible = true
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val parent = e.component as? Container ?: return
    if (!window.isVisible || draggingComponent == null) {
      if (startPt.distance(pt) > gestureMotionThreshold) {
        startDragging(parent, pt)
      }
      return
    }
    val d = draggingComponent?.preferredSize ?: Dimension()
    val p = Point(pt.x - d.width / 2, pt.y - d.height / 2)
    SwingUtilities.convertPointToScreen(p, parent)
    window.location = p
    val idx = (0..<parent.componentCount)
      .map { getTargetIndex(parent, it, pt) }
      .firstOrNull { it >= 0 }
      ?: -1
    swapComponent(parent, gap, gap, idx)
  }

  override fun mouseReleased(e: MouseEvent) {
    val parent = e.component as? Container
    if (parent == null || !window.isVisible || draggingComponent == null) {
      return
    }
    window.isVisible = false
    val pt = e.point
    val max = parent.componentCount
    val cmp = draggingComponent
    draggingComponent = null

    val idx = (0..<max)
      .map { getTargetIndex(parent, it, pt) }
      .firstOrNull { it >= 0 }
      ?: if (parent.parent.bounds.contains(pt)) max else index
    swapComponent(parent, gap, cmp, idx)
  }

  private fun getTargetIndex(
    parent: Container,
    i: Int,
    pt: Point,
  ): Int {
    val c = parent.getComponent(i)
    val r = c.bounds
    val wd2 = r.width / 2
    PREV_AREA.setBounds(r.x, r.y, wd2, r.height)
    NEXT_AREA.setBounds(r.x + wd2, r.y, wd2, r.height)
    return when {
      PREV_AREA.contains(pt) -> if (i > 1) i else 0
      NEXT_AREA.contains(pt) -> i
      else -> -1
    }
  }


  private fun swapComponent(
    parent: Container,
    remove: Component?,
    add: Component?,
    idx: Int,
  ) {
    parent.remove(remove)
    if (idx >= 0 && add != null) {
      parent.add(add, idx)
    }
    parent.revalidate()
    parent.repaint()
  }

  companion object {
    private val PREV_AREA = Rectangle()
    private val NEXT_AREA = Rectangle()
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
