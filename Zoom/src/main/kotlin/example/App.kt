package example

import java.awt.*
import java.awt.event.MouseWheelListener
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val url = cl.getResource("example/test.png")
  val icon = url?.openStream()?.use(ImageIO::read)?.let { ImageIcon(it) } ?: MissingIcon()
  val zoom = ZoomImage(icon)

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

private class ZoomImage(
  private val icon: Icon,
) : JPanel() {
  private var handler: MouseWheelListener? = null
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
    icon.paintIcon(this, g2, 0, 0)
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
