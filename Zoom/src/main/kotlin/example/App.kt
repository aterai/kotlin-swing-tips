package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseWheelListener
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/test.png"))
  val zoom = ZoomImage(icon.image)

  val button1 = JButton("Zoom In")
  button1.addActionListener { zoom.changeScale(-5) }

  val button2 = JButton("Zoom Out")
  button2.addActionListener { zoom.changeScale(5) }

  val button3 = JButton("Original size")
  button3.addActionListener { zoom.initScale() }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button1)
  box.add(button2)
  box.add(button3)

  return JPanel(BorderLayout()).also {
    it.add(zoom)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ZoomImage(@field:Transient private val image: Image) : JPanel() {
  @Transient
  private var handler: MouseWheelListener? = null
  private val iw = image.getWidth(this)
  private val ih = image.getHeight(this)
  private var scale = 1.0

  override fun updateUI() {
    removeMouseWheelListener(handler)
    super.updateUI()
    handler = MouseWheelListener { e -> changeScale(e.wheelRotation) }
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
