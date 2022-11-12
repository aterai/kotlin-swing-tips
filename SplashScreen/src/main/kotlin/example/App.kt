package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.BufferedImage
import java.lang.invoke.MethodHandles
import java.util.logging.ConsoleHandler
import java.util.logging.Logger
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

private val logger = Logger.getLogger(MethodHandles.lookup().lookupClass().name)

fun makeUI(): Component {
  logger.addHandler(ConsoleHandler())
  runCatching {
    Thread.sleep(5000)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(JTree()))
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
    g2.paint = Color.WHITE
    g2.fillRect(x, y, w, h)
    g2.paint = Color.RED
    g2.stroke = BasicStroke(w / 8f)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + h - gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
}

fun main() {
  runCatching {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
  }.onFailure {
    it.printStackTrace()
    Toolkit.getDefaultToolkit().beep()
  }

  val splashScreen = JWindow()
  EventQueue.invokeLater {
    logger.info { "splashScreen show start / EDT: " + EventQueue.isDispatchThread() }
    val cl = Thread.currentThread().contextClassLoader
    val url = cl.getResource("example/splash.png")
    val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
    splashScreen.contentPane.add(JLabel(ImageIcon(img)))
    splashScreen.pack()
    splashScreen.setLocationRelativeTo(null)
    splashScreen.isVisible = true
    logger.info { "splashScreen show end" }
  }
  logger.info { "createGUI start / EDT: " + EventQueue.isDispatchThread() }

  val frame = JFrame().apply {
    defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
    contentPane.add(makeUI())
    pack()
    setLocationRelativeTo(null)
    // isVisible = true
  }
  logger.info { "createGUI end" }

  EventQueue.invokeLater {
    logger.info { "  splashScreen dispose start / EDT: " + EventQueue.isDispatchThread() }
    splashScreen.dispose()
    logger.info { "  splashScreen dispose end" }
    logger.info { "  frame show start / EDT: " + EventQueue.isDispatchThread() }
    frame.isVisible = true
    logger.info { "  frame show end" }
  }
}
