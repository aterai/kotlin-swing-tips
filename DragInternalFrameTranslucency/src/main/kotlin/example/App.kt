package example

import java.awt.*
import javax.swing.*

private var draggingFrame: JComponent? = null

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.desktopManager = object : DefaultDesktopManager() {
    override fun beginDraggingFrame(f: JComponent) {
      draggingFrame = f
      super.beginDraggingFrame(f)
    }

    override fun endDraggingFrame(f: JComponent) {
      draggingFrame = null
      super.endDraggingFrame(f)
      f.repaint()
    }

    override fun beginResizingFrame(f: JComponent, direction: Int) {
      draggingFrame = f
      desktop.dragMode = JDesktopPane.OUTLINE_DRAG_MODE
      super.beginResizingFrame(f, direction)
    }

    override fun endResizingFrame(f: JComponent) {
      draggingFrame = null
      desktop.dragMode = JDesktopPane.LIVE_DRAG_MODE
      super.endResizingFrame(f)
    }
  }

  val frame1 = createFrame("Frame1")
  desktop.add(frame1)
  frame1.setLocation(30, 10)
  frame1.isVisible = true

  val frame2 = createFrame("Frame2")
  desktop.add(frame2)
  frame2.setLocation(50, 30)
  frame2.isVisible = true

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(title: String): JInternalFrame {
  val frame = object : JInternalFrame(title, true, true, true, true) {
    override fun paintComponent(g: Graphics) {
      if (draggingFrame === this) {
        (g as? Graphics2D)?.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .2f)
      }
      super.paintComponent(g)
    }
  }
  frame.setSize(200, 100)
  return frame
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
