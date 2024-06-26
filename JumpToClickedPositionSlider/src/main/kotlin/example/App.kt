package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.metal.MetalSliderUI

fun makeUI(): Component {
  val slider1 = makeSlider(SwingConstants.VERTICAL, 0, 1000, 500)
  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.add(JSlider(SwingConstants.VERTICAL, 0, 1000, 100))
    it.add(Box.createHorizontalStrut(20))
    it.add(slider1)
    it.add(Box.createHorizontalGlue())
  }

  val slider2 = makeSlider(SwingConstants.HORIZONTAL, 0, 1000, 500)
  val box2 = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 0, 20, 20)
    it.add(makeTitledPanel("Default", JSlider(0, 100, 100)))
    it.add(Box.createVerticalStrut(20))
    it.add(makeTitledPanel("Jump to clicked position", slider2))
    it.add(Box.createVerticalGlue())
  }

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.WEST)
    it.add(box2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(
  orientation: Int,
  min: Int,
  max: Int,
  value: Int,
): JSlider {
  return object : JSlider(orientation, min, max, value) {
    override fun updateUI() {
      super.updateUI()
      val ui2 = if (ui is WindowsSliderUI) {
        WindowsJumpToClickedPositionSliderUI(this)
      } else {
        val icon = UIManager.getIcon("html.missingImage")
        UIManager.put("Slider.trackWidth", 0) // Meaningless settings that are not used?
        UIManager.put("Slider.majorTickLength", 0) // BasicSliderUI#getTickLength(): 8
        UIManager.put("Slider.verticalThumbIcon", icon)
        UIManager.put("Slider.horizontalThumbIcon", icon)
        MetalJumpToClickedPositionSliderUI()
      }
      setUI(ui2)
    }
  }
  // slider.setSnapToTicks(false)
  // slider.setPaintTicks(true)
  // slider.setPaintLabels(true)
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class WindowsJumpToClickedPositionSliderUI(
  slider: JSlider,
) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          val s = e.component as? JSlider ?: return
          when (s.orientation) {
            SwingConstants.VERTICAL -> s.value = valueForYPosition(e.y)
            SwingConstants.HORIZONTAL -> s.value = valueForXPosition(e.x)
            else -> error("orientation must be one of: VERTICAL, HORIZONTAL")
          }
          super.mousePressed(e) // isDragging = true
          super.mouseDragged(e)
        } else {
          super.mousePressed(e)
        }
      }

      override fun shouldScroll(direction: Int) = false
    }
  }
}

private class MetalJumpToClickedPositionSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isLeftMouseButton(e)) {
          val s = e.component as? JSlider ?: return
          when (s.orientation) {
            SwingConstants.VERTICAL -> s.value = valueForYPosition(e.y)
            SwingConstants.HORIZONTAL -> s.value = valueForXPosition(e.x)
            else -> error("orientation must be one of: VERTICAL, HORIZONTAL")
          }
          super.mousePressed(e) // isDragging = true
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
