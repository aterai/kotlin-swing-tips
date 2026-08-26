package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI

private const val LOWER_LIMIT = 40
private const val UPPER_LIMIT = 80

fun createUI(): Component {
  val slider1 = JSlider(0, 100, LOWER_LIMIT)
  initSlider(slider1)
  val title1 = "ChangeListener"
  slider1.setBorder(BorderFactory.createTitledBorder(title1))

  val slider2 = DragLimitedSlider(0, 100, LOWER_LIMIT, LOWER_LIMIT, UPPER_LIMIT)
  initSlider(slider2)
  val title2 = "ChangeListener + DragLimitedSlider"
  slider2.setBorder(BorderFactory.createTitledBorder(title2))

  return JPanel(GridLayout(2, 1, 5, 5)).also {
    it.add(slider1)
    it.add(slider2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initSlider(slider: JSlider) {
  slider.majorTickSpacing = 10
  slider.paintTicks = true
  slider.paintLabels = true
  val labelTable = slider.labelTable
  if (labelTable is MutableMap<*, *>) {
    labelTable.forEach { (_: Any?, value: Any?) ->
      if (value is JLabel) {
        highlightOutOfRange(value)
      }
    }
  }
  slider.model.addChangeListener { e ->
    (e.source as? BoundedRangeModel)?.also {
      it.value = it.value.coerceIn(LOWER_LIMIT, UPPER_LIMIT)
    }
  }
}

private fun highlightOutOfRange(label: JLabel) {
  val value = label.text.toInt()
  if (value !in LOWER_LIMIT..UPPER_LIMIT) {
    label.setForeground(Color.RED)
  }
}

private class DragLimitedSlider(
  min: Int,
  max: Int,
  value: Int,
  private val lowerDragLimit: Int,
  private val upperDragLimit: Int,
) : JSlider(min, max, value) {
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

  override fun processMouseMotionEvent(e: MouseEvent) {
    if (e.getID() == MouseEvent.MOUSE_DRAGGED && isThumbDragEvent(e)) {
      setValue(getLimitedValue(e.getPoint()))
    } else {
      super.processMouseMotionEvent(e)
    }
  }

  private fun startThumbDrag(e: MouseEvent) {
    if (isRequestFocusEnabled) {
      requestFocusInWindow()
    }
    setValueIsAdjusting(true)
    setValue(getLimitedValue(e.getPoint()))
  }

  private fun isThumbDragEvent(e: MouseEvent): Boolean =
    isEnabled && getUI() is BasicSliderUI &&
      SwingUtilities.isLeftMouseButton(e)

  private fun getLimitedValue(pt: Point): Int {
    val ui = getUI() as BasicSliderUI
    val horizontal = getOrientation() == HORIZONTAL
    val value = if (horizontal) {
      ui.valueForXPosition(pt.x)
    } else {
      ui.valueForYPosition(pt.y)
    }
    return value.coerceIn(lowerDragLimit, upperDragLimit)
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
