package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.RGBImageFilter
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.roundToInt

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  // symbol_scale_2.jpg: Real World Illustrator: Understanding 9-Slice Scaling
  // https://rwillustrator.blogspot.jp/2007/04/understanding-9-slice-scaling.html
  val img = makeBufferedImage(cl.getResource("example/symbol_scale_2.jpg"))
  val b1 = ScalingButton("Scaling", img)
  val b2 = NineSliceScalingButton("9-Slice Scaling", img)
  val p1 = JPanel(GridLayout(1, 2, 5, 5))
  p1.add(b1)
  p1.add(b2)

  val bi = makeBufferedImage(cl.getResource("example/blue.png"))
  val b3 = JButton("Scaling Icon", NineSliceScalingIcon(bi, 0, 0, 0, 0))
  b3.isContentAreaFilled = false
  b3.border = BorderFactory.createEmptyBorder()
  b3.foreground = Color.WHITE
  b3.horizontalTextPosition = SwingConstants.CENTER
  b3.pressedIcon = NineSliceScalingIcon(makeFilteredImage(bi, PressedImageFilter()), 0, 0, 0, 0)
  b3.rolloverIcon = NineSliceScalingIcon(makeFilteredImage(bi, RolloverImageFilter()), 0, 0, 0, 0)

  val b4 = JButton("9-Slice Scaling Icon", NineSliceScalingIcon(bi, 8, 8, 8, 8))
  b4.isContentAreaFilled = false
  b4.border = BorderFactory.createEmptyBorder()
  b4.foreground = Color.WHITE
  b4.horizontalTextPosition = SwingConstants.CENTER
  b4.pressedIcon = NineSliceScalingIcon(makeFilteredImage(bi, PressedImageFilter()), 8, 8, 8, 8)
  b4.rolloverIcon = NineSliceScalingIcon(makeFilteredImage(bi, RolloverImageFilter()), 8, 8, 8, 8)

  val p2 = JPanel(GridLayout(1, 2, 5, 5))
  p2.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p2.add(b3)
  p2.add(b4)

  return JPanel(BorderLayout()).also {
    it.add(p1)
    it.add(p2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeBufferedImage(url: URL?): BufferedImage {
  val ic = ImageIcon(url)
  val w = ic.iconWidth
  val h = ic.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  ic.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private fun makeFilteredImage(src: BufferedImage, filter: ImageFilter): BufferedImage {
  val ip = src.source
  val img = Toolkit.getDefaultToolkit().createImage(FilteredImageSource(ip, filter))
  val w = img.getWidth(null)
  val h = img.getHeight(null)
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g = bi.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  return bi
}

private class ScalingButton(title: String?, @field:Transient private val image: BufferedImage) : JButton() {
  init {
    setModel(DefaultButtonModel())
    init(title, null)
    isContentAreaFilled = false
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val bw = width
    val bh = height
    g2.drawImage(image, 0, 0, bw, bh, this)
    g2.dispose()
    super.paintComponent(g)
  }
}

private class NineSliceScalingButton(title: String?, @field:Transient private val image: BufferedImage) : JButton() {
  init {
    setModel(DefaultButtonModel())
    init(title, null)
    isContentAreaFilled = false
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val iw = image.getWidth(this)
    val ih = image.getHeight(this)
    val ww = width
    val hh = height
    val lw = 37
    val rw = 36
    val th = 36
    val bh = 36
    g2.drawImage(image.getSubimage(lw, th, iw - lw - rw, ih - th - bh), lw, th, ww - lw - rw, hh - th - bh, this)
    g2.drawImage(image.getSubimage(lw, 0, iw - lw - rw, th), lw, 0, ww - lw - rw, th, this)
    g2.drawImage(image.getSubimage(lw, ih - bh, iw - lw - rw, bh), lw, hh - bh, ww - lw - rw, bh, this)
    g2.drawImage(image.getSubimage(0, th, lw, ih - th - bh), 0, th, lw, hh - th - bh, this)
    g2.drawImage(image.getSubimage(iw - rw, th, rw, ih - th - bh), ww - rw, th, rw, hh - th - bh, this)
    g2.drawImage(image.getSubimage(0, 0, lw, th), 0, 0, this)
    g2.drawImage(image.getSubimage(iw - rw, 0, rw, th), ww - rw, 0, this)
    g2.drawImage(image.getSubimage(0, ih - bh, lw, bh), 0, hh - bh, this)
    g2.drawImage(image.getSubimage(iw - rw, ih - bh, rw, bh), ww - rw, hh - bh, this)
    g2.dispose()
    super.paintComponent(g)
  }
}

private class NineSliceScalingIcon(
  private val image: BufferedImage,
  private val lw: Int,
  private val rw: Int,
  private val th: Int,
  private val bh: Int
) : Icon {
  private var width = 0
  private var height = 0

  override fun getIconWidth() = width

  override fun getIconHeight() = image.getHeight(null).coerceAtLeast(height)

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    RECT.bounds = c.bounds
    SwingUtilities.calculateInnerArea(c as? JComponent, RECT)
    width = RECT.width
    height = RECT.height
    val iw = image.getWidth(c)
    val ih = image.getHeight(c)
    g2.drawImage(image.getSubimage(lw, th, iw - lw - rw, ih - th - bh), lw, th, width - lw - rw, height - th - bh, c)
    // if (lw > 0 && rw > 0 && th > 0 && bh > 0) {
    if (listOf(lw, rw, th, bh).filterNot { it > 0 }.isEmpty()) {
      g2.drawImage(image.getSubimage(lw, 0, iw - lw - rw, th), lw, 0, width - lw - rw, th, c)
      g2.drawImage(image.getSubimage(lw, ih - bh, iw - lw - rw, bh), lw, height - bh, width - lw - rw, bh, c)
      g2.drawImage(image.getSubimage(0, th, lw, ih - th - bh), 0, th, lw, height - th - bh, c)
      g2.drawImage(image.getSubimage(iw - rw, th, rw, ih - th - bh), width - rw, th, rw, height - th - bh, c)
      g2.drawImage(image.getSubimage(0, 0, lw, th), 0, 0, c)
      g2.drawImage(image.getSubimage(iw - rw, 0, rw, th), width - rw, 0, c)
      g2.drawImage(image.getSubimage(0, ih - bh, lw, bh), 0, height - bh, c)
      g2.drawImage(image.getSubimage(iw - rw, ih - bh, rw, bh), width - rw, height - bh, c)
    }
    g2.dispose()
  }

  companion object {
    private val RECT = Rectangle()
  }
}

private class PressedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = ((argb shr 16 and 0xFF) * .6f).roundToInt()
    return argb and 0xFF_00_FF_FF.toInt() or (r shl 16)
  }
}

private class RolloverImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int { // int r = (argb >> 16) & 0xFF;
    val g = 0xFF.coerceAtMost(((argb shr 8 and 0xFF) * 1.5f).roundToInt())
    val b = 0xFF.coerceAtMost(((argb and 0xFF) * 1.5f).roundToInt())
    return argb and 0xFF_FF_00_00.toInt() or (g shl 8) or b
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
