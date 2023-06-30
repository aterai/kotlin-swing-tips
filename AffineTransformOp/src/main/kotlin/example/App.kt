package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.geom.AffineTransform
import java.awt.image.AffineTransformOp
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import javax.swing.*

private var mode = Flip.NONE

fun makeUI(): Component {
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(JLabel("Flip: "))

  val url = Thread.currentThread().contextClassLoader.getResource("example/test.jpg")
  val image = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()

  val p = object : JPanel() {
    override fun paintComponent(g: Graphics) {
      g.color = background
      g.fillRect(0, 0, width, height)
      when (mode) {
        Flip.VERTICAL -> {
          val at = AffineTransform.getScaleInstance(1.0, -1.0)
          at.translate(0.0, -image.getHeight(this).toDouble())
          val g2 = g.create() as? Graphics2D ?: return
          g2.drawImage(image, at, this)
          g2.dispose()
        }
        Flip.HORIZONTAL -> {
          val at = AffineTransform.getScaleInstance(-1.0, 1.0)
          val w = image.getWidth(this)
          val h = image.getHeight(this)
          at.translate(-w.toDouble(), 0.0)
          val atOp = AffineTransformOp(at, null)
          g.drawImage(atOp.filter(image, null), 0, 0, w, h, this)
        }
        Flip.NONE -> {
          val w = image.getWidth(this)
          val h = image.getHeight(this)
          g.drawImage(image, 0, 0, w, h, this)
        }
      }
    }
  }

  val bg = ButtonGroup()
  Flip.values().map { makeRadioButton(it) }
    .forEach {
      box.add(it)
      bg.add(it)
      box.add(Box.createHorizontalStrut(5))
    }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.isOpaque = false
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRadioButton(f: Flip): JRadioButton {
  val rb = JRadioButton(f.toString(), f === Flip.NONE)
  rb.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      mode = f
      rb.rootPane.repaint()
    }
  }
  return rb
}

private fun makeMissingImage(): BufferedImage {
  val missingIcon = MissingIcon()
  val w = missingIcon.iconWidth
  val h = missingIcon.iconHeight
  val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, 0, 0)
  g2.dispose()
  return bi
}

private enum class Flip {
  NONE, VERTICAL, HORIZONTAL
}

private class MissingIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
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
