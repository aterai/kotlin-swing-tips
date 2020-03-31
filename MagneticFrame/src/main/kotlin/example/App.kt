package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
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

  private fun getX(e: Int, w: Int) =
    if (e < w) if (isNear(e)) 0 else e else if (isNear(w)) w + e else e

  private fun getY(n: Int, s: Int) =
    if (n < s) if (isNear(n)) 0 else n else if (isNear(s)) s + n else n

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
