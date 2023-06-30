package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import javax.swing.*
import kotlin.math.roundToInt

fun makeUI(): Component {
  val d = UIDefaults()
  d["Slider.thumbWidth"] = 24
  d["Slider.thumbHeight"] = 24
  val thumbPainter = Painter { g, c: JSlider, w, h ->
    g.paint = Color(0x21_98_F6)
    g.fillOval(0, 0, w, h)
    val icon = NumberIcon(c.value)
    val xx = (w - icon.iconWidth) / 2
    val yy = (h - icon.iconHeight) / 2
    icon.paintIcon(c, g, xx, yy)
  }
  d["Slider:SliderThumb[Disabled].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Enabled].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused+MouseOver].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused+Pressed].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Focused].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[MouseOver].backgroundPainter"] = thumbPainter
  d["Slider:SliderThumb[Pressed].backgroundPainter"] = thumbPainter
  d["Slider:SliderTrack[Enabled].backgroundPainter"] = object : Painter<JSlider> {
    override fun paint(g: Graphics2D, c: JSlider, w: Int, h: Int) {
      val arc = 10
      val thumbSize = 24
      val trackHeight = 8
      val tickSize = 4
      val trackWidth = w - thumbSize
      val fillTop = (thumbSize - trackHeight) / 2
      val fillLeft = thumbSize / 2

      // Paint track
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g.color = Color(0xC6_E4_FC)
      g.fillRoundRect(fillLeft, fillTop + 2, trackWidth, trackHeight - 4, arc, arc)

      val fillBottom = fillTop + trackHeight
      val r = Rectangle(fillLeft, fillTop, trackWidth, fillBottom - fillTop)

      // Paint the major tick marks on the track
      g.color = Color(0x31_A8_F8)
      var value = c.minimum
      while (value <= c.maximum) {
        val xpt = getPositionForValue(c, r, value.toFloat())
        g.fillOval(xpt, r.centerY.toInt() - tickSize / 2, tickSize, tickSize)
        // Overflow checking
        if (Int.MAX_VALUE - c.majorTickSpacing < value) {
          break
        }
        value += c.majorTickSpacing
      }

      // JSlider.isFilled
      val fillRight = getPositionForValue(c, r, c.value.toFloat())
      g.fillRoundRect(fillLeft, fillTop, fillRight - fillLeft, fillBottom - fillTop, arc, arc)
    }

    private fun getPositionForValue(slider: JSlider, trackRect: Rectangle, value: Float): Int {
      val min = slider.minimum.toFloat()
      val max = slider.maximum.toFloat()
      val pixelsPerValue = trackRect.width / (max - min)
      val trackLeft = trackRect.x
      val trackRight = trackRect.x + trackRect.width - 1
      val pos = trackLeft + (pixelsPerValue * (value - min)).roundToInt()
      return pos.coerceIn(trackLeft, trackRight)
    }
  }
  val slider = JSlider()
  slider.snapToTicks = true
  slider.majorTickSpacing = 10
  slider.putClientProperty("Nimbus.Overrides", d)
  slider.addMouseMotionListener(object : MouseAdapter() {
    override fun mouseDragged(e: MouseEvent) {
      super.mouseDragged(e)
      e.component.repaint()
    }
  })

  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Default", JSlider()))
  box.add(Box.createVerticalStrut(5))
  box.add(makeTitledPanel("Paint major tick marks on the track", slider))
  box.add(Box.createVerticalGlue())

  return JPanel(GridLayout(0, 1)).also {
    it.add(box)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class NumberIcon(private val value: Int) : Icon {
  private fun getTextShape(g2: Graphics2D): Shape {
    val txt = if (value > 999) "1K" else value.toString()
    val at = if (txt.length < 3) null else AffineTransform.getScaleInstance(.66, 1.0)
    return TextLayout(txt, g2.font, g2.fontRenderContext).getOutline(at)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val shape = getTextShape(g2)
    val b = shape.bounds
    val tx = iconWidth / 2.0 - b.centerX
    val ty = iconHeight / 2.0 - b.centerY
    val toCenterAtf = AffineTransform.getTranslateInstance(tx, ty)
    g2.paint = Color.WHITE
    g2.fill(toCenterAtf.createTransformedShape(shape))
    g2.dispose()
  }

  override fun getIconWidth() = 20

  override fun getIconHeight() = 20
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
