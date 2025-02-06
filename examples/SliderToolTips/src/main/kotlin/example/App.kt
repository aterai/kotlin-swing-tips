package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.metal.MetalSliderUI

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default", makeSlider(JSlider(0, 100, 0))))
  box.add(Box.createVerticalStrut(25))

  val slider = object : JSlider(0, 100, 0) {
    private var handler: MouseAdapter? = null

    override fun updateUI() {
      removeMouseMotionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      val ui2 = if (ui is WindowsSliderUI) {
        WindowsTooltipSliderUI(this)
      } else {
        MetalTooltipSliderUI()
      }
      setUI(ui2)
      handler = SliderPopupListener()
      addMouseMotionListener(handler)
      addMouseListener(handler)
    }
  }
  box.add(makeTitledPanel("Show ToolTip", makeSlider(slider)))
  box.add(Box.createVerticalGlue())

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSlider(slider: JSlider): JSlider {
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

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class WindowsTooltipSliderUI(
  slider: JSlider,
) : WindowsSliderUI(slider) {
  override fun createTrackListener(slider: JSlider?) = object : TrackListener() {
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

private class MetalTooltipSliderUI : MetalSliderUI() {
  override fun createTrackListener(slider: JSlider?) = object : TrackListener() {
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
