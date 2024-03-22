package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Area
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import java.awt.image.ImageProducer
import java.awt.image.MemoryImageSource
import java.awt.image.PixelGrabber
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/screenshot.png")
  val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()

  val icon = ImageIcon(img)
  val width = icon.iconWidth
  val height = icon.iconHeight
  val bi = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

  val cardLayout = CardLayout()
  val p = JPanel(cardLayout)
  makeRoundedImageProducer(img, width, height)?.also {
    val g = bi.createGraphics()
    g.drawImage(p.createImage(it), 0, 0, p)
    g.dispose()
  }

  p.add(JLabel(ImageIcon(img)), "original")
  p.add(JLabel(ImageIcon(bi)), "rounded")

  val check = JCheckBox("transparency at the rounded windows corners")
  check.addActionListener {
    cardLayout.show(p, if (check.isSelected) "rounded" else "original")
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMissingImage(): Image {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private class MissingIcon : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.paint = Color.GRAY
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 240

  override fun getIconHeight() = 160
}

private fun makeNorthWestCorner() = Area().also {
  it.add(Area(Rectangle(0, 0, 5, 1)))
  it.add(Area(Rectangle(0, 1, 3, 1)))
  it.add(Area(Rectangle(0, 2, 2, 1)))
  it.add(Area(Rectangle(0, 3, 1, 2)))
}

private fun makeRoundedImageProducer(
  img: Image,
  w: Int,
  h: Int,
): ImageProducer? {
  val pix = IntArray(h * w)
  val pg = PixelGrabber(img, 0, 0, w, h, pix, 0, w)
  val ret = runCatching {
    pg.grabPixels()
  }.getOrNull() ?: false
  if (!ret || pg.status and ImageObserver.ABORT != 0) {
    System.err.println("image fetch aborted or error")
    return null
  }

  val area = makeNorthWestCorner()
  val r = area.bounds
  for (y in 0 until r.height) {
    for (x in 0 until r.width) {
      if (area.contains(Point(x, y))) {
        pix[x + y * w] = 0x0
      }
    }
  }
  val at = AffineTransform.getScaleInstance(-1.0, 1.0)
  at.translate(-w.toDouble(), 0.0)
  val s2 = at.createTransformedShape(area) // NE
  for (y in 0 until r.height) {
    for (x in w - r.width until w) {
      if (s2.contains(Point(x, y))) {
        pix[x + y * w] = 0x0
      }
    }
  }
  return MemoryImageSource(w, h, pix, 0, w)
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
