package example

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.border.Border

private val TEXTURE = makeTexturePaint()
private var openFrameCount = 0
private val desktop = JDesktopPane()

fun makeUI(): Component {
  val p1 = JPanel()
  p1.isOpaque = false

  val p2 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      // super.paintComponent(g)
      g.color = Color(0x64_64_32_32, true)
      g.fillRect(0, 0, width, height)
    }
  }

  val p3 = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = TEXTURE
      g2.fillRect(0, 0, width, height)
      g2.dispose()
    }
  }
  desktop.add(createFrame(p1))
  desktop.add(createFrame(p2))
  desktop.add(createFrame(p3))

  val path = "example/GIANT_TCR1_2013.jpg"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  desktop.border = CentredBackgroundBorder(image)

  val menu = JMenu("Frame")
  menu.mnemonic = KeyEvent.VK_D
  menu.add("New Frame").also {
    it.actionCommand = "new"
    it.mnemonic = KeyEvent.VK_N
    it.accelerator = KeyStroke.getKeyStroke("alt N")
    it.addActionListener { desktop.add(createFrame(null)) }
  }
  val menuBar = JMenuBar()
  menuBar.add(menu)

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(c: JComponent?): JInternalFrame {
  val title = "Frame #${++openFrameCount}"
  val frame = JInternalFrame(title, true, true, true, true)
  if (c is JPanel) {
    c.add(JLabel("label"))
    c.add(JButton("button"))
    frame.contentPane = c
  }
  frame.setSize(160, 100)
  frame.setLocation(30 * openFrameCount, 30 * openFrameCount)
  frame.isOpaque = false
  EventQueue.invokeLater { frame.isVisible = true }
  return frame
}

private fun makeTexturePaint(): TexturePaint {
  val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = img.createGraphics()
  g2.paint = Color(0x64_64_78_64, true)
  g2.fillRect(0, 0, 16, 16)
  val cs = 4
  var i = 0
  while (i * cs < 16) {
    var j = 0
    while (j * cs < 16) {
      if ((i + j) % 2 == 0) {
        g2.fillRect(i * cs, j * cs, cs, cs)
      }
      j++
    }
    i++
  }
  g2.dispose()
  return TexturePaint(img, Rectangle(16, 16))
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

private class CentredBackgroundBorder(
  private val image: BufferedImage,
) : Border {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val cx = (width - image.width) / 2.0
    val cy = (height - image.height) / 2.0
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.drawRenderedImage(image, AffineTransform.getTranslateInstance(cx, cy))
    g2.dispose()
  }

  override fun getBorderInsets(c: Component) = Insets(0, 0, 0, 0)

  override fun isBorderOpaque() = true
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
