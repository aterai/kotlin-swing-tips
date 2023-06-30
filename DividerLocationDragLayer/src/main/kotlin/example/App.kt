package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val splitPane = JSplitPane()
  splitPane.dividerSize = 1
  splitPane.resizeWeight = .5
  splitPane.leftComponent = makeTestBox()
  splitPane.rightComponent = makeTestBox()
  val check = JCheckBox("VERTICAL")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    splitPane.orientation = if (b) JSplitPane.VERTICAL_SPLIT else JSplitPane.HORIZONTAL_SPLIT
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JLayer(splitPane, DividerLocationDragLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTestBox(): Component {
  val tree = JTree()
  tree.visibleRowCount = 3

  val box = Box.createVerticalBox()
  box.add(JLabel("111111111111111111111111"))
  box.add(Box.createVerticalStrut(5))
  box.add(JCheckBox("222222222222"))
  box.add(Box.createVerticalStrut(5))
  box.add(JScrollPane(tree))
  box.add(Box.createVerticalStrut(5))
  box.add(JButton("33333"))
  box.add(Box.createVerticalGlue())
  box.border = BorderFactory.createEmptyBorder(2, 5, 2, 5)

  return JScrollPane(box).also {
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
    it.viewportBorder = BorderFactory.createEmptyBorder()
    it.border = BorderFactory.createEmptyBorder()
  }
}

private class DividerLocationDragLayerUI : LayerUI<JSplitPane>() {
  private var dividerLocation = 0
  private val startPt = Point()
  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JSplitPane>) {
    val sp = l.view
    val c = e.component
    if (isDraggableComponent(sp, c) && e.id == MouseEvent.MOUSE_PRESSED) {
      startPt.location = SwingUtilities.convertPoint(c, e.point, sp)
      dividerLocation = sp.dividerLocation
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JSplitPane>) {
    val sp = l.view
    val c = e.component
    if (isDraggableComponent(sp, c) && e.id == MouseEvent.MOUSE_DRAGGED) {
      val pt = SwingUtilities.convertPoint(c, e.point, sp)
      val delta = if (sp.orientation == JSplitPane.HORIZONTAL_SPLIT) {
        pt.x - startPt.x
      } else {
        pt.y - startPt.y
      }
      sp.dividerLocation = 0.coerceAtLeast(dividerLocation + delta)
    }
  }

  companion object {
    private fun isDraggableComponent(splitPane: JSplitPane, c: Component) =
      splitPane == c || splitPane == SwingUtilities.getUnwrappedParent(c)
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
