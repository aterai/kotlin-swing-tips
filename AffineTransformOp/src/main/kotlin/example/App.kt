package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  var mode = Flip.NONE
  private val image = runCatching { ImageIO.read(javaClass.getResource("test.jpg")) }
    .onFailure { it.printStackTrace() }
    .getOrNull() ?: makeMissingImage()
  private val bg = ButtonGroup()

  private val panel = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      g.setColor(getBackground())
      g.fillRect(0, 0, getWidth(), getHeight())
      val w = image.getWidth(this)
      val h = image.getHeight(this)
      when (mode) {
        Flip.VERTICAL -> {
          val at = AffineTransform.getScaleInstance(1.0, -1.0)
          at.translate(0.0, -h.toDouble())
          val g2 = g.create() as? Graphics2D ?: return
          g2.drawImage(image, at, this)
          g2.dispose()
        }
        Flip.HORIZONTAL -> {
          val at = AffineTransform.getScaleInstance(-1.0, 1.0)
          at.translate(-w.toDouble(), 0.0)
          val atOp = AffineTransformOp(at, null)
          g.drawImage(atOp.filter(image, null), 0, 0, w, h, this)
        }
        Flip.NONE -> g.drawImage(image, 0, 0, w, h, this)
      }
    }
  }

  init {
    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(JLabel("Flip: "))
    Flip.values().map { makeRadioButton(it) }
      .forEach {
        box.add(it)
        bg.add(it)
        box.add(Box.createHorizontalStrut(5))
      }
    add(panel)
    add(box, BorderLayout.SOUTH)
    setOpaque(false)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeRadioButton(f: Flip): JRadioButton {
    val rb = JRadioButton(f.toString(), f === Flip.NONE)
    rb.addActionListener {
      mode = f
      panel.repaint()
    }
    return rb
  }

  private fun makeMissingImage(): BufferedImage {
    val missingIcon: Icon = MissingIcon()
    val w = missingIcon.getIconWidth()
    val h = missingIcon.getIconHeight()
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, 0, 0)
    g2.dispose()
    return bi
  }
}

enum class Flip {
  NONE, VERTICAL, HORIZONTAL
}

class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = getIconWidth()
    val h = getIconHeight()
    val gap = w / 5
    g2.setColor(Color.WHITE)
    g2.fillRect(x, y, w, h)
    g2.setColor(Color.RED)
    g2.setStroke(BasicStroke(w / 8f))
    g2.drawLine(x + gap, y + gap, x + w - gap, y + h - gap)
    g2.drawLine(x + gap, y + gap, x + w - gap, y + gap)
    g2.dispose()
  }

  override fun getIconWidth() = 320

  override fun getIconHeight() = 240
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
