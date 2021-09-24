package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

fun makeUI(): Component {
  val slider1 = makeSlider()
  val slider2 = makeSlider()
  slider2.model = slider1.model
  setSliderUI(slider2)

  val ma = SliderPopupListener()
  slider2.addMouseMotionListener(ma)
  slider2.addMouseListener(ma)

  val box = Box.createVerticalBox().also {
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("Default", slider1))
    it.add(Box.createVerticalStrut(25))
    it.add(makeTitledPanel("Show ToolTip", slider2))
    it.add(Box.createVerticalGlue())
  }

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeSlider(): JSlider {
  val slider = JSlider(0, 100, 0)
  slider.paintTicks = true
  slider.majorTickSpacing = 10
  slider.minorTickSpacing = 5
  slider.addMouseWheelListener { e ->
    (e.component as? JSlider)?.also {
      it.value -= e.wheelRotation
    }
  }
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
        if (SwingUtilities.isLeftMouseButton(e)) {
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
        if (SwingUtilities.isLeftMouseButton(e)) {
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
  private val label = object : JLabel(" ", SwingConstants.CENTER) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 32
      return d
    }
  }
  private var prevValue = -1

  init {
    label.isOpaque = true
    label.background = UIManager.getColor("ToolTip.background")
    label.border = UIManager.getBorder("ToolTip.border")
    toolTip.add(label)
    toolTip.pack()
  }

  private fun updateToolTip(e: MouseEvent) {
    val slider = e.component as? JSlider ?: return
    val intValue = slider.value
    if (prevValue != intValue) {
      label.text = "%03d".format(slider.value)
      val pt = e.point
      pt.y = SwingUtilities.calculateInnerArea(slider, null).centerY.toInt()
      SwingUtilities.convertPointToScreen(pt, e.component)
      val h2 = slider.preferredSize.height / 2
      val d = label.preferredSize
      pt.translate(-d.width / 2, -d.height - h2)
      toolTip.location = pt
    }
    prevValue = intValue
  }

  override fun mouseDragged(e: MouseEvent) {
    updateToolTip(e)
  }

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      toolTip.isVisible = true
      updateToolTip(e)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    toolTip.isVisible = false
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
