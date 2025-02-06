package example

import com.sun.java.swing.plaf.windows.WindowsInternalFrameUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.JInternalFrame.JDesktopIcon
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicInternalFrameTitlePane
import javax.swing.plaf.basic.BasicInternalFrameUI

fun makeUI(): Component {
  val desktop0 = makeDesktopPane()
  desktop0.add(createFrame("Default", 0))

  val desktop1 = makeDesktopPane()
  val f = createFrame("JPopupMenu", 0)
  val popup = InternalFrameTitlePanePopupMenu()
  (f.ui as? BasicInternalFrameUI)?.northPane?.componentPopupMenu = popup
  desktop1.add(f)

  val desktop2 = makeDesktopPane()
  desktop2.add(createFrame2())

  val desktop3 = makeDesktopPane()
  desktop3.add(createFrame("JDesktopPane", 1))
  desktop3.add(createFrame("JLayer", 0))

  val tabs = JTabbedPane()
  tabs.add("Default", desktop0)
  tabs.add("JPopupMenu", desktop1)
  tabs.add("WindowsInternalFrameUI", desktop2)
  tabs.add("JLayer", JLayer(desktop3, DesktopLayerUI()))
  // tabs.setComponentPopupMenu(TabbedPanePopupMenu())
  return JPanel(GridLayout(0, 1)).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeDesktopPane() = JDesktopPane().also {
  it.background = Color.LIGHT_GRAY
}

private fun createFrame(
  t: String,
  i: Int,
): JInternalFrame {
  val f = JInternalFrame(t, true, true, true, true)
  f.setSize(200, 100)
  f.setLocation(5 + 40 * i, 5 + 50 * i)
  EventQueue.invokeLater { f.isVisible = true }
  return f
}

private fun createFrame2(): JInternalFrame {
  val f = object : JInternalFrame("WindowsInternalFrameUI", true, true, true, true) {
    override fun updateUI() {
      super.updateUI()
      setUI(object : WindowsInternalFrameUI(this) {
        override fun createBorderListener(
          w: JInternalFrame,
        ) = object : BorderListener() {
          override fun mouseClicked(e: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(e)) {
              super.mouseClicked(e)
            }
          }

          override fun mousePressed(e: MouseEvent) {
            if (SwingUtilities.isLeftMouseButton(e)) {
              super.mousePressed(e)
            }
          }
        }
      })
    }
  }
  f.setSize(200, 100)
  f.setLocation(5 + 40, 5 + 50)
  EventQueue.invokeLater { f.isVisible = true }
  return f
}

private class InternalFrameTitlePanePopupMenu : JPopupMenu() {
  init {
    add("test1")
    add("test2")
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is BasicInternalFrameTitlePane) {
      super.show(c, x, y)
    }
  }
}

private class DesktopLayerUI : LayerUI<JDesktopPane>() {
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

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JDesktopPane>,
  ) {
    if (SwingUtilities.isRightMouseButton(e) && e.clickCount >= 2) {
      val c = e.component
      val clz = BasicInternalFrameTitlePane::class.java
      val p = SwingUtilities.getAncestorOfClass(clz, c)
      val id = e.id
      val b1 = c is BasicInternalFrameTitlePane || p is BasicInternalFrameTitlePane
      val b2 = c is JDesktopIcon && id == MouseEvent.MOUSE_PRESSED
      if (b1 && id == MouseEvent.MOUSE_CLICKED || b2) {
        e.consume()
      }
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out JDesktopPane>,
  ) {
    val b = e.component is JInternalFrame
    val isRight = SwingUtilities.isRightMouseButton(e)
    if (b && isRight && e.id == MouseEvent.MOUSE_DRAGGED) {
      e.consume()
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
