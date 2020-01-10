package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

class MainPanel : JPanel(GridLayout(2, 1, 5, 5)) {
  init {
    val slider1 = makeSlider("ChangeListener")
    val slider2 = makeSlider("TrackListener")
    if (slider2.ui is WindowsSliderUI) {
      slider2.ui = WindowsDragLimitedSliderUI(slider2)
    } else {
      slider2.ui = MetalDragLimitedSliderUI()
    }
    add(slider1)
    add(slider2)
    preferredSize = Dimension(320, 240)
  }

  private fun makeSlider(title: String): JSlider {
    val slider = JSlider(0, 100, 40)
    slider.border = BorderFactory.createTitledBorder(title)
    slider.majorTickSpacing = 10
    slider.paintTicks = true
    slider.paintLabels = true
    slider.labelTable?.elements()
      ?.toList()
      ?.filterIsInstance<JLabel>()
      ?.forEach {
      val v = it.text.toInt()
      if (v > MAXI || v < MINI) {
        it.foreground = Color.RED
      }
    }
    slider.model.addChangeListener { e ->
      val m = e.source as? BoundedRangeModel ?: return@addChangeListener
      if (m.value > MAXI) {
        m.value = MAXI
      } else if (m.value < MINI) {
        m.value = MINI
      }
    }
    return slider
  }

  companion object {
    const val MAXI = 80
    const val MINI = 40
  }
}

class WindowsDragLimitedSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) { // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val thumbLeft = e.x - offset
        val maxPos = xPositionForValue(MainPanel.MAXI) - halfThumbWidth
        val minPos = xPositionForValue(MainPanel.MINI) - halfThumbWidth
        if (thumbLeft > maxPos) {
          e.translatePoint(maxPos + offset - e.x, 0)
        } else if (thumbLeft < minPos) {
          e.translatePoint(minPos + offset - e.x, 0)
        }
        super.mouseDragged(e)
      }
    }
  }
}

class MetalDragLimitedSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) { // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val thumbLeft = e.x - offset
        val maxPos = xPositionForValue(MainPanel.MAXI) - halfThumbWidth
        val minPos = xPositionForValue(MainPanel.MINI) - halfThumbWidth
        if (thumbLeft > maxPos) {
          e.translatePoint(maxPos + offset - e.x, 0)
        } else if (thumbLeft < minPos) {
          e.translatePoint(minPos + offset - e.x, 0)
        }
        super.mouseDragged(e)
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
