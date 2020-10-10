package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentEvent
import java.awt.event.ComponentListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel().also {
  it.preferredSize = Dimension(320, 100)
}

private class DockingListener(private val frame1: JFrame, f2: JFrame) : ComponentListener {
  private val frame2: JFrame

  init {
    frame1.addComponentListener(this)
    frame2 = f2
    frame2.addComponentListener(this)
  }

  override fun componentResized(e: ComponentEvent) {
    positionFrames(e)
  }

  override fun componentMoved(e: ComponentEvent) {
    positionFrames(e)
  }

  override fun componentShown(e: ComponentEvent) {
    positionFrames(e)
  }

  override fun componentHidden(e: ComponentEvent) {
    positionFrames(e)
  }

  private fun positionFrames(e: ComponentEvent) {
    if (e.component == frame1) {
      val x = frame1.bounds.x // + frame1.getBounds().width;
      val y = frame1.bounds.y + frame1.bounds.height
      frame2.removeComponentListener(this)
      frame2.setLocation(x, y)
      frame2.addComponentListener(this)
    } else {
      val x = frame2.bounds.x // - frame1.getBounds().width;
      val y = frame2.bounds.y - frame1.bounds.height
      frame1.removeComponentListener(this)
      frame1.setLocation(x, y)
      frame1.addComponentListener(this)
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
    val frame1 = JFrame("main frame")
    frame1.contentPane.add(makeUI())
    frame1.defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    frame1.pack()
    frame1.setLocationRelativeTo(null)

    val frame2 = JFrame("sub frame")
    frame2.contentPane.add(makeUI())
    frame2.pack()
    DockingListener(frame1, frame2)
    frame1.isVisible = true
    frame2.isVisible = true
  }
}
