package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.metal.MetalSliderUI

fun makeUI(): Component {
  val slider0 = JSlider(-100, 100, 0)
  initSlider(slider0)
  slider0.border = BorderFactory.createTitledBorder("Default")

  val slider1 = object : JSlider(-100, 100, 0) {
    override fun updateUI() {
      super.updateUI()
      if (getUI() is WindowsSliderUI) {
        setUI(WindowsZoomLevelsSliderUI(this))
      } else {
        val icon = UIManager.getIcon("html.missingImage")
        UIManager.put("Slider.trackWidth", 0) // Meaningless settings that are not used?
        UIManager.put("Slider.majorTickLength", 8) // BasicSliderUI#getTickLength(): 8
        UIManager.put("Slider.verticalThumbIcon", icon)
        UIManager.put("Slider.horizontalThumbIcon", icon)
        setUI(MetalZoomLevelsSliderUI())
      }
    }
  }
  initSlider(slider1)
  val help1 = "Dragged: Snap to the center"
  val help2 = "Clicked: Double-click the thumb to reset its value"
  slider1.border = BorderFactory.createTitledBorder("<html>$help1<br>$help2")

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  box.add(slider0)
  box.add(Box.createVerticalStrut(20))
  box.add(slider1)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initSlider(slider: JSlider) {
  slider.majorTickSpacing = 20
  slider.paintTicks = true
  slider.paintLabels = true
  val labelTable = slider.labelTable
  if (labelTable is Map<*, *>) {
    labelTable.forEach { key, value ->
      if (key is Int && value is JLabel) {
        value.text = getLabel(key, slider)
      }
    }
  }
  slider.labelTable = labelTable // Update LabelTable
}

private fun getLabel(
  key: Int,
  slider: JSlider,
) = when (key) {
  0 -> "100%"
  slider.minimum -> "5%"
  slider.maximum -> "800%"
  else -> " "
}

private class WindowsZoomLevelsSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider) = object : TrackListener() {
    override fun mouseClicked(e: MouseEvent) {
      super.mouseClicked(e)
      val isLeftDoubleClick = SwingUtilities.isLeftMouseButton(e) && e.clickCount >= 2
      if (isLeftDoubleClick && thumbRect.contains(e.point)) {
        slider.value = 0
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      // case HORIZONTAL:
      val halfThumbWidth = thumbRect.width / 2
      val trackLength = trackRect.width
      val pos = e.x + halfThumbWidth
      val possibleTickPos = slider.maximum - slider.minimum
      val tickSp = slider.majorTickSpacing.coerceAtLeast(10)
      val tickPixels = trackLength * tickSp / possibleTickPos
      val tickPixels2 = tickPixels / 2
      val trackCenter = trackRect.centerX.toInt()
      if (trackCenter - tickPixels2 < pos && pos < trackCenter + tickPixels2) {
        e.translatePoint(trackCenter - halfThumbWidth - e.x, 0)
        offset = 0
      }
      super.mouseDragged(e)
    }
  }
}

private class MetalZoomLevelsSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider) = object : TrackListener() {
    override fun mouseClicked(e: MouseEvent) {
      val isLeftDoubleClick = SwingUtilities.isLeftMouseButton(e) && e.clickCount >= 2
      if (isLeftDoubleClick && thumbRect.contains(e.point)) {
        slider.value = 0
      } else {
        super.mouseClicked(e)
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      // case HORIZONTAL:
      val halfThumbWidth = thumbRect.width / 2
      val trackLength = trackRect.width
      val pos = e.x + halfThumbWidth
      val possibleTickPos = slider.maximum - slider.minimum
      val tickSp = slider.majorTickSpacing.coerceAtLeast(10)
      val tickPixels = trackLength * tickSp / possibleTickPos
      val trackCenter = trackRect.centerX.toInt()
      if (trackCenter - tickPixels < pos && pos < trackCenter + tickPixels) {
        e.translatePoint(trackCenter - halfThumbWidth - e.x, 0)
        offset = 0
      }
      super.mouseDragged(e)
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
