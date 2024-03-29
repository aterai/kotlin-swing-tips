package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val tabs = JTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT

  val t0 = JToolBar("Default")
  tabs.addTab(t0.name, makePanel(t0))

  val t1 = object : JToolBar("Override createDockingListener()") {
    override fun updateUI() {
      super.updateUI()
      setUI(object : BasicToolBarUI() {
        override fun createDockingListener(): MouseInputListener {
          val listener = super.createDockingListener()
          return DockingListener2(toolBar, listener)
        }
      })
    }
  }
  tabs.addTab(t1.name, makePanel(t1))

  val t2 = JToolBar("DisableRightButtonDraggedOut")
  val l2 = DisableRightButtonDragOutLayerUI()
  tabs.addTab(t2.name, JLayer(makePanel(t2), l2))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initToolBar(toolBar: JToolBar): JToolBar {
  toolBar.add(JLabel(toolBar.name))
  toolBar.add(Box.createRigidArea(Dimension(5, 5)))
  toolBar.add(JButton("JButton"))
  toolBar.add(JCheckBox("JCheckBox"))
  toolBar.add(Box.createGlue())
  val popup = JPopupMenu()
  popup.add("Item 1")
  popup.add("Item 2")
  popup.add("Item 3")
  toolBar.componentPopupMenu = popup
  return toolBar
}

private fun makePanel(toolBar: JToolBar): JPanel {
  val p = JPanel(BorderLayout())
  p.add(initToolBar(toolBar), BorderLayout.NORTH)
  p.add(JScrollPane(JTree()))
  return p
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

private class DisableRightButtonDragOutLayerUI : LayerUI<Container>() {
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

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out Container>,
  ) {
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
