package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI

fun createUI(): Component {
  val slider1 = JumpToClickedPositionSlider(SwingConstants.VERTICAL, 0, 1000, 500)
  val box1 = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.add(JSlider(SwingConstants.VERTICAL, 0, 1000, 100))
    it.add(Box.createHorizontalStrut(20))
    it.add(slider1)
    it.add(Box.createHorizontalGlue())
  }

  val slider2 = JumpToClickedPositionSlider(SwingConstants.HORIZONTAL, 0, 1000, 500)
  val box2 = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(20, 0, 20, 20)
    it.add(createTitledPanel("Default", JSlider(0, 100, 100)))
    it.add(Box.createVerticalStrut(20))
    it.add(createTitledPanel("Jump to clicked position", slider2))
    it.add(Box.createVerticalGlue())
  }

  return JPanel(BorderLayout()).also {
    it.add(box1, BorderLayout.WEST)
    it.add(box2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class JumpToClickedPositionSlider(
  orientation: Int,
  min: Int,
  max: Int,
  value: Int,
) : JSlider(orientation, min, max, value) {
  override fun processMouseEvent(e: MouseEvent) {
    if (e.getID() == MouseEvent.MOUSE_PRESSED && isJumpEvent(e)) {
      setValue(getValueForPoint(e.getPoint()))
    }
    super.processMouseEvent(e)
  }

  private fun isJumpEvent(e: MouseEvent) = isEnabled && getUI() is BasicSliderUI &&
    SwingUtilities.isLeftMouseButton(e)

  private fun getValueForPoint(pt: Point): Int {
    val ui = getUI() as? BasicSliderUI ?: return -1
    val horizontal = getOrientation() == HORIZONTAL
    return if (horizontal) ui.valueForXPosition(pt.x) else ui.valueForYPosition(pt.y)
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
