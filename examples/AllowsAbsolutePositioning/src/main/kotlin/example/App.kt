package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicScrollBarUI

fun makeUI(): Component {
  UIManager.put("ScrollBar.allowsAbsolutePositioning", true)
  val h1 = "middle mouse click in the track will set"
  val h2 = " the position of the track to where the mouse is.\n"
  val help = h1 + h2
  val txt = help.repeat(100)

  val scroll = JScrollPane(JTextArea("override TrackListener#mousePressed(...)\n$txt"))
  scroll.verticalScrollBar = object : JScrollBar(Adjustable.VERTICAL) {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsScrollBarUI) {
        AbsolutePositioningWindowsScrollBarUI()
      } else {
        AbsolutePositioningBasicScrollBarUI()
      }
      setUI(tmp)
      putClientProperty("JScrollBar.fastWheelScrolling", true)
    }
  }

  scroll.horizontalScrollBar = object : JScrollBar(Adjustable.HORIZONTAL) {
    override fun updateUI() {
      super.updateUI()
      val tmp = if (ui is WindowsScrollBarUI) {
        AbsolutePositioningWindowsScrollBarUI()
      } else {
        AbsolutePositioningBasicScrollBarUI()
      }
      setUI(tmp)
      putClientProperty("JScrollBar.fastWheelScrolling", true)
    }
  }

  val p = JPanel(GridLayout(1, 2))
  p.add(JScrollPane(JTextArea(txt)))
  p.add(scroll)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JLabel("ScrollBar.allowsAbsolutePositioning: true"), BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private class AbsolutePositioningWindowsScrollBarUI : WindowsScrollBarUI() {
  override fun createTrackListener() = object : BasicScrollBarUI.TrackListener() {
    override fun mousePressed(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        super.mousePressed(
          MouseEvent(
            e.component,
            e.id,
            e.getWhen(),
            InputEvent.BUTTON2_DOWN_MASK xor InputEvent.BUTTON2_MASK,
            e.x,
            e.y,
            e.xOnScreen,
            e.yOnScreen,
            e.clickCount,
            e.isPopupTrigger,
            MouseEvent.BUTTON2,
          ),
        )
      } else {
        super.mousePressed(e)
      }
    }
  }
}

private class AbsolutePositioningBasicScrollBarUI : BasicScrollBarUI() {
  override fun createTrackListener() = object : BasicScrollBarUI.TrackListener() {
    override fun mousePressed(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e)) {
        super.mousePressed(
          MouseEvent(
            e.component,
            e.id,
            e.getWhen(),
            InputEvent.BUTTON2_DOWN_MASK xor InputEvent.BUTTON2_MASK,
            e.x,
            e.y,
            e.xOnScreen,
            e.yOnScreen,
            e.clickCount,
            e.isPopupTrigger,
            MouseEvent.BUTTON2,
          ),
        )
      } else {
        super.mousePressed(e)
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
