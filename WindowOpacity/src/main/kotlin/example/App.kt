package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

val imageTexture = makeImageTexture()
val checkerTexture = makeCheckerTexture()
var texture: TexturePaint? = null

fun makeUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      texture?.also {
        g2.paint = it
        g2.fillRect(0, 0, width, height)
      }
      g2.dispose()
      super.paintComponent(g)
    }
  }
  p.background = Color(.5f, .8f, .5f, .5f)
  p.add(JLabel("JLabel: "))
  p.add(JTextField(10))
  p.add(JButton("JButton"))

  val model = arrayOf(
    "Color(.5f, .8f, .5f, .5f)",
    "ImageTexturePaint",
    "CheckerTexturePaint",
  )
  val combo = JComboBox(model)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      when (e.item) {
        "ImageTexturePaint" -> {
          texture = imageTexture
          p.isOpaque = false
        }

        "CheckerTexturePaint" -> {
          texture = checkerTexture
          p.isOpaque = false
        }

        else -> {
          texture = null
          p.isOpaque = true
        }
      }
      p.rootPane.contentPane.repaint()
    }
  }
  p.add(combo)
  p.preferredSize = Dimension(320, 240)
  return p
}

fun makeImageTexture(): TexturePaint {
  // unkaku_w.png https://www.viva-edo.com/komon/edokomon.html
  val path = "example/unkaku_w.png"
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  val bi = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
  return TexturePaint(bi, Rectangle(bi.width, bi.height))
}

fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

fun makeCheckerTexture(): TexturePaint {
  val cs = 6
  val sz = cs * cs
  val bi = BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  g2.paint = Color(200, 150, 100, 50)
  g2.fillRect(0, 0, sz, sz)
  var i = 0
  while (i * cs < sz) {
    var j = 0
    while (j * cs < sz) {
      if ((i + j) % 2 == 0) {
        g2.fillRect(i * cs, j * cs, cs, cs)
      }
      j++
    }
    i++
  }
  g2.dispose()
  return TexturePaint(bi, Rectangle(sz, sz))
}

fun main() {
  EventQueue.invokeLater {
    JFrame.setDefaultLookAndFeelDecorated(true)
    JFrame().apply {
      if (graphicsConfiguration.isTranslucencyCapable) {
        background = Color(0x0, true)
      }
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
