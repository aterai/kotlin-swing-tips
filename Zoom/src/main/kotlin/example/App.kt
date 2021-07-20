package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseWheelListener
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val path = "example/test.png"
  val cl = Thread.currentThread().contextClassLoader
  val img = cl.getResource(path)?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val zoom = ZoomImage(img)

  val button1 = JButton("Zoom In")
  button1.addActionListener { zoom.changeScale(-5) }

  val button2 = JButton("Zoom Out")
  button2.addActionListener { zoom.changeScale(5) }

  val button3 = JButton("Original size")
  button3.addActionListener { zoom.initScale() }

  val box = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(button1)
    it.add(button2)
    it.add(button3)
  }

  return JPanel(BorderLayout()).also {
    it.add(zoom)
    it.add(box, BorderLayout.SOUTH)
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
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = iconWidth
    val h = iconHeight
    val gap = w / 5
    g2.color = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.color = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
}

private class ZoomImage(private val image: Image) : JPanel() {
  private var handler: MouseWheelListener? = null
  private val iw = image.getWidth(this)
  private val ih = image.getHeight(this)
  private var scale = 1.0

  override fun updateUI() {
    removeMouseWheelListener(handler)
    super.updateUI()
    handler = MouseWheelListener { changeScale(it.wheelRotation) }
    addMouseWheelListener(handler)
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.scale(scale, scale)
    g2.drawImage(image, 0, 0, iw, ih, this)
    g2.dispose()
  }

  fun initScale() {
    scale = 1.0
    repaint()
  }

  fun changeScale(iv: Int) {
    // scale = maxOf(.05, minOf(5.0, scale - iv * .05))
    scale = (scale - iv * .05).coerceIn(.05, 5.0)
    repaint()
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
