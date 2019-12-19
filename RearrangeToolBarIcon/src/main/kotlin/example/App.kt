package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val toolbar = JToolBar("ToolBarButton")
    toolbar.setFloatable(false)
    val dh = DragHandler()
    toolbar.addMouseListener(dh)
    toolbar.addMouseMotionListener(dh)
    toolbar.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 0))
    val list = listOf(
      "Copy24.gif", "Cut24.gif", "Paste24.gif",
      "Delete24.gif", "Undo24.gif", "Redo24.gif",
      "Help24.gif", "Open24.gif", "Save24.gif"
    )
    list.map { createToolBarButton(it) }.forEach { toolbar.add(it) }
    add(toolbar, BorderLayout.NORTH)
    add(JScrollPane(JTree()))
    setPreferredSize(Dimension(320, 240))
  }

  private fun createToolBarButton(name: String): Component {
    val b = JLabel(ImageIcon(javaClass.getResource(PATH + name)))
    b.setOpaque(false)
    return b
  }

  companion object {
    private const val PATH = "/toolbarButtonGraphics/general/"
  }
}

class DragHandler : MouseAdapter() {
  private val window = JWindow()
  private val gap = Box.createHorizontalStrut(24)
  private val startPt = Point()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var index = -1

  override fun mousePressed(e: MouseEvent) {
    val parent = e.getComponent() as? Container ?: return
    if (parent.getComponentCount() > 0) {
      startPt.setLocation(e.getPoint())
      window.setBackground(Color(0x0, true))
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
    val d = c.getPreferredSize()
    val p = Point(pt.x - d.width / 2, pt.y - d.height / 2)
    SwingUtilities.convertPointToScreen(p, parent)
    window.setLocation(p)
    window.setVisible(true)
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt: Point = e.getPoint()
    val parent = e.getComponent() as? Container ?: return
    if (!window.isVisible() || draggingComponent == null) {
      if (startPt.distance(pt) > gestureMotionThreshold) {
        startDragging(parent, pt)
      }
      return
    }
    val d = draggingComponent?.getPreferredSize() ?: Dimension()
    val p = Point(pt.x - d.width / 2, pt.y - d.height / 2)
    SwingUtilities.convertPointToScreen(p, parent)
    window.setLocation(p)
    for ((i, c) in parent.getComponents().withIndex()) {
      val r = c.getBounds()
      val wd2 = r.width / 2
      PREV_AREA.setBounds(r.x, r.y, wd2, r.height)
      NEXT_AREA.setBounds(r.x + wd2, r.y, wd2, r.height)
      if (PREV_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, gap, if (i > 1) i else 0)
        return
      } else if (NEXT_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, gap, i)
        return
      }
    }
    parent.remove(gap)
    parent.revalidate()
  }

  override fun mouseReleased(e: MouseEvent) {
    val parent = e.getComponent() as? Container
    if (parent == null || !window.isVisible() || draggingComponent == null) {
      return
    }
    window.setVisible(false)
    val pt = e.getPoint()
    val cmp = draggingComponent
    draggingComponent = null

    for ((i, c) in parent.getComponents().withIndex()) {
      val r: Rectangle = c.bounds
      val wd2 = r.width / 2
      PREV_AREA.setBounds(r.x, r.y, wd2, r.height)
      NEXT_AREA.setBounds(r.x + wd2, r.y, wd2, r.height)
      if (PREV_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, cmp, if (i > 1) i else 0)
        return
      } else if (NEXT_AREA.contains(pt)) {
        swapComponentLocation(parent, gap, cmp, i)
        return
      }
    }
    val idx = if (parent.getParent().getBounds().contains(pt)) parent.getComponentCount() else index
    swapComponentLocation(parent, gap, cmp, idx)
  }

  private fun swapComponentLocation(parent: Container, remove: Component, add: Component?, idx: Int) {
    if (add == null) {
      return
    }
    parent.remove(remove)
    parent.add(add, idx)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
