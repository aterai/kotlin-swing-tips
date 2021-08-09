package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val PATH = "toolBarButtonGraphics/general/"

fun makeUI(): Component {
  val toolBar = JToolBar("ToolBarButton")
  toolBar.isFloatable = false
  val dh = DragHandler()
  toolBar.addMouseListener(dh)
  toolBar.addMouseMotionListener(dh)
  toolBar.border = BorderFactory.createEmptyBorder(2, 2, 2, 0)
  val list = listOf(
    "Copy24.gif", "Cut24.gif", "Paste24.gif",
    "Delete24.gif", "Undo24.gif", "Redo24.gif",
    "Help24.gif", "Open24.gif", "Save24.gif"
  )
  list.map { createToolBarButton(it) }.forEach { toolBar.add(it) }

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(JScrollPane(JTree()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToolBarButton(name: String): Component {
  val cl = Thread.currentThread().contextClassLoader
  val b = JLabel(ImageIcon(cl.getResource(PATH + name)))
  b.isOpaque = false
  return b
}

private class DragHandler : MouseAdapter() {
  private val window = JWindow()
  private val gap = Box.createHorizontalStrut(24)
  private val startPt = Point()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var index = -1

  override fun mousePressed(e: MouseEvent) {
    val parent = e.component as? Container ?: return
    if (parent.componentCount > 0) {
      startPt.location = e.point
      window.background = Color(0x0, true)
    }
  }

  private fun startDragging(parent: Container, pt: Point) {
    val c = parent.getComponentAt(pt)
    index = parent.getComponentZOrder(c)
    if (c == null || c == parent || index < 0) {
      return
    }
    draggingComponent = c
    swapComponentLocation(parent, c, gap, index)
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
    if (!searchAndSwap(parent, gap, pt)) {
      parent.remove(gap)
      parent.revalidate()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val parent = e.component as? Container
    if (parent == null || !window.isVisible || draggingComponent == null) {
      return
    }
    window.isVisible = false
    val pt = e.point
    val cmp = draggingComponent
    draggingComponent = null

    if (!searchAndSwap(parent, cmp, pt)) {
      val idx = if (parent.parent.bounds.contains(pt)) parent.componentCount else index
      swapComponentLocation(parent, gap, cmp, idx)
    }
  }

  private fun searchAndSwap(parent: Container, cmp: Component?, pt: Point): Boolean {
    var find = false
    for ((i, c) in parent.components.withIndex()) {
      val r = c.bounds
      val wd2 = r.width / 2
      PREV_AREA.setBounds(r.x, r.y, wd2, r.height)
      NEXT_AREA.setBounds(r.x + wd2, r.y, wd2, r.height)
      if (PREV_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, cmp, if (i > 1) i else 0)
        return true
      } else if (NEXT_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, cmp, i)
        find = true
        break
      }
    }
    return find
  }

  private fun swapComponentLocation(parent: Container, remove: Component, insert: Component?, idx: Int) {
    if (insert == null) {
      return
    }
    parent.remove(remove)
    parent.add(insert, idx)
    parent.revalidate()
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
