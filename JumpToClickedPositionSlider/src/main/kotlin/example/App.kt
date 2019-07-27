package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val slider1 = JSlider(SwingConstants.VERTICAL, 0, 1000, 500)
    setSilderUI(slider1)

    val slider2 = JSlider(0, 1000, 500)
    setSilderUI(slider2)

    add(Box.createHorizontalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))
      it.add(JSlider(SwingConstants.VERTICAL, 0, 1000, 100))
      it.add(Box.createHorizontalStrut(20))
      it.add(slider1)
      it.add(Box.createHorizontalGlue())
    }, BorderLayout.WEST)

    add(Box.createVerticalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 20))
      it.add(makeTitledPanel("Default", JSlider(0, 100, 100)))
      it.add(Box.createVerticalStrut(20))
      it.add(makeTitledPanel("Jump to clicked position", slider2))
      it.add(Box.createVerticalGlue())
    })
    // setBorder(BorderFactory.createEmptyBorder(5, 20, 5, 10));
    setPreferredSize(Dimension(320, 240))
  }

  private fun setSilderUI(slider: JSlider) {
    if (slider.getUI() is WindowsSliderUI) {
      slider.setUI(WindowsJumpToClickedPositionSliderUI(slider))
    } else {
      slider.setUI(MetalJumpToClickedPositionSliderUI())
    }
    // slider.setSnapToTicks(false);
    // slider.setPaintTicks(true);
    // slider.setPaintLabels(true);
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(c)
  }
}

internal class WindowsJumpToClickedPositionSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  protected override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && SwingUtilities.isLeftMouseButton(e)) {
          val s = e.getComponent() as? JSlider ?: return@mousePressed
          when (s.getOrientation()) {
            SwingConstants.VERTICAL -> s.setValue(valueForYPosition(e.getY()))
            SwingConstants.HORIZONTAL -> s.setValue(valueForXPosition(e.getX()))
            else -> error("orientation must be one of: VERTICAL, HORIZONTAL")
          }
          super.mousePressed(e) // isDragging = true;
          super.mouseDragged(e)
        } else {
          super.mousePressed(e)
        }
      }

      override fun shouldScroll(direction: Int) = false
    }
  }
}

internal class MetalJumpToClickedPositionSliderUI : MetalSliderUI() {
  protected override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && SwingUtilities.isLeftMouseButton(e)) {
          val s = e.getComponent() as? JSlider ?: return@mousePressed
          when (s.getOrientation()) {
            SwingConstants.VERTICAL -> s.setValue(valueForYPosition(e.getY()))
            SwingConstants.HORIZONTAL -> s.setValue(valueForXPosition(e.getX()))
            else -> error("orientation must be one of: VERTICAL, HORIZONTAL")
          }
          super.mousePressed(e) // isDragging = true;
          super.mouseDragged(e)
        } else {
          super.mousePressed(e)
        }
      }

      override fun shouldScroll(direction: Int) = false
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
