package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

private const val KEY = "Slider.onlyLeftMouseButtonDrag"

fun makeUI(): Component {
  val slider1 = makeSlider()
  val slider2 = makeSlider()
  slider2.model = slider1.model
  setSliderUI(slider2)

  val ma = SliderPopupListener()
  slider2.addMouseMotionListener(ma)
  slider2.addMouseListener(ma)

  val p = JPanel(BorderLayout())
  p.add(Box.createVerticalBox().also {
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Default", slider1))
    it.add(Box.createVerticalStrut(25))
    it.add(makeTitledPanel("Show ToolTip", slider2))
    it.add(Box.createVerticalGlue())
  }, BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeSlider(): JSlider {
  val slider = JSlider(0, 100, 0)
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.paintTicks = true
  // slider.setPaintLabels(true)
  slider.addMouseWheelListener(SliderMouseWheelListener())
  return slider
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun setSliderUI(slider: JSlider) {
  if (slider.ui is WindowsSliderUI) {
    slider.ui = WindowsTooltipSliderUI(slider)
  } else {
    slider.ui = MetalTooltipSliderUI()
  }
}

private class WindowsTooltipSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean(KEY) && SwingUtilities.isLeftMouseButton(e)) {
          (e.component as? JSlider)?.also {
            if (it.orientation == SwingConstants.VERTICAL) {
              it.value = valueForYPosition(e.y)
            } else { // SwingConstants.HORIZONTAL
              it.value = valueForXPosition(e.x)
            }
            super.mousePressed(e) // isDragging = true
            super.mouseDragged(e)
          }
        } else {
          super.mousePressed(e)
        }
      }

      override fun shouldScroll(direction: Int) = false
    }
  }
}

private class MetalTooltipSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean(KEY) && SwingUtilities.isLeftMouseButton(e)) {
          (e.component as? JSlider)?.also {
            if (it.orientation == SwingConstants.VERTICAL) {
              it.value = valueForYPosition(e.y)
            } else { // SwingConstants.HORIZONTAL
              it.value = valueForXPosition(e.x)
            }
            super.mousePressed(e) // isDragging = true
            super.mouseDragged(e)
          }
        } else {
          super.mousePressed(e)
        }
      }

      override fun shouldScroll(direction: Int) = false
    }
  }
}

private class SliderPopupListener : MouseAdapter() {
  private val toolTip = JWindow()
  private val label = JLabel("", SwingConstants.CENTER)
  private val size = Dimension(30, 20)
  private var prevValue = -1

  init {
    label.isOpaque = false
    label.background = UIManager.getColor("ToolTip.background")
    label.border = UIManager.getBorder("ToolTip.border")
    toolTip.add(label)
    toolTip.size = size
  }

  private fun updateToolTip(e: MouseEvent) {
    val slider = e.component as? JSlider ?: return
    val intValue = slider.value
    if (prevValue != intValue) {
      label.text = "%03d".format(slider.value)
      val pt = e.point
      pt.y = -size.height
      SwingUtilities.convertPointToScreen(pt, e.component)
      pt.translate(-size.width / 2, 0)
      toolTip.location = pt
    }
    prevValue = intValue
  }

  override fun mouseDragged(e: MouseEvent) {
    updateToolTip(e)
  }

  override fun mousePressed(e: MouseEvent) {
    if (UIManager.getBoolean(KEY) && SwingUtilities.isLeftMouseButton(e)) {
      toolTip.isVisible = true
      updateToolTip(e)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    toolTip.isVisible = false
  }
}

private class SliderMouseWheelListener : MouseWheelListener {
  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val s = e.component as? JSlider ?: return
    s.value = s.value - e.wheelRotation
    // val i = s.getValue().toInt() - e.getWheelRotation()
    // val m = s.getModel()
    // s.setValue(minOf(maxOf(i, m.getMinimum()), m.getMaximum()))
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
