package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import java.awt.image.MemoryImageSource
import java.awt.image.PixelGrabber
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val imageA = makeImage("example/a.png")
  val imageB = makeImage("example/b.png")
  val iconA = ImageIcon(imageA)
  val iconB = ImageIcon(imageB)
  val label = JLabel(iconA)
  val w = iconA.iconWidth
  val h = iconA.iconHeight
  val pixelsA = getData(imageA, w, h)
  val pixelsB = getData(imageB, w, h)
  for (i in pixelsA.indices) {
    if (pixelsA[i] == pixelsB[i]) {
      pixelsA[i] = pixelsA[i] and 0x44_FF_FF_FF
    }
  }
  val ra = JRadioButton("a.png", true)
  ra.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = iconA
    }
  }
  val rb = JRadioButton("b.png")
  rb.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = iconB
    }
  }
  val p = JPanel()
  val source = MemoryImageSource(w, h, pixelsA, 0, w)
  val rr = JRadioButton("diff")
  rr.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      label.icon = ImageIcon(p.createImage(source))
    }
  }

  val bg = ButtonGroup()
  listOf(ra, rb, rr).forEach {
    bg.add(it)
    p.add(it)
  }

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(p, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getData(
  img: Image,
  w: Int,
  h: Int,
): IntArray {
  val image = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
  val g = image.createGraphics()
  g.drawImage(img, 0, 0, null)
  g.dispose()
  // return (image.raster.dataBuffer as? DataBufferInt)?.data ?: IntArray(0)
  val pixels = IntArray(w * h)
  val systemEventQueue = Toolkit.getDefaultToolkit().systemEventQueue
  val loop = systemEventQueue.createSecondaryLoop()
  val worker = Thread {
    runCatching {
      PixelGrabber(image, 0, 0, w, h, pixels, 0, w).grabPixels()
    }.onFailure {
      Thread.currentThread().interrupt()
    }
    loop.exit()
  }
  worker.start()
  loop.enter()
  return pixels
}

private fun makeImage(path: String): BufferedImage {
  val cl = Thread.currentThread().contextClassLoader
  return cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
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
