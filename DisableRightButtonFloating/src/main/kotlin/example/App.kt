package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val toolBar = makeToolBar("Override createDockingListener()")
  toolBar.ui = object : BasicToolBarUI() {
    override fun createDockingListener() = DockingListener2(
      toolBar,
      super.createDockingListener(),
    )
  }

  val p = JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
    it.add(toolBar, BorderLayout.NORTH)
    it.add(makeToolBar("DisableRightButtonDraggedOut"), BorderLayout.SOUTH)
    it.add(makeToolBar("Default"), BorderLayout.WEST)
  }
  return JPanel(BorderLayout()).also {
    it.add(JLayer(p, DisableRightButtonDragOutLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeToolBar(title: String): JToolBar {
  val toolBar = JToolBar(title)
  toolBar.add(JLabel(title))
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(JButton("Button"))
  toolBar.add(Box.createGlue())
  val popup = JPopupMenu()
  popup.add("Item 1")
  popup.add("Item 2")
  popup.add("Item 3")
  toolBar.componentPopupMenu = popup
  return toolBar
}

private class DockingListener2(
  private val toolBar: JToolBar,
  private val listener: MouseInputListener,
) : MouseInputAdapter() {
  private fun cancelDrag(e: MouseEvent) =
    !toolBar.isEnabled || !SwingUtilities.isLeftMouseButton(e)

  override fun mousePressed(e: MouseEvent) {
    if (cancelDrag(e)) {
      return
    }
    listener.mousePressed(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    if (cancelDrag(e)) {
      return
    }
    listener.mouseDragged(e)
  }

  override fun mouseReleased(e: MouseEvent) {
    if (cancelDrag(e)) {
      return
    }
    listener.mouseReleased(e)
  }
}

private class DisableRightButtonDragOutLayerUI : LayerUI<JPanel>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
    super.uninstallUI(c)
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JPanel>) {
    val c = e.component
    if (c is JToolBar) {
      val dragEvent = e.id == MouseEvent.MOUSE_DRAGGED
      val leftButton = SwingUtilities.isLeftMouseButton(e)
      val checkName = "DisableRightButtonDraggedOut" == c.getName()
      if (dragEvent && !leftButton && checkName) {
        e.consume()
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
