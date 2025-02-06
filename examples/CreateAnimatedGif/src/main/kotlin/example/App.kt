package example

import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import java.util.Collections
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadataNode
import javax.swing.*

private const val DELAY = 10
private const val R = 20.0
private const val SX = 20.0
private const val SY = 20.0
private const val WIDTH = (R * 8 + SX * 2).toInt()
private const val HEIGHT = (R * 8 + SY * 2).toInt()
private val ellipseColor = Color(.5f, .5f, .5f)
private val list = mutableListOf(
  Ellipse2D.Double(SX + 3 * R, SY + 0 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 5 * R, SY + 1 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 6 * R, SY + 3 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 5 * R, SY + 5 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 3 * R, SY + 6 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 1 * R, SY + 5 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 0 * R, SY + 3 * R, 2 * R, 2 * R),
  Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R),
)

fun makeLabel() = JLabel().also {
  it.isOpaque = true
  it.background = Color.WHITE
  it.verticalTextPosition = SwingConstants.TOP
  it.horizontalAlignment = SwingConstants.CENTER
  it.horizontalTextPosition = SwingConstants.CENTER
}

fun makeUI(): Component {
  val label = makeLabel()

  // val file = File(System.getProperty("user.dir"), "anime.gif")
  val button = JButton("make")
  button.addActionListener {
    makeAnimatedGif()?.absolutePath?.also {
      label.text = it
      label.icon = ImageIcon(it)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeAnimatedGif() = runCatching {
  val ite = ImageIO.getImageWritersByFormatName("gif")
  val writer = ite.next() ?: return null
  val file = File.createTempFile("anime", ".gif")
  file.deleteOnExit()
  ImageIO.createImageOutputStream(file).use {
    writer.output = it
    writer.prepareWriteSequence(null)

    val gce = IIOMetadataNode("GraphicControlExtension")
    gce.setAttribute("disposalMethod", "none")
    gce.setAttribute("userInputFlag", "FALSE")
    gce.setAttribute("transparentColorFlag", "FALSE")
    gce.setAttribute("transparentColorIndex", "0")
    gce.setAttribute("delayTime", DELAY.toString())

    val ae = IIOMetadataNode("ApplicationExtension")
    ae.setAttribute("applicationID", "NETSCAPE")
    ae.setAttribute("authenticationCode", "2.0")
    // last two bytes is an unsigned short (little endian) that
    // indicates the number of times to loop.
    // 0 means loop forever.
    ae.userObject = byteArrayOf(0x1, 0x0, 0x0)

    val aes = IIOMetadataNode("ApplicationExtensions")
    aes.appendChild(ae)

    // Create animated GIF using imageio | Oracle Community
    // https://community.oracle.com/thread/1264385
    val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
    val iwp = writer.defaultWriteParam
    val metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier(image), iwp)
    val metaFormat = metadata.nativeMetadataFormatName
    val root = metadata.getAsTree(metaFormat)
    root.appendChild(gce)
    root.appendChild(aes)
    metadata.setFromTree(metaFormat, root)

    // make frame
    repeat(list.size * DELAY) {
      paintFrame(image, list)
      Collections.rotate(list, 1)
      writer.writeToSequence(IIOImage(image, null, metadata), null)
      // metadata = null
    }
    writer.endWriteSequence()
  }
  file
}.getOrNull()

fun paintFrame(
  image: BufferedImage,
  list: List<Shape>,
) {
  val g2 = image.createGraphics()
  g2.paint = Color.WHITE
  g2.fillRect(0, 0, image.width, image.height)
  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  g2.paint = ellipseColor
  val size = list.size
  list.forEachIndexed { idx, shape ->
    val alpha = (idx + 1f) / size
    g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
    g2.fill(shape)
  }
  g2.dispose()
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
