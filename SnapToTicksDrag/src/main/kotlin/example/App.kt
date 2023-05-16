package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI
import kotlin.math.roundToInt

fun makeUI(): Component {
  val slider = makeSlider("Custom SnapToTicks")
  initSlider(slider)

  val list = listOf(makeSlider("Default SnapToTicks"), slider)

  val check = JCheckBox("JSlider.setMinorTickSpacing(5)")
  check.addActionListener { e ->
    val mts = if ((e.source as? JCheckBox)?.isSelected == true) 5 else 0
    list.forEach { it.minorTickSpacing = mts }
  }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  for (s in list) {
    box.add(s)
    box.add(Box.createVerticalStrut(10))
  }
  box.add(check)
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(title: String) = JSlider(0, 100, 50).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.majorTickSpacing = 10
  it.snapToTicks = true
  it.paintTicks = true
  it.paintLabels = true
}

private fun initSlider(slider: JSlider) {
  slider.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0, false), "RIGHT_ARROW")
  val a1 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val s = e.source as? JSlider ?: return
      s.value = s.value + s.majorTickSpacing
    }
  }
  slider.actionMap.put("RIGHT_ARROW", a1)
  slider.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0, false), "LEFT_ARROW")
  val a2 = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val s = e.source as? JSlider ?: return
      s.value = s.value - s.majorTickSpacing
    }
  }
  slider.actionMap.put("LEFT_ARROW", a2)
  slider.addMouseWheelListener { e ->
    (e.component as? JSlider)?.also {
      val hasMinorTickSp = it.minorTickSpacing > 0
      val tickSpacing = if (hasMinorTickSp) it.minorTickSpacing else it.majorTickSpacing
      val v = it.value - e.wheelRotation * tickSpacing
      val m = it.model
      // it.value = minOf(m.maximum, maxOf(v, m.minimum))
      it.value = v.coerceIn(m.minimum, m.maximum)
    }
  }
  if (slider.ui is WindowsSliderUI) {
    slider.ui = WindowsSnapToTicksDragSliderUI(slider)
  } else {
    slider.ui = MetalSnapToTicksDragSliderUI()
  }
}

private class WindowsSnapToTicksDragSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) {
        if (!slider.snapToTicks || slider.majorTickSpacing == 0) {
          super.mouseDragged(e)
          return
        }
        // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val trackLength = trackRect.width
        val trackLeft = trackRect.x - halfThumbWidth
        val trackRight = trackRect.x + trackRect.width - 1 + halfThumbWidth
        val pos = e.x
        val snappedPos = when {
          pos <= trackLeft -> trackLeft
          pos >= trackRight -> trackRight
          else -> getSnappedPos(trackLength, pos, trackLeft)
        }
        e.translatePoint(snappedPos - pos, 0)
        super.mouseDragged(e)
      }

      private fun getSnappedPos(trackLength: Int, pos: Int, trackLeft: Int): Int {
        offset = 0
        val possibleTickPos = slider.maximum - slider.minimum
        val hasMinorTick = slider.minorTickSpacing > 0
        val tickSpacing = if (hasMinorTick) slider.minorTickSpacing else slider.majorTickSpacing
        val tickPixels = trackLength * tickSpacing / possibleTickPos.toFloat()
        val px = pos - trackLeft
        return ((px / tickPixels).roundToInt() * tickPixels).roundToInt() + trackLeft
      }
    }
  }
}

private class MetalSnapToTicksDragSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider): TrackListener {
    return object : TrackListener() {
      override fun mouseDragged(e: MouseEvent) {
        if (!slider.snapToTicks || slider.majorTickSpacing == 0) {
          super.mouseDragged(e)
          return
        }
        // case HORIZONTAL:
        val halfThumbWidth = thumbRect.width / 2
        val trackLength = trackRect.width
        val trackLeft = trackRect.x - halfThumbWidth
        val trackRight = trackRect.x + trackRect.width - 1 + halfThumbWidth
        val pos = e.x
        val snappedPos = when {
          pos <= trackLeft -> trackLeft
          pos >= trackRight -> trackRight
          else -> getSnappedPos(trackLength, pos, trackLeft)
        }
        e.translatePoint(snappedPos - pos, 0)
        super.mouseDragged(e)
      }

      private fun getSnappedPos(trackLength: Int, pos: Int, trackLeft: Int): Int {
        offset = 0
        val possibleTickPos = slider.maximum - slider.minimum
        val hasMinorTick = slider.minorTickSpacing > 0
        val tickSpacing = if (hasMinorTick) slider.minorTickSpacing else slider.majorTickSpacing
        val tickPixels = trackLength * tickSpacing / possibleTickPos.toFloat()
        val px = pos - trackLeft
        return ((px / tickPixels).roundToInt() * tickPixels).roundToInt() + trackLeft
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
