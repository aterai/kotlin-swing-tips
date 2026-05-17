package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.plaf.LayerUI
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt

fun createUI(): Component {
  val box = Box.createVerticalBox()
  box.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20))
  addLabel(box, ShimmerLabel("Standard Area Shimmer"))
  addLabel(box, TextShimmerLabel("Text-Only Shimmer (Clipping)"))
  addLabel(box, TextCompositeShimmerLabel("Text-Only Shimmer (Composite)"))
  val text = "JLayer Shimmer Label"
  val icon = UIManager.getIcon("InternalFrame.icon")
  val label = JLabel(text, icon, JLabel.LEADING)
  addLabel(box, JLayer(label, ShimmerLayerUI()))
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addLabel(box: Box, component: JComponent) {
  component.setAlignmentX(Component.LEFT_ALIGNMENT)
  box.add(component)
  box.add(Box.createVerticalStrut(20))
}

private fun addLabel(box: Box, label: JLabel) {
  label.setIcon(UIManager.getIcon("InternalFrame.icon"))
  addLabel(box, label as JComponent)
}

private object ShimmerColors {
  fun luminance(c: Color): Double {
    val r = srgbToLinear(c.red / 255.0)
    val g = srgbToLinear(c.green / 255.0)
    val b = srgbToLinear(c.blue / 255.0)
    return .2126 * r + .7152 * g + .0722 * b
  }

  fun srgbToLinear(v: Double) = if (v <= .04045) {
    v / 12.92
  } else {
    ((v + .055) / 1.055).pow(2.4)
  }

  fun shimmerBright(fg: Color, alpha: Int): Color {
    val isDarkMode = luminance(fg) >= 0.5
    val r = if (isDarkMode) blend(fg.red, 255, 0.6) else 255
    val g = if (isDarkMode) blend(fg.green, 255, 0.6) else 255
    val b = if (isDarkMode) blend(fg.blue, 255, 0.6) else 255
    return Color(r, g, b, alpha)
  }

  fun blend(src: Int, dst: Int, t: Double) = (src + (dst - src) * t).roundToInt()
}

private object ShimmerLayout {
  fun layoutLabel(label: JLabel, g2: Graphics2D): Array<Rectangle> {
    val area = SwingUtilities.calculateInnerArea(label, null)
    val fm = g2.getFontMetrics(label.getFont())
    val iconRect = Rectangle()
    val textRect = Rectangle()
    SwingUtilities.layoutCompoundLabel(
      label,
      fm,
      label.text,
      label.icon,
      label.verticalAlignment,
      label.horizontalAlignment,
      label.verticalTextPosition,
      label.horizontalTextPosition,
      area,
      iconRect,
      textRect,
      label.iconTextGap,
    )
    return arrayOf(iconRect, textRect)
  }
}

private abstract class AbstractShimmerLabel(
  text: String,
  fps: Int,
  speed: Float,
  bandWidth: Int,
) : JLabel(text) {
  protected val fractions = floatArrayOf(0f, .5f, 1f)
  protected var animX = 0f

  private val shimmerTimer = Timer(fps) {
    animX += speed
    if (animX > getWidth() + bandWidth) {
      animX = -bandWidth.toFloat()
    }
    repaint()
  }

  init {
    shimmerTimer.start()
  }

  override fun isOpaque() = false

  override fun removeNotify() {
    shimmerTimer.stop()
    super.removeNotify()
  }

  fun clearTextRect(g2: Graphics2D, textRect: Rectangle) {
    val oldPaint = g2.paint
    g2.paint = getBackground()
    g2.fillRect(textRect.x, textRect.y, textRect.width, textRect.height)
    g2.paint = oldPaint
  }

  companion object {
    val TRANSPARENT = Color(0x0, true)
    const val ALPHA = 200

    fun applyRenderingHints(g2: Graphics2D) {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
      )
    }
  }
}

// ShimmerLabel: Shimmer that sweeps the entire component rectangle.
private class ShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, FPS, SPEED, BAND_WIDTH) {
  private val colors = arrayOf<Color>(SHIMMER_BASE, SHIMMER_BRIGHT, SHIMMER_BASE)

  override fun isOpaque() = true

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  companion object {
    const val FPS = 15
    const val BAND_WIDTH = 80
    const val SPEED = 4f
    val SHIMMER_BASE = Color(0x00_FF_FF_FF, true)
    val SHIMMER_BRIGHT = Color(-0x37_00_00_01, true)
  }
}

private class TextShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, FPS, SPEED, BAND_WIDTH) {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    applyRenderingHints(g2)

    val rects = ShimmerLayout.layoutLabel(this, g2)
    val textRect = rects[1]
    val fm = g2.getFontMetrics(getFont())

    val x = textRect.x.toFloat()
    val y = (textRect.y + fm.ascent).toFloat()

    // Clear the text area painted by super.paintComponent to avoid double-draw.
    clearTextRect(g2, textRect)

    // 1. Base text
    g2.paint = getForeground()
    val frc = g2.fontRenderContext
    val layout = TextLayout(text, getFont(), frc)
    layout.draw(g2, x, y)

    // 2. Clip to text outline
    val dx = x.toDouble()
    val dy = y.toDouble()
    g2.clip(layout.getOutline(AffineTransform.getTranslateInstance(dx, dy)))

    // 3. Draw gradient inside clip
    val colors = arrayOf(
      TRANSPARENT,
      ShimmerColors.shimmerBright(getForeground(), ALPHA),
      TRANSPARENT,
    )
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  companion object {
    private const val FPS = 15
    private const val BAND_WIDTH = 100
    private const val SPEED = 4f
  }
}

