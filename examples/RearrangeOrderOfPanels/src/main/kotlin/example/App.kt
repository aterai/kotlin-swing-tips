package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  val dh = RearrangingHandler()
  box.addMouseListener(dh)
  box.addMouseMotionListener(dh)
  val list = listOf<Component>(
    JLabel("<html>1<br>11<br>111"),
    JButton("22"),
    JCheckBox("333"),
    JScrollPane(JTextArea(4, 12)),
  )
  for ((i, c) in list.withIndex()) {
    box.add(createSortablePanel(i, c))
  }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSortablePanel(
  i: Int,
  c: Component,
): Component {
  val l = JLabel(" %04d ".format(i))
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
  return p
}

private class RearrangingHandler : MouseAdapter() {
  private val prevRect = Rectangle()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private val window = JWindow()
  private val startPt = Point()
  private val dragOffset = Point()
  private var draggingComponent: Component? = null
  private var gap: Component? = null
  private var index = -1

  override fun mousePressed(e: MouseEvent) {
    val c = e.component as? Container ?: return
    if (c.componentCount > 0) {
      startPt.location = e.point
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
    val d = c.size
    val dp = c.location
    dragOffset.setLocation(pt.x - dp.x, pt.y - dp.y)
    gap = Box.createRigidArea(d)
    swapComponent(parent, c, gap, index)
    window.background = Color(0x0, true)
    window.add(draggingComponent)
    // window.setSize(d)
    window.pack()
    updateWindowLocation(pt, parent)
    window.isVisible = true
  }

  private fun updateWindowLocation(
    pt: Point,
    parent: Component,
  ) {
    if (window.isVisible && draggingComponent != null) {
      val p = Point(pt.x - dragOffset.x, pt.y - dragOffset.y)
      SwingUtilities.convertPointToScreen(p, parent)
      window.location = p
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    (e.component as? Container)?.also { p ->
      if (draggingComponent == null) {
        if (startPt.distance(pt) > gestureMotionThreshold) {
          startDragging(p, pt)
        }
        return
      }
      updateWindowLocation(pt, p)
      if (!prevRect.contains(pt)) {
        val idx = (0..<p.componentCount)
          .filter { i ->
            p.getComponent(i).let {
              it !== gap || it.bounds.contains(pt)
            }
          }.map {
            getTargetIndex(p, it, pt)
          }.firstOrNull {
            it >= 0
          } ?: -1
        swapComponent(p, gap, gap, idx)
      }
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    dragOffset.setLocation(0, 0)
    prevRect.setBounds(0, 0, 0, 0)
    window.isVisible = false
    (e.component as? Container)?.also { parent ->
      val pt = e.point
      val max = parent.componentCount
      val c = draggingComponent
      draggingComponent = null
      val idx = (0..<max)
        .map { getTargetIndex(parent, it, pt) }
        .firstOrNull { it >= 0 }
        ?: if (parent.parent.bounds.contains(pt)) max else index
      swapComponent(parent, gap, c, idx)
    }
  }

  private fun getTargetIndex(
    parent: Container,
    i: Int,
    pt: Point,
  ): Int {
    val c = parent.getComponent(i)
    val r = c.bounds
    val ht2 = r.height / 2
    PREV.setBounds(r.x, r.y, r.width, ht2)
    NEXT.setBounds(r.x, r.y + ht2, r.width, ht2)
    return when {
      PREV.contains(pt) -> i.also { prevRect.bounds = PREV }.takeIf { it > 1 } ?: 0
      NEXT.contains(pt) -> i.also { prevRect.bounds = NEXT }
      else -> -1
    }
  }

  private fun swapComponent(
    parent: Container,
    remove: Component?,
    insert: Component?,
    idx: Int,
  ) {
    parent.remove(remove)
    if (idx >= 0 && insert != null) {
      parent.add(insert, idx)
    }
    parent.revalidate()
    parent.repaint()
  }

  companion object {
    private val PREV = Rectangle()
    private val NEXT = Rectangle()
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
