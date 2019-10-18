package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

@Transient
val imageTexture = makeImageTexture()
@Transient
val checkerTexture = makeCheckerTexture()
@Transient
var texture: TexturePaint? = null

fun makeUI(): Component {
  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      texture?.also {
        val g2 = g.create() as? Graphics2D ?: return@also
        g2.setPaint(it)
        g2.fillRect(0, 0, getWidth(), getHeight())
        g2.dispose()
      }
      super.paintComponent(g)
    }
  }
  p.setBackground(Color(.5f, .8f, .5f, .5f))
  p.add(JLabel("JLabel: "))
  p.add(JTextField(10))
  p.add(JButton("JButton"))

  val model = arrayOf("Color(.5f, .8f, .5f, .5f)", "ImageTexturePaint", "CheckerTexturePaint")
  val combo = JComboBox<String>(model)
  combo.addItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      when (e.getItem()) {
        "ImageTexturePaint" -> {
          texture = imageTexture
          p.setOpaque(false)
        }
        "CheckerTexturePaint" -> {
          texture = checkerTexture
          p.setOpaque(false)
        }
        else -> {
          texture = null
          p.setOpaque(true)
        }
      }
      p.getRootPane().getContentPane().repaint()
    }
  }
  p.add(combo)
  p.setPreferredSize(Dimension(320, 240))
  return p
}

fun makeImageTexture(): TexturePaint {
  // unkaku_w.png http://www.viva-edo.com/komon/edokomon.html
  val bi = runCatching {
    val cl = Thread.currentThread().getContextClassLoader()
    ImageIO.read(cl.getResource("example/unkaku_w.png"))
  }.getOrNull() ?: makeMissingImage()
  return TexturePaint(bi, Rectangle(bi.getWidth(), bi.getHeight()))
}

fun makeMissingImage(): BufferedImage {
  val missingIcon = UIManager.getIcon("OptionPane.errorIcon")
  val w = missingIcon.getIconWidth()
  val h = missingIcon.getIconHeight()
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
  g2.setPaint(Color(200, 150, 100, 50))
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
      setBackground(Color(0x0, true))
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