private class TextCompositeShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, FPS, SPEED, BAND_WIDTH) {
  private var buffer: BufferedImage? = null

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as Graphics2D
    applyRenderingHints(g2)

    val rects = ShimmerLayout.layoutLabel(this, g2)
    val textRect = rects[1]
    clearTextRect(g2, textRect)
    updateBuffer()
    buffer?.also {
      g2.drawImage(it, 0, 0, this)
    }
    g2.dispose()
  }

  private fun updateBuffer() {
    val w = max(1, getWidth())
    val h = max(1, getHeight())
    if (buffer == null || buffer?.width != w || buffer?.height != h) {
      buffer = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }
    val g2 = buffer?.createGraphics() ?: return
    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, w, h)
    g2.composite = AlphaComposite.SrcOver
    applyRenderingHints(g2)

    val rects = ShimmerLayout.layoutLabel(this, g2)
    val textRect = rects[1]
    val fm = g2.getFontMetrics(getFont())

    val x = textRect.x.toFloat()
    val y = (textRect.y + fm.ascent).toFloat()

    g2.paint = getForeground()
    val frc = g2.fontRenderContext
    TextLayout(text, getFont(), frc).draw(g2, x, y)

    g2.composite = AlphaComposite.SrcAtop
    val colors = arrayOf(
      TRANSPARENT,
      ShimmerColors.shimmerBright(getForeground(), ALPHA),
      TRANSPARENT,
    )
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, w, h)
    g2.dispose()
  }

  companion object {
    private const val FPS = 15
    private const val BAND_WIDTH = 160
    private const val SPEED = 4f
  }
}

private class ShimmerLayerUI : LayerUI<JLabel>() {
  private val timer = Timer(FPS, null)
  private val fractions = floatArrayOf(0f, .5f, 1f)
  private var animX = 0f
  private var buffer: BufferedImage? = null

  override fun installUI(c: JComponent) {
    super.installUI(c)
    timer.addActionListener {
      animX += SPEED
      if (animX > c.getWidth() + BAND_WIDTH) {
        animX = -BAND_WIDTH.toFloat()
      }
      c.repaint()
    }
    timer.start()
  }

  override fun uninstallUI(c: JComponent?) {
    timer.stop()
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent?) {
    // super.paint(g, c);
    val layer = c as JLayer<*>
    val label = layer.getView() as? JLabel
    if (label == null || label.text == null || label.text.isEmpty()) {
      super.paint(g, c)
    } else {
      val w = max(1, c.getWidth())
      val h = max(1, c.getHeight())
      ensureBuffer(w, h)
      paintLayerToBuffer(c, w, h)
      val shimBuf = createShimmerBuffer(label, c, w, h)
      paintShimmerBuffer(shimBuf)
      g.drawImage(buffer, 0, 0, c)
    }
  }

  private fun ensureBuffer(w: Int, h: Int) {
    if (buffer == null || buffer?.width != w || buffer?.height != h) {
      buffer = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }
  }

  private fun paintLayerToBuffer(c: JComponent, w: Int, h: Int) {
    val buf = buffer?.createGraphics() ?: return
    buf.composite = AlphaComposite.Clear
    buf.fillRect(0, 0, w, h)
    buf.composite = AlphaComposite.SrcOver
    c.paint(buf)
    buf.dispose()
  }

  private fun createShimmerBuffer(
    label: JLabel,
    c: JComponent,
    w: Int,
    h: Int,
  ): BufferedImage {
    val shimBuf = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val sg = shimBuf.createGraphics()

    AbstractShimmerLabel.applyRenderingHints(sg)
    val rects = ShimmerLayout.layoutLabel(label, sg)
    val textRect = rects[1]
    val fm = sg.getFontMetrics(label.getFont())
    val labelOrigin = SwingUtilities.convertPoint(label, 0, 0, c)
    val x = (labelOrigin.x + textRect.x).toFloat()
    val y = (labelOrigin.y + textRect.y + fm.ascent).toFloat()

    // 1. Draw the text outline to the shimmer buffer (to serve as a mask).
    sg.paint = label.getForeground()
    val frc = sg.fontRenderContext
    TextLayout(label.text, label.getFont(), frc).draw(sg, x, y)

    // 2. Compose the gradient using SrcAtop (masked by the text's alpha channel).
    sg.composite = AlphaComposite.SrcAtop
    val colors = arrayOf(
      TRANSPARENT,
      ShimmerColors.shimmerBright(label.getForeground(), ALPHA),
      TRANSPARENT,
    )
    val endX = animX + BAND_WIDTH
    sg.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    sg.fillRect(0, 0, w, h)
    sg.dispose()

    // Clear the text area painted
    val buf = buffer!!.createGraphics()
    // buf.setComposite(AlphaComposite.DstOut);
    buf.paint = label.getBackground()
    buf.fill(textRect)
    buf.dispose()

    return shimBuf
  }

  private fun paintShimmerBuffer(shimBuf: BufferedImage) {
    val buf = buffer?.createGraphics() ?: return
    buf.composite = AlphaComposite.SrcOver
    buf.drawImage(shimBuf, 0, 0, null)
    buf.dispose()
  }

  companion object {
    private const val FPS = 16
    private const val BAND_WIDTH = 100
    private const val SPEED = 4f
    private const val ALPHA = 180
    private val TRANSPARENT = Color(0x0, true)
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
