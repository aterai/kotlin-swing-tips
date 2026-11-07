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
  addComponent(box, JLayer(label, ShimmerLayerUI()))
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun addComponent(box: Box, component: JComponent) {
  component.setAlignmentX(Component.LEFT_ALIGNMENT)
  box.add(component)
  box.add(Box.createVerticalStrut(20))
}

private fun addLabel(box: Box, label: JLabel) {
  label.setIcon(UIManager.getIcon("InternalFrame.icon"))
  addComponent(box, label)
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

  fun highlightColor(fg: Color, alpha: Int): Color {
    val isDarkMode = luminance(fg) >= 0.5
    val r = if (isDarkMode) blend(fg.red, 255, 0.6) else 255
    val g = if (isDarkMode) blend(fg.green, 255, 0.6) else 255
    val b = if (isDarkMode) blend(fg.blue, 255, 0.6) else 255
    return Color(r, g, b, alpha)
  }

  // Returns the gradient colors of the shimmer band: transparent, highlight, transparent.
  // LinearGradientPaint interpolates non-premultiplied colors, so the transparent ends
  // keep the RGB of the highlight; fading to transparent black would darken the band edges.
  fun gradientColors(fg: Color, alpha: Int): Array<Color> {
    val highlight = highlightColor(fg, alpha)
    val transparent = Color(highlight.rgb and 0xFF_FF_FF, true)
    return arrayOf(transparent, highlight, transparent)
  }

  fun blend(src: Int, dst: Int, t: Double) = (src + (dst - src) * t).roundToInt()
}

private object ShimmerLayout {
  // TextLayout rejects a null or empty string, so check this before creating one.
  fun hasText(label: JLabel) = !label.text.isNullOrEmpty()

  // Computes the text bounds of the given label using the Graphics2D context.
  fun getTextRect(label: JLabel, g2: Graphics2D): Rectangle {
    val area = SwingUtilities.calculateInnerArea(label, null)
    val fm = g2.getFontMetrics(label.getFont())
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
      Rectangle(),
      textRect,
      label.iconTextGap,
    )
    return textRect
  }
}

private abstract class AbstractShimmerLabel(
  text: String,
  delay: Int,
  speed: Float,
  bandWidth: Int,
) : JLabel(text) {
  protected val fractions = floatArrayOf(0f, .5f, 1f)
  protected var animX = 0f

  private val shimmerTimer = Timer(delay) {
    animX += speed
    if (animX > getWidth() + bandWidth) {
      animX = -bandWidth.toFloat()
    }
    repaint()
  }

  override fun isOpaque() = false

  // Run the timer only while displayable so that the animation
  // also resumes when the label is added to a container again.
  override fun addNotify() {
    super.addNotify()
    shimmerTimer.start()
  }

  override fun removeNotify() {
    shimmerTimer.stop()
    super.removeNotify()
  }

  fun clearTextRect(g2: Graphics2D, textRect: Rectangle) {
    g2.paint = getBackground()
    g2.fill(textRect)
  }

  companion object {
    const val ALPHA = 200

    fun applyRenderingHints(label: JLabel, g2: Graphics2D) {
      val isWindows = label
        .getUI()
        .javaClass
        .getName()
        .contains("Windows")
      if (!isWindows) {
        val toolkit = Toolkit.getDefaultToolkit()
        val hints = toolkit.getDesktopProperty("awt.font.desktophints")
        if (hints is MutableMap<*, *>) {
          g2.addRenderingHints(hints)
        }
      }
    }
  }
}

// ShimmerLabel: Shimmer that sweeps the entire component rectangle.
private class ShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, DELAY, SPEED, BAND_WIDTH) {
  private val colors = arrayOf(SHIMMER_BASE, SHIMMER_BRIGHT, SHIMMER_BASE)

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
    const val DELAY = 15
    const val BAND_WIDTH = 80
    const val SPEED = 4f
    val SHIMMER_BASE = Color(0x00_FF_FF_FF, true)
    val SHIMMER_BRIGHT = Color(-0x37_00_00_01, true)
  }
}

private class TextShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, DELAY, SPEED, BAND_WIDTH) {
  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (ShimmerLayout.hasText(this)) {
      paintShimmerText(g)
    }
  }

  private fun paintShimmerText(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    applyRenderingHints(this, g2)

    val textRect = ShimmerLayout.getTextRect(this, g2)
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
    val colors = ShimmerColors.gradientColors(getForeground(), ALPHA)
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, getWidth(), getHeight())
    g2.dispose()
  }

  companion object {
    private const val DELAY = 15
    private const val BAND_WIDTH = 100
    private const val SPEED = 4f
  }
}

