package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/duke.running.gif")
  val label = JLabel()
  label.icon = ImageIcon(url)
  label.border = BorderFactory.createTitledBorder("duke.running.gif")

  val box = Box.createHorizontalBox()
  box.border = BorderFactory.createTitledBorder("Extract frames from Animated GIF")
  runCatching {
    ImageIO.createImageInputStream(url?.openStream()).use { iis ->
      loadFromStream(iis).forEach { box.add(JLabel(ImageIcon(it))) }
    }
  }.onFailure {
    label.text = it.message
  }

  return JPanel(BorderLayout()).also {
    it.add(label, BorderLayout.WEST)
    it.add(JScrollPane(box))
    it.preferredSize = Dimension(320, 240)
  }
}

@Throws(IOException::class)
private fun loadFromStream(imageStream: ImageInputStream): List<BufferedImage> {
  val reader = ImageIO.getImageReaders(imageStream).asSequence().first { checkGifFormat(it) }
    ?: throw IOException("Can not read image format!")
  reader.setInput(imageStream, false, false)
  val list = ArrayList<BufferedImage>()
  for (i in 0 until reader.getNumImages(true)) {
    val frame = reader.readAll(i, null)
    (frame.renderedImage as? BufferedImage)?.also {
      list.add(it)
    }
  }
  reader.dispose()
  return list
}

private fun checkGifFormat(reader: ImageReader): Boolean {
  val metaFormat = reader.originatingProvider.nativeImageMetadataFormatName
  val name = runCatching { reader.formatName }.getOrNull() ?: ""
  return "gif".equals(name, ignoreCase = true) && "javax_imageio_gif_image_1.0" == metaFormat
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
