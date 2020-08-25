package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.color.ColorSpace
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.image.ColorConvertOp
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val orgImage = ImageIcon(cl.getResource("example/i03-10.gif"))

  val p1 = JPanel(GridLayout(1, 2))
  p1.background = Color.WHITE
  p1.add(makeLabel(makeGrayImageIcon1(orgImage.image), orgImage, "ColorConvertOp"))
  p1.add(makeLabel(makeGrayImageIcon2(orgImage.image), orgImage, "TYPE_BYTE_GRAY"))

  val p3 = JPanel(GridLayout(1, 2))
  p3.add(makeLabel(makeGrayImageIcon4(orgImage.image), orgImage, "GrayFilter(true, 50)"))
  p3.add(makeLabel(makeGrayImageIcon5(orgImage.image), orgImage, "GrayImageFilter"))
  p3.background = Color.WHITE

  return JPanel(GridLayout(0, 1)).also {
    it.add(p1)
    it.add(makeLabel(makeGrayImageIcon3(orgImage.image), orgImage, "GrayFilter.createDisabledImage"))
    it.add(p3)
    it.background = Color.WHITE
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeLabel(image: ImageIcon, orgImage: ImageIcon, str: String): JLabel {
  val label = JLabel(str, image, SwingConstants.LEFT)
  val ml = object : MouseAdapter() {
    private var isGray = false
    override fun mouseClicked(e: MouseEvent) {
      (e.component as? JLabel)?.icon = if (isGray) image else orgImage
      isGray = isGray xor true
    }
  }
  label.addMouseListener(ml)
  return label
}

private fun makeGrayImageIcon1(img: Image): ImageIcon {
  val w = img.getWidth(null)
  val h = img.getHeight(null)
  val source = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g: Graphics = source.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  val ccOp = ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_GRAY), null)
  return ImageIcon(ccOp.filter(source, null))
}

private fun makeGrayImageIcon2(img: Image): ImageIcon {
  val w = img.getWidth(null)
  val h = img.getHeight(null)
  val destination = BufferedImage(w, h, BufferedImage.TYPE_BYTE_GRAY)
  val g: Graphics = destination.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  return ImageIcon(destination)
}

private fun makeGrayImageIcon3(img: Image) = ImageIcon(GrayFilter.createDisabledImage(img))

private fun makeGrayImageIcon4(img: Image): ImageIcon {
  val ip = FilteredImageSource(img.source, GrayFilter(true, 50))
  return ImageIcon(Toolkit.getDefaultToolkit().createImage(ip))
}

private fun makeGrayImageIcon5(img: Image): ImageIcon {
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
