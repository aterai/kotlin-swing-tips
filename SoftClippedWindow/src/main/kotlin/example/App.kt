package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val image = runCatching {
    ImageIO.read(cl.getResource("example/test.jpg"))
  }.getOrNull() ?: makeMissingImage()

  val width = image.width
  val height = image.height
  val shape = RoundRectangle2D.Float(0f, 0f, width / 2f, height / 2f, 50f, 50f)

  val clippedImage = makeClippedImage(image, shape)

  val button1 = JButton("clipped window")
  button1.addActionListener { e ->
    JWindow().also {
      it.contentPane.add(makePanel(image))
      it.shape = shape
      it.pack()
      it.setLocationRelativeTo((e.source as? JComponent)?.rootPane)
      it.isVisible = true
    }
  }

  val button2 = JButton("soft clipped window")
  button2.addActionListener { e ->
    JWindow().also {
      it.contentPane.add(makePanel(clippedImage))
      it.background = Color(0x0, true)
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

private fun makePanel(image: BufferedImage): Component {
  val panel = object : JPanel(BorderLayout()) {
    override fun getPreferredSize() = Dimension(image.getWidth(this) / 2, image.getHeight(this) / 2)

    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.drawImage(image, 0, 0, this)
      g2.dispose()
      super.paintComponent(g)
    }
  }

  val dwl = DragWindowListener()
  panel.addMouseListener(dwl)
  panel.addMouseMotionListener(dwl)

  val close = JButton("close")
  close.addActionListener { e ->
    (e.source as? Component)?.also { SwingUtilities.getWindowAncestor(it)?.dispose() }
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
private fun makeClippedImage(source: BufferedImage, shape: Shape): BufferedImage {
  val width = source.width
  val height = source.height

  val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
  val g2 = image.createGraphics()
  // g2.setComposite(AlphaComposite.Clear)
  // g2.fillRect(0, 0, width, height)

  g2.composite = AlphaComposite.Src
  g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
  // g2.setColor(Color.WHITE)
  g2.fill(shape)

  g2.composite = AlphaComposite.SrcAtop
  g2.drawImage(source, 0, 0, null)
  g2.dispose()

  return image
}

private fun makeMissingImage(): BufferedImage {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
