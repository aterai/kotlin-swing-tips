package example

import com.sun.java.swing.plaf.windows.WindowsSliderUI

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent
import java.awt.event.MouseWheelListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.metal.MetalSliderUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val slider1 = makeSlider()
    val slider2 = makeSlider()
    slider2.setModel(slider1.getModel())
    setSilderUI(slider2)

    val ma = SliderPopupListener()
    slider2.addMouseMotionListener(ma)
    slider2.addMouseListener(ma)

    add(Box.createVerticalBox().also {
      it.add(Box.createVerticalStrut(5))
      it.add(makeTitledPanel("Default", slider1))
      it.add(Box.createVerticalStrut(25))
      it.add(makeTitledPanel("Show ToolTip", slider2))
      it.add(Box.createVerticalGlue())
    }, BorderLayout.NORTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeSlider(): JSlider {
    val slider = JSlider(0, 100, 0)
    slider.setMajorTickSpacing(10)
    slider.setMinorTickSpacing(5)
    slider.setPaintTicks(true)
    // slider.setPaintLabels(true)
    slider.addMouseWheelListener(SliderMouseWheelListener())
    return slider
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }

  private fun setSilderUI(slider: JSlider) {
    if (slider.getUI() is WindowsSliderUI) {
      slider.setUI(WindowsTooltipSliderUI(slider))
    } else {
      slider.setUI(MetalTooltipSliderUI())
    }
  }
}

internal class WindowsTooltipSliderUI(slider: JSlider) : WindowsSliderUI(slider) {
  protected override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && SwingUtilities.isLeftMouseButton(e)) {
          (e.getComponent() as? JSlider)?.also {
            if (it.getOrientation() == SwingConstants.VERTICAL) {
              it.setValue(valueForYPosition(e.getY()))
            } else { // SwingConstants.HORIZONTAL
              it.setValue(valueForXPosition(e.getX()))
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

internal class MetalTooltipSliderUI : MetalSliderUI() {
  protected override fun createTrackListener(slider: JSlider?): TrackListener {
    return object : TrackListener() {
      override fun mousePressed(e: MouseEvent) {
        if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && SwingUtilities.isLeftMouseButton(e)) {
          (e.getComponent() as? JSlider)?.also {
            if (it.getOrientation() == SwingConstants.VERTICAL) {
              it.setValue(valueForYPosition(e.getY()))
            } else { // SwingConstants.HORIZONTAL
              it.setValue(valueForXPosition(e.getX()))
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

internal class SliderPopupListener : MouseAdapter() {
  private val toolTip = JWindow()
  private val label = JLabel("", SwingConstants.CENTER)
  private val size = Dimension(30, 20)
  private var prevValue = -1

  init {
    label.setOpaque(false)
    label.setBackground(UIManager.getColor("ToolTip.background"))
    label.setBorder(UIManager.getBorder("ToolTip.border"))
    toolTip.add(label)
    toolTip.setSize(size)
  }

  protected fun updateToolTip(e: MouseEvent) {
    val slider = e.getComponent() as? JSlider ?: return
    val intValue = slider.getValue().toInt()
    if (prevValue != intValue) {
      label.setText(String.format("%03d", slider.getValue()))
      val pt = e.getPoint()
      pt.y = -size.height
      SwingUtilities.convertPointToScreen(pt, e.getComponent())
      pt.translate(-size.width / 2, 0)
      toolTip.setLocation(pt)
    }
    prevValue = intValue
  }

  override fun mouseDragged(e: MouseEvent) {
    updateToolTip(e)
  }

  override fun mousePressed(e: MouseEvent) {
    if (UIManager.getBoolean("Slider.onlyLeftMouseButtonDrag") && SwingUtilities.isLeftMouseButton(e)) {
      toolTip.setVisible(true)
      updateToolTip(e)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    toolTip.setVisible(false)
  }
}

internal class SliderMouseWheelListener : MouseWheelListener {
  override fun mouseWheelMoved(e: MouseWheelEvent) {
    val s = e.getComponent() as? JSlider ?: return
    val i = s.getValue().toInt() - e.getWheelRotation()
    val m = s.getModel()
    s.setValue(minOf(maxOf(i, m.getMinimum()), m.getMaximum()))
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
