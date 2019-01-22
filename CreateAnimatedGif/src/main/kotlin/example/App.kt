package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import java.io.File
import java.io.IOException
import java.util.Collections
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import javax.imageio.metadata.IIOMetadata
import javax.imageio.metadata.IIOMetadataNode
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  private val DELAY = 10
  private val ELLIPSE_COLOR = Color(.5f, .5f, .5f)
  private val R = 20.0
  private val SX = 20.0
  private val SY = 20.0
  private val WIDTH = (R * 8 + SX * 2).toInt()
  private val HEIGHT = (R * 8 + SY * 2).toInt()
  private val list = mutableListOf<Shape>(
      Ellipse2D.Double(SX + 3 * R, SY + 0 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 5 * R, SY + 1 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 6 * R, SY + 3 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 5 * R, SY + 5 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 3 * R, SY + 6 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 1 * R, SY + 5 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 0 * R, SY + 3 * R, 2 * R, 2 * R),
      Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R))

  init {
    val label = JLabel()
    label.setOpaque(true)
    label.setBackground(Color.WHITE)
    label.setVerticalTextPosition(SwingConstants.TOP)
    label.setHorizontalAlignment(SwingConstants.CENTER)
    label.setHorizontalTextPosition(SwingConstants.CENTER)

    // File file = new File(System.getProperty("user.dir"), "anime.gif");
    val button = JButton("make")
    button.addActionListener {
      val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB)
      val ite = ImageIO.getImageWritersByFormatName("gif")
      try {
        val writer = if (ite.hasNext()) {
          ite.next()
        } else {
          throw IOException()
        }

        val file = File.createTempFile("anime", ".gif")
        file.deleteOnExit()
        val stream = ImageIO.createImageOutputStream(file)
        writer.setOutput(stream)
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
        // indicates the the number of times to loop.
        // 0 means loop forever.
        ae.setUserObject(byteArrayOf(0x1, 0x0, 0x0))

        val aes = IIOMetadataNode("ApplicationExtensions")
        aes.appendChild(ae)

        // Create animated GIF using imageio | Oracle Community
        // https://community.oracle.com/thread/1264385
        val iwp = writer.getDefaultWriteParam()
        var metadata: IIOMetadata = writer.getDefaultImageMetadata(ImageTypeSpecifier(image), iwp)
        val metaFormat = metadata.getNativeMetadataFormatName()
        val root = metadata.getAsTree(metaFormat)
        root.appendChild(gce)
        root.appendChild(aes)
        metadata.setFromTree(metaFormat, root)

        // make frame
        for (i in 0 until list.size * DELAY) {
          paintFrame(image, list)
          Collections.rotate(list, 1)
          writer.writeToSequence(IIOImage(image, null, metadata), null)
          // metadata = null
        }
        writer.endWriteSequence()
        stream.close()

        val path = file.getAbsolutePath()
        label.setText(path)
        label.setIcon(ImageIcon(path))
      } catch (ex: IOException) {
        ex.printStackTrace()
      }
    }

    add(label)
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun paintFrame(image: BufferedImage, list: List<Shape>) {
    val g2 = image.createGraphics() as Graphics2D
    g2.setPaint(Color.WHITE)
    g2.fillRect(0, 0, WIDTH, HEIGHT)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(ELLIPSE_COLOR)
    val size = list.size.toFloat()
    list.forEach { s ->
      val alpha = (list.indexOf(s) + 1) / size
      g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha))
      g2.fill(s)
    }
    g2.dispose()
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
