package example

import java.awt.*
import java.awt.geom.Point2D
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageFilter
import java.awt.image.BufferedImageOp
import java.awt.image.ColorModel
import java.awt.image.FilteredImageSource
import javax.imageio.ImageIO
import javax.swing.*

fun createUI(): Component {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.isContinuousLayout = true
  split.resizeWeight = .5

  val path = "example/test.jpg"
  val cl = Thread.currentThread().contextClassLoader
  val img =
    cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val sourceIcon = ImageIcon(img)

  val beforeCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      sourceIcon.paintIcon(this, g, 0, 0)
    }
  }
  split.leftComponent = beforeCanvas

  val filter = BufferedImageFilter(MosaicImageFilter(16))
  val producer = FilteredImageSource(img.source, filter)
  val mosaicImage = Toolkit.getDefaultToolkit().createImage(producer)
  val mosaicIcon = ImageIcon(mosaicImage)
  val afterCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.translate(-location.x + split.insets.left, 0)
      mosaicIcon.paintIcon(this, g, 0, 0)
    }
  }
  split.rightComponent = afterCanvas

  return JPanel(BorderLayout(5, 5)).also {
    it.isOpaque = false
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MosaicImageFilter(
  private val blockSize: Int,
) : BufferedImageOp {
  override fun filter(src: BufferedImage, dst: BufferedImage?): BufferedImage {
    val width = src.width
    val height = src.height
    val dest = dst ?: createCompatibleDestImage(src, null)
    val pixels = IntArray(blockSize * blockSize)
    var y = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        val w = minOf(blockSize, width - x)
        val h = minOf(blockSize, height - y)
        // getRGB/setRGB always use the default ARGB color model, so this works
        // for any image type (Raster#getDataElements(...) would require an
        // int-packed raster and could lose the alpha channel of TYPE_INT_RGB)
        src.getRGB(x, y, w, h, pixels, 0, w)
        updatePixels(w, h, pixels)
        dest.setRGB(x, y, w, h, pixels, 0, w)
        x += blockSize
      }
      y += blockSize
    }
    return dest
  }

  // the mosaic does not change the geometry of the image
  override fun getBounds2D(src: BufferedImage): Rectangle2D = src.raster.bounds

  override fun createCompatibleDestImage(src: BufferedImage, dstCm: ColorModel?) =
    BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)

  override fun getPoint2D(srcPt: Point2D, dstPt: Point2D?): Point2D {
    val pt = dstPt ?: srcPt.clone() as Point2D
    pt.setLocation(srcPt)
    return pt
  }

  override fun getRenderingHints() = RenderingHints(
    emptyMap<RenderingHints.Key, Any>(),
  )

  companion object {
    fun getBlockRgb(w: Int, h: Int, pixels: IntArray): Int {
      var r = 0
      var g = 0
      var b = 0
      val size = w * h
      for (i in 0..<size) {
        val argb = pixels[i]
        r += argb shr 16 and 0xFF
        g += argb shr 8 and 0xFF
        b += argb and 0xFF
      }
      r = r / size shl 16
      g = g / size shl 8
      b /= size
      return r or g or b
    }

    fun updatePixels(w: Int, h: Int, pixels: IntArray) {
      val rgb = getBlockRgb(w, h, pixels)
      // the block is stored contiguously (scansize == w), so a single loop is enough
      for (i in 0..<w * h) {
        pixels[i] = pixels[i] and 0xFF_00_00_00.toInt() or rgb
      }
    }
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
