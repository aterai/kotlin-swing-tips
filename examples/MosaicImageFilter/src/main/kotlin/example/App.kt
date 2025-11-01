package example

import java.awt.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.BufferedImageFilter
import java.awt.image.BufferedImageOp
import java.awt.image.ColorModel
import java.awt.image.FilteredImageSource
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
  split.isContinuousLayout = true
  split.resizeWeight = .5

  val path = "example/test.jpg"
  val cl = Thread.currentThread().contextClassLoader
  val img =
    cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val imageIcon1 = ImageIcon(img)

  val beforeCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      imageIcon1.paintIcon(this, g, 0, 0)
    }
  }
  split.leftComponent = beforeCanvas

  val filter = BufferedImageFilter(MosaicImageFilter(16))
  val producer = FilteredImageSource(img.source, filter)
  val result = Toolkit.getDefaultToolkit().createImage(producer)
  val imageIcon2 = ImageIcon(result)
  val afterCanvas = object : JComponent() {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      g.translate(-location.x + split.insets.left, 0)
      imageIcon2.paintIcon(this, g, 0, 0)
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
    val srcRaster = src.raster
    val img = dst ?: createCompatibleDestImage(src, null)
    val dstRaster = img.raster
    val pixels = IntArray(blockSize * blockSize)
    var y = 0
    while (y < height) {
      var x = 0
      while (x < width) {
        val w = minOf(blockSize, width - x)
        val h = minOf(blockSize, height - y)
        srcRaster.getDataElements(x, y, w, h, pixels)
        updatePixels(w, h, pixels)
        dstRaster.setDataElements(x, y, w, h, pixels)
        x += blockSize
      }
      y += blockSize
    }
    return img
  }

  override fun getBounds2D(src: BufferedImage) = null

  override fun createCompatibleDestImage(src: BufferedImage, dstCm: ColorModel?) =
    BufferedImage(src.width, src.height, BufferedImage.TYPE_INT_ARGB)

  override fun getPoint2D(srcPt: Point2D, dstPt: Point2D) = null

  override fun getRenderingHints() = RenderingHints(
    emptyMap<RenderingHints.Key, Any>(),
  )

  companion object {
    fun getBlockRgb(w: Int, h: Int, pixels: IntArray): Int {
      var r = 0
      var g = 0
      var b = 0
      for (by in 0..<h) {
        for (bx in 0..<w) {
          val argb = pixels[bx + by * w]
          r += argb shr 16 and 0xFF
          g += argb shr 8 and 0xFF
          b += argb and 0xFF
        }
      }
      val size = w * h
      r = r / size shl 16
      g = g / size shl 8
      b /= size
      return r or g or b
    }

    fun updatePixels(w: Int, h: Int, pixels: IntArray) {
      val rgb = getBlockRgb(w, h, pixels)
      for (by in 0..<h) {
        for (bx in 0..<w) {
          val i = bx + by * w
          pixels[i] = pixels[i] and 0xFF_00_00_00.toInt() or rgb
        }
      }
    }
  }
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
