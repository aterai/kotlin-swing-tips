package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val box1 = makeTestBox()
  box1.border = BorderFactory.createTitledBorder("DragScrollListener")
  val l = DragScrollListener()
  box1.addMouseListener(l)
  box1.addMouseMotionListener(l)

  val box2 = makeTestBox()
  box2.border = BorderFactory.createTitledBorder("DragScrollLayerUI")

  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(JScrollPane(box1))
    it.add(JLayer(JScrollPane(box2), DragScrollLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestBox(): Box {
  val tab1 = JTabbedPane()
  tab1.addTab("aaa", JLabel("11111111111"))
  tab1.addTab("bbb", JCheckBox("2222222222"))

  val tab2 = JTabbedPane()
  tab2.addTab("ccc cc", JLabel("3333"))
  tab2.addTab("ddd dd", JLabel("444444444444"))

  val tree = JTree()
  tree.visibleRowCount = 5

  val box = Box.createVerticalBox()
  box.add(JLabel("aaa aaa aaa aaa aaa aaa"))
  box.add(Box.createVerticalStrut(5))
  box.add(tab1)
  box.add(Box.createVerticalStrut(5))
  box.add(JCheckBox("bbb bbb bbb bbb"))
  box.add(Box.createVerticalStrut(5))
  box.add(tab2)
  box.add(Box.createVerticalStrut(5))
  box.add(JSlider(0, 100, 50))
  box.add(Box.createVerticalStrut(5))
  box.add(JScrollPane(tree))
  box.add(Box.createVerticalStrut(5))
  box.add(JButton("ccc ccc"))
  box.add(Box.createVerticalGlue())
  return box
}

private class DragScrollListener : MouseInputAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      val cp = SwingUtilities.convertPoint(c, e.point, it)
      val vp = it.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, it.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      pp.location = SwingUtilities.convertPoint(c, e.point, it)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
  }
}

private class DragScrollLayerUI : LayerUI<JScrollPane>() {
  private val pp = Point()
  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    if (c is JScrollBar || c is JSlider) {
      return
    }
    if (e.id == MouseEvent.MOUSE_PRESSED) {
      pp.location = SwingUtilities.convertPoint(c, e.point, l.view.viewport)
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    if (c is JScrollBar || c is JSlider) {
      return
    }
    if (e.id == MouseEvent.MOUSE_DRAGGED) {
      val viewPort = l.view.viewport
      val cp = SwingUtilities.convertPoint(c, e.point, viewPort)
      val vp = viewPort.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (viewPort.view as? JComponent)?.scrollRectToVisible(Rectangle(vp, viewPort.size))
      pp.location = cp
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
