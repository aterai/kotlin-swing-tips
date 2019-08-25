package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    add(ColorWheel())
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ColorWheel : JPanel() {
  @Transient private val image: BufferedImage

  init {
    image = updateImage()
    setPreferredSize(Dimension(320, 240))
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)

    // SIZE = 32 * 6; // Drawing breaks on Corretto 1.8.0_212
    val s = SIZE
    val g2 = g.create() as? Graphics2D ?: return

    // Soft Clipping
    val gc = g2.getDeviceConfiguration()
    val buf = gc.createCompatibleImage(s, s, Transparency.TRANSLUCENT)
    val g2d = buf.createGraphics()

    g2d.setComposite(AlphaComposite.Src)
    g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2d.fill(Ellipse2D.Float(0f, 0f, s.toFloat(), s.toFloat()))

    g2d.setComposite(AlphaComposite.SrcAtop)
    g2d.drawImage(image, 0, 0, null)
    g2d.dispose()

    g2.drawImage(buf, null, (getWidth() - s) / 2, (getHeight() - s) / 2)
    g2.dispose()
  }

  // Colors: a Color Dialog | Java Graphics
  // https://javagraphics.blogspot.com/2007/04/jcolorchooser-making-alternative.html
  //   https://javagraphics.java.net/
  //   http://www.javased.com/index.php?source_dir=SPREAD/src/colorpicker/swing/ColorPickerPanel.java
  private fun updateImage(): BufferedImage {
    val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
    val row = IntArray(SIZE)
    val size = SIZE.toFloat()
    val radius = size / 2f

    for (yidx in 0 until SIZE) {
      val y = yidx - size / 2.0
      for (xidx in 0 until SIZE) {
        val x = xidx - size / 2.0
        var theta = Math.atan2(y, x) - 3.0 * Math.PI / 2.0
        if (theta < 0) {
          theta += 2.0 * Math.PI
        }
        val r = Math.sqrt(x * x + y * y)
        val hue = (theta / (2.0 * Math.PI)).toFloat()
        val sat = minOf((r / radius).toFloat(), 1f)
        val bri = 1f
        row[xidx] = Color.HSBtoRGB(hue, sat, bri)
      }
      image.getRaster().setDataElements(0, yidx, SIZE, 1, row)
    }
    return image
  }

  companion object {
    private const val SIZE = 180
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
