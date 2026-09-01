package example

import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.math.roundToInt

fun createUI(): Component {
  // symbol_scale_2.jpg: Real World Illustrator: Understanding 9-Slice Scaling
  // https://rwillustrator.blogspot.jp/2007/04/understanding-9-slice-scaling.html
  val img = createBufferedImage("example/symbol_scale_2.jpg")
  val b1 = ScalingButton("Scaling", img)
  val b2 = NineSliceScalingButton("9-Slice Scaling", img)
  val p1 = JPanel(GridLayout(1, 2, 5, 5))
  p1.add(b1)
  p1.add(b2)

  val bi = createBufferedImage("example/blue.png")
  val b3 = JButton("Scaling Icon", NineSliceScalingIcon(bi, 0, 0, 0, 0))
  b3.isContentAreaFilled = false
  b3.border = BorderFactory.createEmptyBorder()
  b3.foreground = Color.WHITE
  b3.horizontalTextPosition = SwingConstants.CENTER
  val pressedImg = createFilteredImage(bi, PressedImageFilter())
  b3.pressedIcon = NineSliceScalingIcon(pressedImg, 0, 0, 0, 0)
  val rolloverImg = createFilteredImage(bi, RolloverImageFilter())
  b3.rolloverIcon = NineSliceScalingIcon(rolloverImg, 0, 0, 0, 0)

  val b4 = JButton("9-Slice Scaling Icon", NineSliceScalingIcon(bi, 8, 8, 8, 8))
  b4.isContentAreaFilled = false
  b4.border = BorderFactory.createEmptyBorder()
  b4.foreground = Color.WHITE
  b4.horizontalTextPosition = SwingConstants.CENTER
  b4.pressedIcon = NineSliceScalingIcon(pressedImg, 8, 8, 8, 8)
  b4.rolloverIcon = NineSliceScalingIcon(rolloverImg, 8, 8, 8, 8)

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

private fun createBufferedImage(path: String): BufferedImage {
  val cl = Thread.currentThread().contextClassLoader
  return cl.getResource(path)?.openStream()?.use { ImageIO.read(it) }
    ?: createMissingImage()
}

private fun createMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(124, 124, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (124 - iw) / 2, (124 - ih) / 2)
  g2.dispose()
  return bi
}

private fun createFilteredImage(
  src: BufferedImage,
  filter: ImageFilter,
): BufferedImage {
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

private class ScalingButton(
  title: String?,
  private val image: BufferedImage,
) : JButton() {
  init {
    setModel(DefaultButtonModel())
    init(title, null)
    isContentAreaFilled = false
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val bw = width
    val bh = height
    g2.drawImage(image, 0, 0, bw, bh, this)
    g2.dispose()
    super.paintComponent(g)
  }
}

private class NineSliceScalingButton(
  title: String?,
  private val image: BufferedImage,
) : JButton() {
  init {
    setModel(DefaultButtonModel())
    init(title, null)
    isContentAreaFilled = false
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val iw = image.getWidth(this)
    val ih = image.getHeight(this)
    val bw = width
    val bh = height
    val left = 37
    val right = 36
    val top = 36
    val bottom = 36
    val sub1 = image.getSubimage(left, top, iw - left - right, ih - top - bottom)
    g2.drawImage(sub1, left, top, bw - left - right, bh - top - bottom, this)
    val sub2 = image.getSubimage(left, 0, iw - left - right, top)
    g2.drawImage(sub2, left, 0, bw - left - right, top, this)
    val sub3 = image.getSubimage(left, ih - bottom, iw - left - right, bottom)
    g2.drawImage(sub3, left, bh - bottom, bw - left - right, bottom, this)
    val sub4 = image.getSubimage(0, top, left, ih - top - bottom)
    g2.drawImage(sub4, 0, top, left, bh - top - bottom, this)
    val sub5 = image.getSubimage(iw - right, top, right, ih - top - bottom)
    g2.drawImage(sub5, bw - right, top, right, bh - top - bottom, this)
    val sub6 = image.getSubimage(0, 0, left, top)
    g2.drawImage(sub6, 0, 0, this)
    val sub7 = image.getSubimage(iw - right, 0, right, top)
    g2.drawImage(sub7, bw - right, 0, this)
    val sub8 = image.getSubimage(0, ih - bottom, left, bottom)
    g2.drawImage(sub8, 0, bh - bottom, this)
    val sub9 = image.getSubimage(iw - right, ih - bottom, right, bottom)
    g2.drawImage(sub9, bw - right, bh - bottom, this)
    g2.dispose()
    super.paintComponent(g)
  }
}

private class NineSliceScalingIcon(
  private val image: BufferedImage,
  private val left: Int,
  private val right: Int,
  private val top: Int,
  private val bottom: Int,
) : Icon {
  private val innerArea = Rectangle()
  private var width = 0
  private var height = 0

  override fun getIconWidth() = width

  override fun getIconHeight() = image.getHeight(null).coerceAtLeast(height)

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    innerArea.bounds = c.bounds
    SwingUtilities.calculateInnerArea(c as? JComponent, innerArea)
    width = innerArea.width
    height = innerArea.height
    val iw = image.getWidth(c)
    val ih = image.getHeight(c)
    val sub = image.getSubimage(left, top, iw - left - right, ih - top - bottom)
    g2.drawImage(sub, left, top, width - left - right, height - top - bottom, c)
    // if (left > 0 && right > 0 && top > 0 && bottom > 0) {
    if (listOf(left, right, top, bottom).filterNot { it > 0 }.isEmpty()) {
      val sub1 = image.getSubimage(left, 0, iw - left - right, top)
      g2.drawImage(sub1, left, 0, width - left - right, top, c)
      val sub2 = image.getSubimage(left, ih - bottom, iw - left - right, bottom)
      g2.drawImage(sub2, left, height - bottom, width - left - right, bottom, c)
      val sub3 = image.getSubimage(0, top, left, ih - top - bottom)
      g2.drawImage(sub3, 0, top, left, height - top - bottom, c)
      val sub4 = image.getSubimage(iw - right, top, right, ih - top - bottom)
      g2.drawImage(sub4, width - right, top, right, height - top - bottom, c)
      val sub5 = image.getSubimage(0, 0, left, top)
      g2.drawImage(sub5, 0, 0, c)
      val sub6 = image.getSubimage(iw - right, 0, right, top)
      g2.drawImage(sub6, width - right, 0, c)
      val sub7 = image.getSubimage(0, ih - bottom, left, bottom)
      g2.drawImage(sub7, 0, height - bottom, c)
      val sub8 = image.getSubimage(iw - right, ih - bottom, right, bottom)
      g2.drawImage(sub8, width - right, height - bottom, c)
    }
    g2.dispose()
  }
}

private class PressedImageFilter : RGBImageFilter() {
  override fun filterRGB(
    x: Int,
    y: Int,
    argb: Int,
  ): Int {
    val r = ((argb shr 16 and 0xFF) * .6f).roundToInt()
    return argb and 0xFF_00_FF_FF.toInt() or (r shl 16)
  }
}

private class RolloverImageFilter : RGBImageFilter() {
  override fun filterRGB(
    x: Int,
    y: Int,
    argb: Int,
  ): Int {
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
