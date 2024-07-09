package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import kotlin.math.roundToInt

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
  private var index = -1
  private var draggingComponent: Component? = null
  private var gap: Component? = null

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
    swapComponentLocation(parent, c, gap, index)
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

  private fun getTargetIndex(
    r: Rectangle,
    pt: Point,
    i: Int,
  ): Int {
    val ht2 = (r.height / 2f).roundToInt()
    PREV_AREA.setBounds(r.x, r.y, r.width, ht2)
    NEXT_AREA.setBounds(r.x, r.y + ht2, r.width, ht2)
    return when {
      PREV_AREA.contains(pt) ->
        i
          .also {
            prevRect.bounds = PREV_AREA
          }.takeIf { it > 1 } ?: 0

      NEXT_AREA.contains(pt) -> i.also {
        prevRect.bounds = NEXT_AREA
      }

      else -> -1
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    (e.component as? Container)?.also { parent ->
      if (draggingComponent == null) {
        if (startPt.distance(pt) > gestureMotionThreshold) {
          startDragging(parent, pt)
        }
        return
      }
      updateWindowLocation(pt, parent)
      if (!prevRect.contains(pt) && !searchAndSwap(parent, gap, pt)) {
        parent.remove(gap)
        parent.revalidate()
      }
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    dragOffset.setLocation(0, 0)
    prevRect.setBounds(0, 0, 0, 0)
    window.isVisible = false

    val pt = e.point
    (e.component as? Container)?.also { parent ->
      val c = draggingComponent
      draggingComponent = null
      if (!searchAndSwap(parent, c, pt)) {
        val i = if (parent.parent.bounds.contains(pt)) parent.componentCount else index
        swapComponentLocation(parent, gap, c, i)
      }
    }
  }

  private fun searchAndSwap(
    parent: Container,
    cmp: Component?,
    pt: Point,
  ): Boolean {
    var find = false
    for ((i, c) in parent.components.withIndex()) {
      val r = c.bounds
      if (c == gap && r.contains(pt)) {
        swapComponentLocation(parent, gap, cmp, i)
        return true
      }
      val tgt = getTargetIndex(r, pt, i)
      if (tgt >= 0) {
        swapComponentLocation(parent, gap, cmp, tgt)
        find = true
        break
      }
    }
    return find
  }

  private fun swapComponentLocation(
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
