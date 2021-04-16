package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.ByteLookupTable
import java.awt.image.LookupOp
import java.net.URL
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border

fun makeUI(): Component {
  val area = JTextArea()
  area.foreground = Color.WHITE
  area.background = Color(0x0, true) // Nimbus
  area.lineWrap = true
  area.isOpaque = false
  area.text = """
    private static void createAndShowGui() {
      JFrame frame = new JFrame();
      frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
      frame.getContentPane().add(new MainPanel());
      frame.pack();
      frame.setLocationRelativeTo(null);
      frame.setVisible(true);
    }
  """.trimIndent()

  val cl = Thread.currentThread().contextClassLoader
  val bi = getFilteredImage(cl.getResource("example/GIANT_TCR1_2013.jpg"))
  val scroll = JScrollPane(area)
  scroll.viewport.isOpaque = false
  scroll.viewportBorder = CentredBackgroundBorder(bi)
  scroll.verticalScrollBar.unitIncrement = 25

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getFilteredImage(url: URL?): BufferedImage {
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  val dest = BufferedImage(image.width, image.height, BufferedImage.TYPE_INT_RGB)
  val b = ByteArray(256)
  for (i in b.indices) {
    b[i] = (i * .2f).toInt().toByte()
  }
  val op = LookupOp(ByteLookupTable(0, b), null)
  op.filter(image, dest)
  return dest
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

private class CentredBackgroundBorder(private val image: BufferedImage) : Border {
  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
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
