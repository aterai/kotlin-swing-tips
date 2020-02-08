package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

class MainPanel : JPanel(BorderLayout()) {
  private val desktop = JDesktopPane()

  init {
    val p1 = JPanel()
    p1.isOpaque = false
    val p2: JPanel = object : JPanel() {
      override fun paintComponent(g: Graphics) { // super.paintComponent(g);
        g.color = Color(0x64643232, true)
        g.fillRect(0, 0, width, height)
      }
    }
    val p3: JPanel = object : JPanel() {
      override fun paintComponent(g: Graphics) {
        val g2 = g.create() as Graphics2D
        g2.paint = TEXTURE
        g2.fillRect(0, 0, width, height)
        g2.dispose()
      }
    }
    desktop.add(createFrame(p1))
    desktop.add(createFrame(p2))
    desktop.add(createFrame(p3))

    val image = runCatching { ImageIO.read(javaClass.getResource("tokeidai.jpg")) }
      .onFailure { it.printStackTrace() }
      .getOrNull() ?: makeMissingImage()
    desktop.border = CentredBackgroundBorder(image)
    add(desktop)
    add(createMenuBar(), BorderLayout.NORTH)
    preferredSize = Dimension(320, 240)
  }

  private fun createMenuBar(): JMenuBar {
    val menu = JMenu("Frame")
    menu.mnemonic = KeyEvent.VK_D
    val menuItem = menu.add("New Frame")
    menuItem.mnemonic = KeyEvent.VK_N
    menuItem.accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.ALT_DOWN_MASK)
    menuItem.actionCommand = "new"
    menuItem.addActionListener { createFrame(null) }
    val menuBar = JMenuBar()
    menuBar.add(menu)
    return menuBar
  }

  companion object {
    val TEXTURE: Paint = makeTexturePaint()
    private var openFrameCount = 0

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
      frame.isVisible = true
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
  }

  private fun makeMissingImage(): BufferedImage {
    val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
    val w = missingIcon.iconWidth
    val h = missingIcon.iconHeight
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    return bi
  }
}

class CentredBackgroundBorder(private val image: BufferedImage) : Border {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
