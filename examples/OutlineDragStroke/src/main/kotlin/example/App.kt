package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI

fun createUI(): Component {
  val rubberBand = Rectangle()
  val desktop = JDesktopPane()
  desktop.desktopManager = OutlineResizeDesktopManager(rubberBand)
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
    override fun paint(
      g: Graphics,
      c: JComponent,
    ) {
      super.paint(g, c)
      if (!rubberBand.isEmpty) {
        val g2 = g.create() as? Graphics2D ?: return
        // val paint = UIManager.getColor("InternalFrame.borderColor")
        g2.paint = Color.GRAY
        g2.stroke = createDotStroke()
        g2.draw(rubberBand)
        g2.dispose()
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

fun createDotStroke(): Stroke {
  val dash = floatArrayOf(1f, 1f)
  return BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, dash, 0f)
}

private class OutlineResizeDesktopManager(
  private val rubberBand: Rectangle,
) : DefaultDesktopManager() {
  override fun resizeFrame(
    f: JComponent,
    newX: Int,
    newY: Int,
    newWidth: Int,
    newHeight: Int,
  ) {
    // Do not call super.resizeFrame(...): the frame keeps its bounds while resizing,
    // and only the rubber band is updated and painted by the LayerUI.
    repaintRubberBand(f)
    rubberBand.setBounds(newX, newY, newWidth, newHeight)
    repaintRubberBand(f)
  }

  override fun endResizingFrame(f: JComponent) {
    if (!rubberBand.isEmpty) {
      repaintRubberBand(f)
      setBoundsForFrame(
        f,
        rubberBand.x,
        rubberBand.y,
        rubberBand.width,
        rubberBand.height,
      )
      rubberBand.setBounds(0, 0, 0, 0)
    }
    super.endResizingFrame(f)
  }

  private fun repaintRubberBand(f: JComponent) {
    val parent = f.parent
    if (parent != null && !rubberBand.isEmpty) {
      // Graphics2D#draw(Rectangle) paints one pixel beyond the width and height
      parent.repaint(
        rubberBand.x,
        rubberBand.y,
        rubberBand.width + 1,
        rubberBand.height + 1,
      )
    }
  }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
