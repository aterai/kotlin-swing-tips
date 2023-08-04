package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.swing.*

fun makeUI(): Component {
  val text = "012345678901234567890123456789012345678901234567890123456789"
  val box = Box.createVerticalBox().also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(makeTitledPanel("default JLabel ellipsis", JLabel(text)))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("html JLabel fade out", FadeOutLabel("<html>$text")))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("JLabel TextLayout fade out", TextOverflowFadeLabel(text)))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("JLabel BufferedImage fade out", FadingOutLabel(text)))
    it.add(Box.createVerticalGlue())
  }
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private class FadeOutLabel(text: String) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val i = insets
    val w = width - i.left - i.right
    val h = height - i.top - i.bottom

    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as? Graphics2D ?: return
    g2.font = g.font
    g2.clip = rect
    g2.composite = AlphaComposite.SrcOver.derive(.99999f)
    super.paintComponent(g2)

    rect.width = 1
    var alpha = 1f
    for (x in w - LENGTH until w) {
      rect.x = x
      alpha = maxOf(0f, alpha - DIFF)
      g2.composite = AlphaComposite.SrcOver.derive(alpha)
      g2.clip = rect
      super.paintComponent(g2)
    }
    g2.dispose()
  }

  companion object {
    private const val LENGTH = 20
    private const val DIFF = .05f
  }
}

private class TextOverflowFadeLabel(text: String) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val i = insets
    val w = width - i.left - i.right
    val h = height - i.top - i.bottom
    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as? Graphics2D ?: return
    g2.font = g.font
    g2.paint = foreground

    val tl = TextLayout(text, font, g2.fontRenderContext)
    val baseline = getBaseline(w, h).toFloat()
    val fx = i.left.toFloat()

    g2.clip = rect
    tl.draw(g2, fx, baseline)

    rect.width = 1
    var alpha = 1f
    for (x in w - LENGTH until w) {
      rect.x = x
      alpha = maxOf(0f, alpha - DIFF)
      g2.composite = AlphaComposite.SrcOver.derive(alpha)
      g2.clip = rect
      tl.draw(g2, fx, baseline)
    }
    g2.dispose()
  }

  companion object {
    private const val LENGTH = 20
    private const val DIFF = .05f
  }
}

private class FadingOutLabel(text: String) : JLabel(text) {
  private val dim = Dimension()
  private var buffer: Image? = null

  override fun paintComponent(g: Graphics) {
    // super.paintComponent(g)
    val w = width
    val h = height
    if (buffer == null || dim.width != w || dim.height != h) {
      dim.setSize(w, h)
      buffer = updateImage(dim)
    }
    g.drawImage(buffer, 0, 0, this)
  }

  private fun updateImage(d: Dimension): BufferedImage {
    val img = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.font = font
    g2.paint = foreground
    val tl = TextLayout(text, font, g2.fontRenderContext)
    val baseline = getBaseline(d.width, d.height).toFloat()
    tl.draw(g2, insets.left.toFloat(), baseline)
    g2.dispose()

    val spx = maxOf(0, d.width - LENGTH)
    for (x in 0 until LENGTH) {
      val factor = 1.0 - x / LENGTH.toDouble()
      for (y in 0 until d.height) {
        val argb = img.getRGB(spx + x, y)
        val rgb = argb and 0x00_FF_FF_FF
        val a = argb shr 24 and 0xFF
        img.setRGB(spx + x, y, (a * factor).toInt() shl 24 or rgb)
      }
    }
    return img
  }

  companion object {
    private const val LENGTH = 20
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