private class TextCompositeShimmerLabel(
  text: String,
) : AbstractShimmerLabel(text, DELAY, SPEED, BAND_WIDTH) {
  private var buffer: BufferedImage? = null

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (ShimmerLayout.hasText(this)) {
      val g2 = g.create() as? Graphics2D ?: return
      val textRect = ShimmerLayout.getTextRect(this, g2)

      // Clear the text area painted by super.paintComponent to avoid double-draw.
      clearTextRect(g2, textRect)

      updateBuffer(textRect)
      g2.drawImage(buffer, 0, 0, this)
      g2.dispose()
    }
  }

  private fun updateBuffer(textRect: Rectangle) {
    val w = max(1, getWidth())
    val h = max(1, getHeight())
    if (buffer == null || buffer?.width != w || buffer?.height != h) {
      buffer = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }
    val g2 = buffer?.createGraphics() ?: return
    applyRenderingHints(this, g2)
    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, w, h)
    g2.composite = AlphaComposite.SrcOver

    val fm = g2.getFontMetrics(getFont())
    val x = textRect.x.toFloat()
    val y = (textRect.y + fm.ascent).toFloat()

    // 1. Draw base text into buffer.
    g2.paint = getForeground()
    val frc = g2.fontRenderContext
    TextLayout(text, getFont(), frc).draw(g2, x, y)

    // 2. Composite shimmer gradient over text pixels only (SrcAtop).
    g2.composite = AlphaComposite.SrcAtop
    val colors = ShimmerColors.gradientColors(getForeground(), ALPHA)
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, w, h)
    g2.dispose()
  }

  companion object {
    private const val DELAY = 15
    private const val BAND_WIDTH = 160
    private const val SPEED = 4f
  }
}

private class ShimmerLayerUI : LayerUI<JLabel>() {
  private val timer = Timer(DELAY, null)
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
    // Otherwise reinstalling this UI would add a second listener and double the speed.
    timer.actionListeners.forEach { timer.removeActionListener(it) }
    super.uninstallUI(c)
  }

  // `super.paint()` draws the `JLabel` including its plain text, so the text area is
  // cleared and then overlaid with the text-only shimmer buffer using SrcOver.
  // Applying SrcAtop to the whole layer would cause the shimmer to affect the icon as well,
  // so the text shimmer is composited in a separate buffer before being transferred.
  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    val layer = c as JLayer<*>
    val label = layer.getView() as? JLabel
    if (label != null && ShimmerLayout.hasText(label)) {
      val g2 = g.create() as? Graphics2D ?: return
      val textRect = ShimmerLayout.getTextRect(label, g2)
      val labelOrigin = SwingUtilities.convertPoint(label, 0, 0, c)
      textRect.translate(labelOrigin.x, labelOrigin.y)

      g2.paint = label.getBackground()
      g2.fill(textRect)

      updateBuffer(label, textRect, c.size)
      g2.drawImage(buffer, 0, 0, c)
      g2.dispose()
    }
  }

  private fun updateBuffer(label: JLabel, textRect: Rectangle, size: Dimension) {
    val w = max(1, size.width)
    val h = max(1, size.height)
    if (buffer == null || buffer?.width != w || buffer?.height != h) {
      buffer = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    }
    val g2 = buffer?.createGraphics() ?: return
    AbstractShimmerLabel.applyRenderingHints(label, g2)
    g2.composite = AlphaComposite.Clear
    g2.fillRect(0, 0, w, h)
    g2.composite = AlphaComposite.SrcOver

    val fm = g2.getFontMetrics(label.getFont())
    val x = textRect.x.toFloat()
    val y = (textRect.y + fm.ascent).toFloat()

    // 1. Draw the text outline to the shimmer buffer (to serve as a mask).
    g2.paint = label.getForeground()
    val frc = g2.fontRenderContext
    TextLayout(label.text, label.getFont(), frc).draw(g2, x, y)

    // 2. Compose the gradient using SrcAtop (masked by the text's alpha channel).
    g2.composite = AlphaComposite.SrcAtop
    val colors = ShimmerColors.gradientColors(label.getForeground(), ALPHA)
    val endX = animX + BAND_WIDTH
    g2.paint = LinearGradientPaint(animX, 0f, endX, 0f, fractions, colors)
    g2.fillRect(0, 0, w, h)
    g2.dispose()
  }

  companion object {
    private const val DELAY = 16
    private const val BAND_WIDTH = 100
    private const val SPEED = 4f
    private const val ALPHA = 180
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
