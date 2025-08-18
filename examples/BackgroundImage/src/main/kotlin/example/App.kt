package example

import java.awt.*
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val img = Thread
    .currentThread()
    .contextClassLoader
    .getResource("example/16x16.png")
    ?.openStream()
    ?.use(ImageIO::read)
    ?: makeMissingImage()
  val p = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      val d = size
      val w = img.width
      val h = img.height
      var i = 0
      while (i * w < d.width) {
        var j = 0
        while (j * h < d.height) {
          g.drawImage(img, i * w, j * h, w, h, this)
          j++
        }
        i++
      }
      super.paintComponent(g)
    }
  }
  p.add(JLabel("BackgroundImage"))
  p.isOpaque = false
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
  g2.dispose()
  return bi
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
