package example

import java.awt.*
import java.awt.color.ColorSpace
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val path = "example/i03-10.gif"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val icon = ImageIcon(image)

  val p1 = JPanel(GridLayout(1, 2))
  p1.background = Color.WHITE
  p1.add(makeLabel(makeGrayIcon1(image), icon, "ColorConvertOp"))
  p1.add(makeLabel(makeGrayIcon2(image), icon, "TYPE_BYTE_GRAY"))

  val p3 = JPanel(GridLayout(1, 2))
  p3.add(makeLabel(makeGrayIcon4(image), icon, "GrayFilter(true, 50)"))
  p3.add(makeLabel(makeGrayIcon5(image), icon, "GrayImageFilter"))
  p3.background = Color.WHITE

  return JPanel(GridLayout(0, 1)).also {
    it.add(p1)
    it.add(makeLabel(makeGrayIcon3(image), icon, "GrayFilter.createDisabledImage"))
    it.add(p3)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private fun makeLabel(icon: Icon, orgIcon: Icon, str: String): JLabel {
  val label = JLabel(str, icon, SwingConstants.LEFT)
  val ml = object : MouseAdapter() {
    private var isGray = false
    override fun mouseClicked(e: MouseEvent) {
      (e.component as? JLabel)?.icon = if (isGray) icon else orgIcon
      isGray = isGray xor true
    }
  }
  label.addMouseListener(ml)
  return label
}

private fun makeGrayIcon1(img: Image): Icon {
  val w = img.getWidth(null)
  val h = img.getHeight(null)
  val source = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g = source.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  val ccOp = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
  return ImageIcon(ccOp.filter(source, null))
}

private fun makeGrayIcon2(img: Image): Icon {
  val w = img.getWidth(null)
  val h = img.getHeight(null)
  val destination = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
  val g = destination.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  return ImageIcon(destination)
}

private fun makeGrayIcon3(img: Image) = ImageIcon(GrayFilter.createDisabledImage(img))

private fun makeGrayIcon4(img: Image): Icon {
  val ip = FilteredImageSource(img.source, GrayFilter(true, 50))
  return ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
}

private fun makeGrayIcon5(img: Image): Icon {
  val ip = FilteredImageSource(img.source, GrayImageFilter())
  return ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
}

private class GrayImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    val b = argb and 0xFF
    val m = (2 * r + 4 * g + b) / 7
    return argb and 0xFF_00_00_00.toInt() or (m shl 16) or (m shl 8) or m
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
