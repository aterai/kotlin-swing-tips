package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.*
import kotlin.math.roundToInt

private const val THUMB_SIZE = 24
private val THUMB_COLOR = Color(0x21_98_F6)

fun createUI(): JPanel {
  val overrides = UIDefaults()
  overrides["Slider.thumbWidth"] = THUMB_SIZE
  overrides["Slider.thumbHeight"] = THUMB_SIZE
  // Paint a filled circle with the current value drawn on top as the slider thumb.
  val thumbPainter = Painter { g, c: JSlider, w, h ->
    g.paint = THUMB_COLOR
    g.fillOval(0, 0, w, h)
    val icon = NumberIcon(c.value)
    val iconX = (w - icon.iconWidth) / 2
    val iconY = (h - icon.iconHeight) / 2
    icon.paintIcon(c, g, iconX, iconY)
  }
  overrides["Slider:SliderThumb[Disabled].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[Enabled].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[Focused+MouseOver].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[Focused+Pressed].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[Focused].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[MouseOver].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderThumb[Pressed].backgroundPainter"] = thumbPainter
  overrides["Slider:SliderTrack[Enabled].backgroundPainter"] = SliderTrackPainter()

  val slider = JSlider()
  slider.snapToTicks = true
  slider.majorTickSpacing = 10
  slider.putClientProperty("Nimbus.Overrides", overrides)
  slider.addMouseMotionListener(object : MouseAdapter() {
    override fun mouseDragged(e: MouseEvent) {
      super.mouseDragged(e)
      e.component.repaint()
    }
  })

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(createTitledPanel("Default", JSlider()))
  box.add(Box.createVerticalStrut(5))
  box.add(createTitledPanel("Paint major tick marks on the track", slider))
  box.add(Box.createVerticalGlue())

  return JPanel(GridLayout(0, 1)).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val panel = JPanel(BorderLayout())
  panel.setBorder(BorderFactory.createTitledBorder(title))
  panel.add(c)
  return panel
}

private class SliderTrackPainter : Painter<JSlider> {
  override fun paint(g: Graphics2D, slider: JSlider, width: Int, height: Int) {
    val thumbSize = 24
    val trackHeight = 8
    val trackWidth = width - thumbSize
    val arc = 10
    val fillTop = (thumbSize - trackHeight) / 2
    val fillLeft = thumbSize / 2

    // Paint track
    g.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g.color = TRACK_COLOR
    g.fillRoundRect(fillLeft, fillTop + 2, trackWidth, trackHeight - 4, arc, arc)

    val fillBottom = fillTop + trackHeight
    val trackRect = Rectangle(fillLeft, fillTop, trackWidth, fillBottom - fillTop)

    // Paint the major tick marks on the track
    g.color = TICK_COLOR
    var value = slider.minimum
    val tickSize = 4
    while (value <= slider.maximum) {
      val tickX = getPositionForValue(slider, trackRect, value.toFloat())
      g.fillOval(tickX, trackRect.centerY.toInt() - tickSize / 2, tickSize, tickSize)
      // Overflow checking
      if (Int.MAX_VALUE - slider.getMajorTickSpacing() < value) {
        break
      }
      value += slider.getMajorTickSpacing()
    }

    // JSlider.isFilled
    val fillRight = getPositionForValue(slider, trackRect, slider.value.toFloat())
    g.color = FILL_COLOR
    g.fillRoundRect(
      fillLeft,
      fillTop,
      fillRight - fillLeft,
      fillBottom - fillTop,
      arc,
      arc,
    )
  }

  private fun getPositionForValue(
    slider: JSlider,
    trackRect: Rectangle,
    value: Float,
  ): Int {
    val min = slider.minimum.toFloat()
    val max = slider.maximum.toFloat()
    val pixelsPerValue = trackRect.width / (max - min)
    val trackLeft = trackRect.x
    val trackRight = trackRect.x + trackRect.width - 1
    val pos = trackLeft + (pixelsPerValue * (value - min)).roundToInt()
    return pos.coerceIn(trackLeft, trackRight)
  }

  companion object {
    private val TRACK_COLOR = Color(0xC6E4FC)
    private val TICK_COLOR = Color(0x31A8F8)
    private val FILL_COLOR = Color(0x2198F6)
  }
}

private class NumberIcon(
  private val value: Int,
) : Icon {
  fun getTextShape(g2: Graphics2D): Shape {
    val txt = if (value > MAX_VALUE_LENGTH) "1K" else value.toString()
    val at = if (txt.length < 3) {
      null
    } else {
      AffineTransform.getScaleInstance(NARROW_SCALE_X, 1.0)
    }
    return TextLayout(txt, g2.font, g2.fontRenderContext).getOutline(at)
  }

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)

    val shape = getTextShape(g2)
    val b = shape.bounds2D
    val tx = iconWidth / 2.0 - b.centerX
    val ty = iconHeight / 2.0 - b.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(tx, ty)
    g2.paint = Color.WHITE
    g2.fill(toCenterAt.createTransformedShape(shape))
    g2.dispose()
  }

  override fun getIconWidth() = ICON_SIZE

  override fun getIconHeight() = ICON_SIZE

  companion object {
    private const val ICON_SIZE = 20
    private const val MAX_VALUE_LENGTH = 999
    private const val NARROW_SCALE_X = .66
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
      // UIManager.put("JSlider.isFilled", true)
      // UIManager.put("Slider.paintValue", true)
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
