package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI

private const val MAXI = 80
private const val MINI = 40

fun makeUI(): Component {
  val slider1 = makeSlider("ChangeListener")
  val slider2 = makeSlider("TrackListener")
  if (slider2.ui is WindowsSliderUI) {
    slider2.setUI(WindowsDragLimitedSliderUI(slider2))
  } else {
    slider2.setUI(BasicDragLimitedSliderUI(slider2))
  }
  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.add(slider1)
    it.add(slider2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(title: String): JSlider {
  val slider = JSlider(0, 100, 40)
  slider.border = BorderFactory.createTitledBorder(title)
  slider.majorTickSpacing = 10
  slider.paintTicks = true
  slider.paintLabels = true
  // slider.labelTable?.elements()?.toList()
  //   ?.filterIsInstance<JLabel>()
  //   ?.filterNot { it.text.toInt() in MINI..MAXI }
  //   ?.forEach { it.foreground = Color.RED }
  (slider.labelTable as? Map<*, *>)?.forEach { (_, value) ->
    if (value is JLabel && value.text.toInt() !in MINI..MAXI) {
      value.foreground = Color.RED
    }
  }
  slider.model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      it.value = it.value.coerceIn(MINI, MAXI)
    }
  }
  return slider
}

private class WindowsDragLimitedSliderUI(
  slider: JSlider,
) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider) = object : TrackListener() {
    override fun mouseDragged(e: MouseEvent) { // case HORIZONTAL:
      val halfThumbWidth = thumbRect.width / 2
      val thumbLeft = e.x - offset
      val maxPos = xPositionForValue(MAXI) - halfThumbWidth
      val minPos = xPositionForValue(MINI) - halfThumbWidth
      if (thumbLeft > maxPos) {
        e.translatePoint(maxPos + offset - e.x, 0)
      } else if (thumbLeft < minPos) {
        e.translatePoint(minPos + offset - e.x, 0)
      }
      super.mouseDragged(e)
    }
  }
}

private class BasicDragLimitedSliderUI(
  slider: JSlider,
) : BasicSliderUI(slider) {
  override fun createTrackListener(slider: JSlider) = object : TrackListener() {
    override fun mouseDragged(e: MouseEvent) { // case HORIZONTAL:
      val halfThumbWidth = thumbRect.width / 2
      val thumbLeft = e.x - offset
      val maxPos = xPositionForValue(MAXI) - halfThumbWidth
      val minPos = xPositionForValue(MINI) - halfThumbWidth
      if (thumbLeft > maxPos) {
        e.translatePoint(maxPos + offset - e.x, 0)
      } else if (thumbLeft < minPos) {
        e.translatePoint(minPos + offset - e.x, 0)
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
