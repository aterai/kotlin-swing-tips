package example

import java.awt.*
import javax.swing.*
import kotlin.math.abs

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.background = Color.GRAY.brighter()
  desktop.dragMode = JDesktopPane.OUTLINE_DRAG_MODE
  desktop.desktopManager = MagneticDesktopManager()

  val magneticFrame1 = createFrame("Frame1")
  desktop.add(magneticFrame1)
  magneticFrame1.setLocation(30, 10)
  magneticFrame1.isVisible = true

  val magneticFrame2 = createFrame("Frame2")
  desktop.add(magneticFrame2)
  magneticFrame2.setLocation(50, 30)
  magneticFrame2.isVisible = true

  desktop.preferredSize = Dimension(320, 240)
  return desktop
}

private fun createFrame(title: String): JInternalFrame {
  val frame = JInternalFrame(title, false, false, true, true)
  frame.setSize(200, 100)
  return frame
}

private class MagneticDesktopManager : DefaultDesktopManager() {
  override fun dragFrame(frame: JComponent, x: Int, y: Int) {
    val desktop = SwingUtilities.getAncestorOfClass(JDesktopPane::class.java, frame)
    if (desktop is JDesktopPane) {
      val w = desktop.size.width - frame.size.width - x
      val s = desktop.size.height - frame.size.height - y
      @Suppress("ComplexCondition")
      if (isNear(x) || isNear(y) || isNear(w) || isNear(s)) {
        super.dragFrame(frame, getX(x, w), getY(y, s))
      } else {
        super.dragFrame(frame, x, y)
      }
    }
  }

  private fun getX(east: Int, west: Int) =
    if (east < west) {
      if (isNear(east)) 0 else east
    } else {
      if (isNear(west)) west + east else east
    }

  private fun getY(north: Int, south: Int) =
    if (north < south) {
      if (isNear(north)) 0 else north
    } else {
      if (isNear(south)) south + north else north
    }

  private fun isNear(c: Int) = abs(c) < 10
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
