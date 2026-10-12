package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.*

fun createUI(): Component {
  val path = "example/test.jpg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val image = url?.let(::readImage) ?: createMissingImage()
  val width = image.width
  val height = image.height
  val shape = RoundRectangle2D.Float(0f, 0f, width / 2f, height / 2f, 50f, 50f)
  val clippedImage = createClippedImage(image, shape)

  val button1 = JButton("clipped window")
  button1.addActionListener { e ->
    JWindow().also {
      it.contentPane.add(createPanel(image))
      it.shape = shape
      it.pack()
      it.setLocationRelativeTo((e.source as? JComponent)?.rootPane)
      it.isVisible = true
    }
  }

  val button2 = JButton("soft clipped window")
  button2.addActionListener { e ->
    JWindow().also {
      it.contentPane.add(createPanel(clippedImage))
      if (it.graphicsConfiguration?.isTranslucencyCapable == true) {
        it.background = Color(0x0, true)
      }
      it.pack()
      it.setLocationRelativeTo((e.source as? JComponent)?.rootPane)
      it.isVisible = true
    }
  }

  return JPanel().also {
    it.add(button1)
    it.add(button2)
    it.preferredSize = Dimension(320, 240)
  }
}

// ImageIO.read(...) returns null if no registered ImageReader can decode the stream
private fun readImage(url: URL) = runCatching {
  url.openStream().use(ImageIO::read)
}.getOrNull() ?: createMissingImage()

private fun createPanel(image: BufferedImage): Component {
  val panel = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = Dimension(image.width / 2, image.height / 2)

    override fun paintComponent(g: Graphics) {
      g.drawImage(image, 0, 0, this)
      super.paintComponent(g)
    }
  }

  val dwl = DragWindowListener()
  panel.addMouseListener(dwl)
  panel.addMouseMotionListener(dwl)

  val close = JButton("close")
  close.addActionListener { e ->
    (e.source as? Component)?.also {
      SwingUtilities.getWindowAncestor(it)?.dispose()
    }
  }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(2, 0, 10, 30)
    it.add(Box.createHorizontalGlue())
    it.add(close)
    // it.setOpaque(false)
  }

  panel.add(box, BorderLayout.SOUTH)
  panel.isOpaque = false
  return panel
}

// @see https://community.oracle.com/blogs/campbell/2006/07/19/java-2d-trickery-soft-clipping
// campbell: Java 2D Trickery: Soft Clipping Blog | Oracle Community
private fun createClippedImage(source: BufferedImage, shape: Shape): BufferedImage {
  val width = source.width
  val height = source.height

  val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  // A newly created TYPE_INT_ARGB image is already fully transparent,
  // so there is no need to clear it with AlphaComposite.Clear.
  val g2 = image.createGraphics()
  g2.composite = AlphaComposite.Src
  g2.setRenderingHint(
    RenderingHints.KEY_ANTIALIASING,
    RenderingHints.VALUE_ANTIALIAS_ON,
  )
  // Only the alpha channel of the (antialiased) shape matters here.
  g2.fill(shape)

  // Draw the source image only where the destination is opaque (soft clipping)
  g2.composite = AlphaComposite.SrcAtop
  g2.drawImage(source, 0, 0, null)
  g2.dispose()

  return image
}

private fun createMissingImage(): BufferedImage {
  val bi = BufferedImage(320, 240, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  g2.paint = Color.RED
  g2.fillRect(0, 0, 320, 240)
  g2.dispose()
  return bi
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()

  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val window = SwingUtilities.getRoot(e.component) as? Window ?: return
    if (SwingUtilities.isLeftMouseButton(e)) {
      val pt = window.location
      window.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
