package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelListener
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.roundToInt

fun createUI(): Component {
  val slider0 = JSlider(0, 100, 50)
  slider0.border = BorderFactory.createTitledBorder("Default SnapToTicks")

  val slider1 = SnapToTicksDragSlider(0, 100, 50)
  slider1.border = BorderFactory.createTitledBorder("Custom SnapToTicks")
  val handler = MouseWheelListener { e ->
    (e.component as? JSlider)?.also {
      val hasMinorTick = it.minorTickSpacing > 0
      val tickSpacing = if (hasMinorTick) {
        it.minorTickSpacing
      } else {
        it.majorTickSpacing
      }
      it.value = it.value - e.wheelRotation * tickSpacing
    }
  }
  slider1.addMouseWheelListener(handler)
  slider1.inputMap.put(KeyStroke.getKeyStroke("RIGHT"), "RIGHT_ARROW")
  slider1.actionMap.put(
    "RIGHT_ARROW",
    object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.source as? JSlider ?: return
        s.value += s.majorTickSpacing
      }
    },
  )
  slider1.inputMap.put(KeyStroke.getKeyStroke("LEFT"), "LEFT_ARROW")
  slider1.actionMap.put(
    "LEFT_ARROW",
    object : AbstractAction() {
      override fun actionPerformed(e: ActionEvent) {
        val s = e.source as? JSlider ?: return
        s.value -= s.majorTickSpacing
      }
    },
  )

  val list = listOf(initSlider(slider0), initSlider(slider1))
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

private fun initSlider(slider: JSlider): JSlider {
  slider.majorTickSpacing = 10
  slider.snapToTicks = true
  slider.paintTicks = true
  slider.paintLabels = true
  return slider
}

// Snap the thumb to the nearest tick while dragging the mouse
private class SnapToTicksDragSlider(
  min: Int,
  max: Int,
  value: Int,
) : JSlider(min, max, value) {
  private val tickSpacing
    get() = if (minorTickSpacing > 0) minorTickSpacing else majorTickSpacing

  // BasicSliderUI.TrackListener#mouseDragged(...) moves the thumb to the raw
  // mouse location, and BasicSliderUI#calculateThumbLocation() that snaps the
  // value is only called while BasicSliderUI#isDragging() returns false.
  // Consuming MOUSE_PRESSED, MOUSE_DRAGGED and MOUSE_RELEASED keeps the
  // TrackListener from starting a thumb drag, so the LookAndFeel itself places
  // the thumb on the tick that matches the current value.
  override fun processMouseEvent(e: MouseEvent) {
    val id = e.id
    val isPressed = id == MouseEvent.MOUSE_PRESSED
    val isReleased = id == MouseEvent.MOUSE_RELEASED
    if ((isPressed || isReleased) && isSnapDragEvent(e)) {
      if (isPressed) {
        startSnapDrag(e.point)
      } else {
        valueIsAdjusting = false
      }
    } else {
      super.processMouseEvent(e)
    }
  }

  private fun startSnapDrag(pt: Point) {
    if (isRequestFocusEnabled) {
      requestFocusInWindow()
    }
    valueIsAdjusting = true
    value = getSnappedValue(pt)
  }

  override fun processMouseMotionEvent(e: MouseEvent) {
    if (e.id == MouseEvent.MOUSE_DRAGGED && isSnapDragEvent(e)) {
      value = getSnappedValue(e.point)
    } else {
      // MOUSE_MOVED is delegated to the TrackListener to keep the rollover state
      super.processMouseMotionEvent(e)
    }
  }

  private fun isSnapDragEvent(e: MouseEvent) =
    isEnabled && snapToTicks && tickSpacing > 0 &&
      ui is BasicSliderUI && SwingUtilities.isLeftMouseButton(e)

  private fun getSnappedValue(pt: Point): Int {
    val sliderUi = ui as? BasicSliderUI ?: return value
    val horizontal = orientation == HORIZONTAL
    val v = if (horizontal) {
      sliderUi.valueForXPosition(pt.x)
    } else {
      sliderUi.valueForYPosition(pt.y)
    }
    val ts = tickSpacing
    val snapped = minimum + ((v - minimum) / ts.toFloat()).roundToInt() * ts
    return snapped.coerceIn(minimum, maximum)
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
