package example

import java.awt.*
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
import javax.swing.*
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot

fun makeUI() = JPanel(BorderLayout()).also {
  it.add(ColorWheel())
  it.preferredSize = Dimension(320, 240)
}

private class ColorWheel : JPanel() {
  private val image = makeColorWheelImage()

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val s = SIZE
    val g2 = g.create() as? Graphics2D ?: return

    // Soft Clipping
    val gc = g2.deviceConfiguration
    val buf = gc.createCompatibleImage(s, s, Transparency.TRANSLUCENT)
    val g2d = buf.createGraphics()

    g2d.composite = AlphaComposite.Src
    g2d.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2d.fill(Ellipse2D.Float(0f, 0f, s.toFloat(), s.toFloat()))

    g2d.composite = AlphaComposite.SrcAtop
    g2d.drawImage(image, 0, 0, null)
    g2d.dispose()

    g2.drawImage(buf, null, (width - s) / 2, (height - s) / 2)
    g2.dispose()
  }

  // Colors: a Color Dialog | Java Graphics
  // https://javagraphics.blogspot.com/2007/04/jcolorchooser-making-alternative.html
  //   https://javagraphics.java.net/
  //   http://www.javased.com/index.php?source_dir=SPREAD/src/colorpicker/swing/ColorPickerPanel.java
  private fun makeColorWheelImage(): BufferedImage {
    val image = BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB)
    val row = IntArray(SIZE)
    val size = SIZE.toFloat()
    val radius = size / 2f

    for (yi in 0..<SIZE) {
      val y = yi - size / 2.0
      for (xi in 0..<SIZE) {
        val x = xi - size / 2.0
        var theta = atan2(y, x) - 3.0 * PI / 2.0
        if (theta < 0) {
          theta += 2.0 * PI
        }
        val r = hypot(x, y) // Math.sqrt(x * x + y * y)
        val hue = (theta / (2.0 * PI)).toFloat()
        val sat = minOf((r / radius).toFloat(), 1f)
        val bri = 1f
        row[xi] = Color.HSBtoRGB(hue, sat, bri)
      }
      image.raster.setDataElements(0, yi, SIZE, 1, row)
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
