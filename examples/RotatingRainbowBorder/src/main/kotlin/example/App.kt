package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.awt.image.DataBufferInt
import javax.swing.*
import javax.swing.border.EmptyBorder
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

fun createUI(): Component {
  val label1 = AnimatedLabel("LinearGradient")
  label1.setBorder(RotatingRainbowBorder(16, 16, 16, 16))
  val label2 = AnimatedLabel("ConicGradient")
  label2.setBorder(RotatingConicRainbowBorder(16, 16, 16, 16))
  return JPanel(GridLayout(2, 1, 20, 20)).also {
    it.add(label1)
    it.add(label2)
    it.border = BorderFactory.createEmptyBorder(20, 20, 20, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private class AnimatedLabel(
  text: String?,
) : JLabel(text) {
  private var rotation = 0f
  private val timer = Timer(16) {
    val border = getBorder()
    if (border is RotatingRainbowBorder) {
      rotation += .08f
      border.rotation = rotation
      repaint()
    }
  }

  override fun getPreferredSize() = Dimension(240, 100)

  override fun addNotify() {
    super.addNotify()
    timer.start()
  }

  override fun removeNotify() {
    super.removeNotify()
    timer.stop()
  }
}

private open class RotatingRainbowBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
) : EmptyBorder(top, left, bottom, right) {
  var rotation: Float = 0f

  override fun paintBorder(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val cx = x + width * .5f
    val cy = y + height * .5f

    val gradientRadius = hypot(width.toDouble(), height.toDouble()).toFloat() * .5f
    val dx = (cos(this.rotation.toDouble()) * gradientRadius).toFloat()
    val dy = (sin(this.rotation.toDouble()) * gradientRadius).toFloat()
    g2.paint = LinearGradientPaint(
      Point2D.Float(cx - dx, cy - dy),
      Point2D.Float(cx + dx, cy + dy),
      FRACTIONS,
      COLORS,
    )
    g2.fill(createBorderArea(x, y, width, height))
    g2.dispose()
  }

  companion object {
    const val BORDER_WIDTH = 2f
    const val CORNER_RADIUS = 12f
    const val HUE_STEPS = 24
    val FRACTIONS = FloatArray(HUE_STEPS + 1) { it.toFloat() / HUE_STEPS }
    val COLORS = Array(HUE_STEPS + 1) { Color.getHSBColor(FRACTIONS[it], .8f, 1f) }

    fun createBorderArea(x: Int, y: Int, width: Int, height: Int): Area {
      val outer: Shape = RoundRectangle2D.Float(
        x.toFloat(),
        y.toFloat(),
        width - 1f,
        height - 1f,
        CORNER_RADIUS,
        CORNER_RADIUS,
      )
      val inner: Shape = RoundRectangle2D.Float(
        x + BORDER_WIDTH,
        y + BORDER_WIDTH,
        width - BORDER_WIDTH * 2f - 1f,
        height - BORDER_WIDTH * 2f - 1f,
        CORNER_RADIUS - BORDER_WIDTH * 2f,
        CORNER_RADIUS - BORDER_WIDTH * 2f,
      )
      val area = Area(outer)
      area.subtract(Area(inner))
      return area
    }
  }
}

private class RotatingConicRainbowBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
) : RotatingRainbowBorder(top, left, bottom, right) {
  private var offscreen: BufferedImage? = null
  private var cachedWidth = 0
  private var cachedHeight = 0

  override fun paintBorder(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    if (offscreen == null || cachedWidth != width || cachedHeight != height) {
      offscreen = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
      cachedWidth = width
      cachedHeight = height
    }
    val borderArea = createBorderArea(x, y, width, height)
    offscreen?.also {
      val g2 = it.createGraphics()
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.composite = AlphaComposite.Clear
      g2.fillRect(0, 0, width, height)
      // Pass 1: Draw border shape with AA enabled → Edge alpha is smooth
      g2.composite = AlphaComposite.Src
      g2.fill(borderArea)
      g2.dispose()

      // Pass 2: Pixel-direct conic gradient while preserving AA alpha from pass 1
      renderConicGradient(it, borderArea, this.rotation)
      g.drawImage(it, 0, 0, null)
    }
  }

  private fun renderConicGradient(
    offscreen: BufferedImage,
    borderArea: Area,
    rotation: Float,
  ) {
    val width = offscreen.width
    val height = offscreen.height
    val cx = width * .5
    val cy = height * .5
    val dataBuffer = offscreen.raster.getDataBuffer()
    val pixels = (dataBuffer as? DataBufferInt)?.getData() ?: return
    val bounds = borderArea.bounds
    for (py in bounds.y..<bounds.y + bounds.height) {
      for (px in bounds.x..<bounds.x + bounds.width) {
        val dstAlpha = (pixels[py * width + px] ushr 24) and 0xFF
        if (dstAlpha == 0) {
          continue
        }
        val angle = atan2(py - cy, px - cx) - rotation
        val v = angle / (2.0 * Math.PI)
        val t = (v % 1.0 + 1.0) % 1.0
        val rgb = interpolateColorRgb(t) and 0x00_FF_FF_FF
        pixels[py * width + px] = (dstAlpha shl 24) or rgb
      }
    }
  }

  private fun interpolateColorRgb(t: Double): Int {
    // FRACTIONS is evenly spaced, so the segment index is computable directly
    val pos = t.coerceIn(0.0, 1.0) * HUE_STEPS
    val idx = pos.toInt().coerceAtMost(HUE_STEPS - 1)
    val ratio = (pos - idx).toFloat()
    return interpolateArgb(COLORS[idx].rgb, COLORS[idx + 1].rgb, ratio)
  }

  private fun interpolateArgb(c0: Int, c1: Int, t: Float): Int {
    val a = lerp((c0 ushr 24) and 0xFF, (c1 ushr 24) and 0xFF, t)
    val r = lerp((c0 ushr 16) and 0xFF, (c1 ushr 16) and 0xFF, t)
    val g = lerp((c0 ushr 8) and 0xFF, (c1 ushr 8) and 0xFF, t)
    val b = lerp(c0 and 0xFF, c1 and 0xFF, t)
    return (a shl 24) or (r shl 16) or (g shl 8) or b
  }

  private fun lerp(a: Int, b: Int, t: Float) = (a + (b - a) * t).roundToInt()
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
