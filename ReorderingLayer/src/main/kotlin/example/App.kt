// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.dnd.DragSource
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createMatteBorder(10, 8, 5, 1, Color.RED))
    for ((idx, c) in listOf<Component>(
      JLabel("<html>111<br>11<br>11"), JButton("2"), JCheckBox("3"), JTextField(14)
    ).withIndex()) {
      box.add(createToolBarButton(idx, c))
    }
    add(JLayer(box, ReorderingLayerUI<JComponent>()), BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun createToolBarButton(i: Int, c: Component): Component {
    val l = JLabel(String.format(" %04d ", i))
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
    return p
  }
}

class ReorderingLayerUI<V : JComponent> : LayerUI<V>() {
  private val prevRect = Rectangle()
  private val draggingRect = Rectangle()
  private val startPt = Point()
  private val dragOffset = Point()
  private val rubberStamp = JPanel()
  private val gestureMotionThreshold = DragSource.getDragThreshold()
  private var draggingComponent: Component? = null
  private var gap: Component? = null
  private var index = -1

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*> && draggingComponent != null) {
      SwingUtilities.paintComponent(g, draggingComponent, rubberStamp, draggingRect)
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
      MouseEvent.MOUSE_RELEASED -> {
        val pt = e.getPoint()
        val cmp = draggingComponent
        draggingComponent = null
        // swap the dragging panel and the dummy filler
        for ((i, c) in parent.getComponents().withIndex()) {
          if (c == gap) {
            replaceComponent(parent, gap, cmp, i)
            return
          }
          val tgt = getTargetIndex(c.getBounds(), pt, i)
          if (tgt >= 0) {
            replaceComponent(parent, gap, cmp, tgt)
            return
          }
        }
        val idx = if (parent.getParent().getBounds().contains(pt)) parent.getComponentCount() else index
        replaceComponent(parent, gap, cmp, idx)
        l.repaint()
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out V>) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED) {
      val pt = e.getPoint()
      val parent = l.getView()
      if (draggingComponent == null) {
        // MotionThreshold
        if (startPt.distance(pt) > gestureMotionThreshold) {
          startDragging(parent, pt)
        }
        return
      }

      // update the cursor window location
      updateWindowLocation(pt, parent)
      l.repaint()
      if (prevRect.contains(pt)) {
        return
      }

      // change the dummy filler location
      for ((i, c) in parent.getComponents().withIndex()) {
        val r = c.getBounds()
        if (c == gap && r.contains(pt)) {
          return
        }
        val tgt = getTargetIndex(r, pt, i)
        if (tgt >= 0) {
          replaceComponent(parent, gap, gap, tgt)
          return
        }
      }
      parent.revalidate()
      l.repaint()
    }
  }

  private fun startDragging(parent: JComponent, pt: Point) {
    val c = parent.getComponentAt(pt)
    index = parent.getComponentZOrder(c)
    if (c == parent || index < 0) {
      return
    }
    draggingComponent = c
    val r = c.getBounds()
    draggingRect.setBounds(r) // save draggingComponent size

    dragOffset.setLocation(pt.x - r.x, pt.y - r.y)
    gap = Box.createRigidArea(r.getSize())
    replaceComponent(parent, c, gap, index)
    updateWindowLocation(pt, parent)
  }

  private fun updateWindowLocation(pt: Point, parent: JComponent) {
    val i = parent.getInsets()
    val r = SwingUtilities.calculateInnerArea(parent, R3)
    val x = r.x
    val y = pt.y - dragOffset.y
    val h = draggingRect.height
    val yy = if (y < i.top) i.top else if (r.contains(x, y + h)) y else r.height + i.top - h
    draggingRect.setLocation(x, yy)
  }

  private fun getTargetIndex(r: Rectangle, pt: Point, i: Int): Int {
    val ht2 = (.5 + r.height * .5).toInt()
    R1.setBounds(r.x, r.y, r.width, ht2)
    R2.setBounds(r.x, r.y + ht2, r.width, ht2)
    return when {
      R1.contains(pt) -> {
        prevRect.setBounds(R1)
        if (i > 1) i else 0
      }
      R2.contains(pt) -> {
        prevRect.setBounds(R2)
        i
      }
      else -> -1
    }
  }

  private fun replaceComponent(parent: Container, remove: Component?, insert: Component?, idx: Int) {
    if (insert == null) {
      return
    }
    parent.remove(remove)
    parent.add(insert, idx)
    parent.revalidate()
    parent.repaint()
  }

  companion object {
    private val R1 = Rectangle()
    private val R2 = Rectangle()
    private val R3 = Rectangle()
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
