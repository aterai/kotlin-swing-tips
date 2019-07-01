package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI

class MainPanel : JPanel(BorderLayout(5, 5)) {
  init {
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/plaf/synth/doc-files/componentProperties.html
    UIManager.put("ScrollBar.allowsAbsolutePositioning", java.lang.Boolean.TRUE)

    val help = "middle mouse click in the track will set the position of the track to where the mouse is.\n"
    val txt = help.repeat(100)

    val scroll = JScrollPane(JTextArea("override TrackListener#mousePressed(...)\n$txt"))
    scroll.setVerticalScrollBar(object : JScrollBar(Adjustable.VERTICAL) {
      override fun updateUI() {
        super.updateUI()
        if (getUI() is WindowsScrollBarUI) {
          setUI(AbsolutePositioningWindowsScrollBarUI())
        } else {
          setUI(AbsolutePositioningBasicScrollBarUI())
        }
        putClientProperty("JScrollBar.fastWheelScrolling", true)
      }
    })

    scroll.setHorizontalScrollBar(object : JScrollBar(Adjustable.HORIZONTAL) {
      override fun updateUI() {
        super.updateUI()
        if (getUI() is WindowsScrollBarUI) {
          setUI(AbsolutePositioningWindowsScrollBarUI())
        } else {
          setUI(AbsolutePositioningBasicScrollBarUI())
        }
        putClientProperty("JScrollBar.fastWheelScrolling", true)
      }
    })

    val p = JPanel(GridLayout(1, 2))
    p.add(JScrollPane(JTextArea(txt)))
    p.add(scroll)

    add(JLabel("ScrollBar.allowsAbsolutePositioning: true"), BorderLayout.NORTH)
    add(p)
    setPreferredSize(Dimension(320, 240))
  }
}

class AbsolutePositioningWindowsScrollBarUI : WindowsScrollBarUI() {
  protected override fun createTrackListener(): BasicScrollBarUI.TrackListener {
    return object : BasicScrollBarUI.TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          super.mousePressed(MouseEvent(
            e.getComponent(), e.getID(), e.getWhen(),
            InputEvent.BUTTON2_DOWN_MASK xor InputEvent.BUTTON2_MASK, // e.getModifiers(),
            e.getX(), e.getY(),
            e.getXOnScreen(), e.getYOnScreen(),
            e.getClickCount(),
            e.isPopupTrigger(),
            MouseEvent.BUTTON2))
        } else {
          super.mousePressed(e)
        }
      }
    }
  }
}

class AbsolutePositioningBasicScrollBarUI : BasicScrollBarUI() {
  protected override fun createTrackListener(): BasicScrollBarUI.TrackListener {
    return object : BasicScrollBarUI.TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          super.mousePressed(MouseEvent(
            e.getComponent(), e.getID(), e.getWhen(),
            InputEvent.BUTTON2_DOWN_MASK xor InputEvent.BUTTON2_MASK,
            e.getX(), e.getY(),
            e.getXOnScreen(), e.getYOnScreen(),
            e.getClickCount(),
            e.isPopupTrigger(),
            MouseEvent.BUTTON2))
        } else {
          super.mousePressed(e)
        }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
