package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val rubberBand = Rectangle()
  val desktop = JDesktopPane()
  desktop.desktopManager = object : DefaultDesktopManager() {
    override fun beginResizingFrame(f: JComponent, direction: Int) {
      (f as? JInternalFrame)?.desktopPane?.dragMode = JDesktopPane.OUTLINE_DRAG_MODE
      super.beginResizingFrame(f, direction)
    }

    override fun resizeFrame(f: JComponent, newX: Int, newY: Int, newWidth: Int, newHeight: Int) {
      val d = (f as? JInternalFrame)?.desktopPane
      if (d?.dragMode == JDesktopPane.OUTLINE_DRAG_MODE) {
        super.resizeFrame(f, newX, newY, 0, 0)
        rubberBand.setBounds(newX, newY, newWidth, newHeight)
        d.repaint()
      } else {
        super.resizeFrame(f, newX, newY, newWidth, newHeight)
      }
    }

    override fun endResizingFrame(f: JComponent) {
      (f as? JInternalFrame)?.desktopPane?.dragMode = JDesktopPane.LIVE_DRAG_MODE
      if (!rubberBand.isEmpty) {
        super.resizeFrame(f, rubberBand.x, rubberBand.y, rubberBand.width, rubberBand.height)
        rubberBand.setBounds(0, 0, 0, 0)
      }
      super.endResizingFrame(f)
    }
  }
  val frame1 = createFrame("Frame1")
  desktop.add(frame1)
  frame1.setLocation(30, 10)

  val frame2 = createFrame("Frame2")
  desktop.add(frame2)
  frame2.setLocation(50, 30)

  EventQueue.invokeLater {
    frame1.isVisible = true
    frame2.isVisible = true
  }
  val layerUI = object : LayerUI<JDesktopPane>() {
    override fun paint(g: Graphics, c: JComponent) {
      super.paint(g, c)
      if (c is JLayer<*>) {
        val d = c.view as? JDesktopPane
        if (d?.dragMode == JDesktopPane.OUTLINE_DRAG_MODE) {
          val g2 = g.create() as? Graphics2D ?: return
          g2.paint = Color.GRAY
          g2.stroke = makeDotStroke()
          g2.draw(rubberBand)
          g2.dispose()
        }
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JLayer(desktop, layerUI))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(title: String): JInternalFrame {
  val frame = JInternalFrame(title, true, true, true, true)
  frame.setSize(200, 100)
  return frame
}

fun makeDotStroke(): Stroke {
  val dist = floatArrayOf(1f, 1f)
  return BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dist, 0f)
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
