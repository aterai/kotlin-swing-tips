package example

import java.awt.*
import java.awt.dnd.DragSource
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun createUI(): Component {
  val box = Box.createVerticalBox()
  val dh = RearrangingHandler()
  box.addMouseListener(dh)
  box.addMouseMotionListener(dh)
  listOf(
    JLabel("<html>1<br>11<br>111"),
    JButton("22"),
    JCheckBox("333"),
    JScrollPane(JTextArea(4, 12)),
  ).forEach { box.add(createSortablePanel(it)) }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createSortablePanel(c: Component): Component {
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
  return p
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

private class RearrangingHandler : MouseAdapter() {
  private val prevRect = Rectangle()
  private val dragThreshold = DragSource.getDragThreshold()
  private val window = JWindow()
  private val startPt = Point()
  private val dragOffset = Point()
  private var isDragging = false
  private var draggingComponent: Component? = null
  private var gap: Component? = null

  override fun mousePressed(e: MouseEvent) {
    startPt.location = e.point
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val parent = e.component as? Container ?: return
    if (isDragging) {
      updateWindowLocation(pt, parent)
      // keep the gap while the pointer stays in the half checked last time
      if (!prevRect.contains(pt)) {
        updateGapLocation(parent, pt)
      }
    } else if (startPt.distance(pt) > dragThreshold) {
      startDragging(parent, pt)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    // a click without dragging does nothing
    val parent = e.component as? Container
    if (isDragging && parent != null) {
      window.isVisible = false
      // replace the temporary gap with the dragging panel
      val idx = parent.getComponentZOrder(gap)
      replaceComponent(parent, gap, draggingComponent, idx)
      isDragging = false
    }
  }

  private fun startDragging(
    parent: Container,
    pt: Point,
  ) {
    // drag the panel under the pressed point, not the one under the current point
    val c = parent.getComponentAt(startPt) ?: return
    val index = parent.getComponentZOrder(c)
    if (index >= 0) {
      isDragging = true
      draggingComponent = c
      prevRect.setBounds(0, 0, 0, 0)

      val r = c.bounds
      dragOffset.setLocation(startPt.x - r.x, startPt.y - r.y)

      gap = Box.createRigidArea(r.size)
      replaceComponent(parent, c, gap, index)

      val gc = window.graphicsConfiguration
      if (gc?.isTranslucencyCapable == true) {
        window.background = Color(0x0, true)
      }
      window.add(c)
      window.pack()

      // set the location before the window is shown
      updateWindowLocation(pt, parent)
      window.isVisible = true
    }
  }

  private fun updateWindowLocation(
    pt: Point,
    parent: Component,
  ) {
    val p = Point(pt.x - dragOffset.x, pt.y - dragOffset.y)
    SwingUtilities.convertPointToScreen(p, parent)
    window.location = p
  }

  private fun updateGapLocation(
    parent: Container,
    pt: Point,
  ) {
    val c = parent.getComponentAt(pt) ?: return
    val idx = parent.getComponentZOrder(c)
    if (idx >= 0 && c != gap) {
      // remember the half of the panel under the pointer
      // to avoid moving the gap back and forth after the layout changes
      val r = c.bounds
      val half = Math.round(r.height / 2f)
      val y = if (pt.y < r.y + half) r.y else r.y + half
      prevRect.setBounds(r.x, y, r.width, half)
      // move the temporary gap to the location of the panel
      replaceComponent(parent, gap, gap, idx)
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
