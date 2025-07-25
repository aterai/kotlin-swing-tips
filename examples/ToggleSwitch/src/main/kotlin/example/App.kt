package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.roundToInt

fun makeUI(): Component {
  val def = UIManager.getLookAndFeelDefaults()
  def["Slider.thumbWidth"] = 40
  def["Slider.thumbHeight"] = 40
  val slider0 = makeToggleSlider(def)
  val d = makeSliderPainter()
  val slider1 = makeToggleSlider(d)
  slider1.addMouseMotionListener(object : MouseAdapter() {
    override fun mouseDragged(e: MouseEvent) {
      super.mouseDragged(e)
      e.component.repaint()
    }
  })
  val layer = JLayer(makeToggleSlider(d), ToggleSwitchLayerUI())
  return JPanel().also {
    it.add(makeTitledPanel("Default", makeToggleSlider(null)))
    it.add(makeTitledPanel("Thumb size", slider0))
    it.add(makeTitledPanel("SliderTrack", slider1))
    it.add(makeTitledPanel("JSlider + JLayer", layer))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeToggleSlider(d: UIDefaults?): JSlider {
  val slider = object : JSlider(0, 1, 0) {
    override fun getPreferredSize() = Dimension(100, 40)
  }
  slider.font = slider.font.deriveFont(Font.BOLD, 32f)
  if (d != null) {
    slider.putClientProperty("Nimbus.Overrides", d)
  }
  return slider
}

private fun makeSliderPainter(): UIDefaults {
  val d = UIDefaults()
  d["Slider.thumbWidth"] = 40
  d["Slider.thumbHeight"] = 40
  d["Slider:SliderTrack[Enabled].backgroundPainter"] = makeBackgroundPainter()
  val thumbPainter = makeThumbPainter()
  d["Slider:SliderThumb[Disabled].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Enabled].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused+MouseOver].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused+Pressed].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[MouseOver].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Pressed].backgroundPainter"] = thumbPainter
  return d
}

private fun makeBackgroundPainter() = Painter<JSlider> { g, c, w, h ->
  val arc = 40
  val fillLeft = 2
  val fillTop = 2
  val trackWidth = w - fillLeft - fillLeft
  val trackHeight = h - fillTop - fillTop
  val baseline = trackHeight - fillTop - fillTop
  val off = "Off"
  g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g.color = Color.GRAY
  g.fillRoundRect(fillLeft, fillTop, trackWidth, trackHeight, arc, arc)
  g.paint = Color.WHITE
  g.drawString(off, w - g.fontMetrics.stringWidth(off) - fillLeft * 5, baseline)
  val trackRect = Rectangle(fillLeft, fillTop, trackWidth, trackHeight)
  val fillRight = getPositionForValue(c, trackRect)
  g.color = Color.ORANGE
  g.fillRoundRect(fillLeft + 1, fillTop, fillRight - fillLeft, trackHeight, arc, arc)
  g.paint = Color.WHITE
  if (fillRight - fillLeft > 0) {
    g.drawString("On", fillLeft * 5, baseline)
  }
  g.stroke = BasicStroke(2.5f)
  g.drawRoundRect(fillLeft, fillTop, trackWidth, trackHeight, arc, arc)
}

private fun makeThumbPainter() = Painter<JSlider> { g, _, w, h ->
  val fillLeft = 8
  val fillTop = 8
  val trackWidth = w - fillLeft - fillLeft
  val trackHeight = h - fillTop - fillTop
  g.paint = Color.WHITE
  g.fillOval(fillLeft, fillTop, trackWidth, trackHeight)
}

// @see javax/swing/plaf/basic/BasicSliderUI#xPositionForValue(int value)
private fun getPositionForValue(
  slider: JSlider,
  trackRect: Rectangle,
): Int {
  val value = slider.value
  val min = slider.minimum
  val max = slider.maximum
  val trackLength = trackRect.width
  val valueRange = max.toFloat() - min.toFloat()
  val pixelsPerValue = trackLength.toFloat() / valueRange
  val trackLeft = trackRect.x
  val trackRight = trackRect.x + trackRect.width - 1
  var xp = trackLeft
  xp += (pixelsPerValue * (value.toFloat() - min)).roundToInt()
  xp = trackLeft.coerceAtLeast(xp)
  xp = trackRight.coerceAtMost(xp)
  return xp
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

private class ToggleSwitchLayerUI : LayerUI<JSlider>() {
  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(
    e: MouseEvent,
    l: JLayer<out JSlider>,
  ) {
    val leftMouseButton = SwingUtilities.isLeftMouseButton(e)
    if (e.id == MouseEvent.MOUSE_PRESSED && leftMouseButton) {
      e.component.dispatchEvent(
        MouseEvent(
          e.component,
          e.id,
          e.getWhen(),
          InputEvent.BUTTON3_DOWN_MASK,
          e.x,
          e.y,
          e.xOnScreen,
          e.yOnScreen,
          e.clickCount,
          e.isPopupTrigger,
          MouseEvent.BUTTON3,
        ),
      )
      e.consume()
    } else if (e.id == MouseEvent.MOUSE_CLICKED && leftMouseButton) {
      val slider = l.view
      val v = slider.value
      if (slider.minimum == v) {
        slider.value = slider.maximum
      } else if (slider.maximum == v) {
        slider.value = slider.minimum
      }
    }
  }

  override fun processMouseMotionEvent(
    e: MouseEvent,
    l: JLayer<out JSlider>,
  ) {
    l.view.repaint()
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
