package example

import java.awt.*
import java.io.IOException
import javax.imageio.ImageIO
import javax.imageio.ImageReader
import javax.imageio.stream.ImageInputStream
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/duke.running.gif")
  val icon = url?.let { ImageIcon(it) } ?: UIManager.getIcon("html.missingImage")
  val label = JLabel(icon)
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
private fun loadFromStream(stream: ImageInputStream): List<Image> {
  val reader = ImageIO.getImageReaders(stream).asSequence().first { checkGifFormat(it) }
    ?: throw IOException("Can not read image format!")
  reader.setInput(stream, false, false)
  val list = ArrayList<Image>()
  for (i in 0..<reader.getNumImages(true)) {
    (reader.readAll(i, null).renderedImage as? Image)?.also { list.add(it) }
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
