package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.abs
import kotlin.math.max

fun createUI(): Component {
  val slider0 = JSlider(-100, 100, 0)
  initSlider(slider0)
  slider0.border = BorderFactory.createTitledBorder("Default")

  val slider1 = ZoomLevelsSlider(-100, 100, 0)
  initSlider(slider1)
  val help1 = "Dragged: Snap to the center"
  val help2 = "Double-clicked: Reset to the initial value"
  slider1.setBorder(BorderFactory.createTitledBorder("<html>$help1<br>$help2"))

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
  slider.labelTable = labelTable
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

private class ZoomLevelsSlider(
  min: Int,
  max: Int,
  private val defaultValue: Int,
) : JSlider(min, max, defaultValue) {
  override fun processMouseEvent(e: MouseEvent) {
    val id = e.getID()
    val isPressed = id == MouseEvent.MOUSE_PRESSED
    val isReleased = id == MouseEvent.MOUSE_RELEASED
    if ((isPressed || isReleased) && isThumbDragEvent(e)) {
      if (isPressed) {
        startThumbDrag(e)
      } else {
        setValueIsAdjusting(false)
      }
    } else {
      super.processMouseEvent(e)
    }
  }

  private fun startThumbDrag(e: MouseEvent) {
    if (isRequestFocusEnabled) {
      requestFocusInWindow()
    }
    setValueIsAdjusting(true)
    val isDoubleClick = e.getClickCount() >= 2
    setValue(if (isDoubleClick) defaultValue else getSnappedValue(e.getPoint()))
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED && isThumbDragEvent(e)) {
      setValue(getSnappedValue(e.getPoint()))
    } else {
      super.processMouseMotionEvent(e)
    }
  }

  private fun isThumbDragEvent(e: MouseEvent) =
    isEnabled && getUI() is BasicSliderUI &&
      SwingUtilities.isLeftMouseButton(e)

  private fun getSnappedValue(pt: Point): Int {
    val ui = getUI() as BasicSliderUI
    val horizontal = getOrientation() == HORIZONTAL
    val value = if (horizontal) {
      ui.valueForXPosition(pt.x)
    } else {
      ui.valueForYPosition(pt.y)
    }
    val tickSpacing = max(getMajorTickSpacing(), MIN_TICK_SPACING)
    val nearDefaultValue = abs(value - defaultValue) < tickSpacing / 2
    return if (nearDefaultValue) defaultValue else value
  }

  companion object {
    private const val MIN_TICK_SPACING = 10
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
