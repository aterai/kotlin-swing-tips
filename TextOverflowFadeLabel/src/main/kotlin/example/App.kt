package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val text = "012345678901234567890123456789012345678901234567890123456789"
    val box = Box.createVerticalBox().also {
      it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
      it.add(makeTitledPanel("defalut JLabel ellipsis", JLabel(text)))
      it.add(Box.createVerticalStrut(5))
      it.add(makeTitledPanel("html JLabel fade out", FadeOutLabel("<html>$text")))
      it.add(Box.createVerticalStrut(5))
      it.add(makeTitledPanel("JLabel TextLayout fade out", TextOverflowFadeLabel(text)))
      it.add(Box.createVerticalStrut(5))
      it.add(makeTitledPanel("JLabel BufferedImage fade out", FadingOutLabel(text)))
      it.add(Box.createVerticalGlue())
    }

    add(box, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component) = Box.createVerticalBox().also {
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(Box.createVerticalStrut(2))
    it.add(c)
  }
}

internal class FadeOutLabel(text: String) : JLabel(text) {

  override fun paintComponent(g: Graphics) {
    val i = getInsets()
    val w = getWidth() - i.left - i.right
    val h = getHeight() - i.top - i.bottom

    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as Graphics2D
    g2.setFont(g.getFont())
    g2.setClip(rect)
    g2.setComposite(AlphaComposite.SrcOver.derive(.99999f))
    super.paintComponent(g2)

    rect.width = 1
    var alpha = 1f
    (w - LENGTH until w).forEach {
      rect.x = it
      alpha = Math.max(0f, alpha - DIFF)
      g2.setComposite(AlphaComposite.SrcOver.derive(alpha))
      g2.setClip(rect)
      super.paintComponent(g2)
    }
    g2.dispose()
  }

  companion object {
    private const val LENGTH = 20
    private const val DIFF = .05f
  }
}

internal class TextOverflowFadeLabel(text: String) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val i = getInsets()
    val w = getWidth() - i.left - i.right
    val h = getHeight() - i.top - i.bottom
    val rect = Rectangle(i.left, i.top, w - LENGTH, h)

    val g2 = g.create() as Graphics2D
    g2.setFont(g.getFont())
    g2.setPaint(getForeground())

    val frc = g2.getFontRenderContext()
    val tl = TextLayout(getText(), getFont(), frc)
    val baseline = getBaseline(w, h).toFloat()
    val fx = i.left.toFloat()

    g2.setClip(rect)
    tl.draw(g2, fx, baseline)

    rect.width = 1
    var alpha = 1f
    (w - LENGTH until w).forEach { x ->
      rect.x = x
      alpha = Math.max(0f, alpha - DIFF)
      g2.setComposite(AlphaComposite.SrcOver.derive(alpha))
      g2.setClip(rect)
      tl.draw(g2, fx, baseline)
    }
    g2.dispose()
  }

  companion object {
    private const val LENGTH = 20
    private const val DIFF = .05f
  }
}

internal class FadingOutLabel(text: String) : JLabel(text) {
  private val dim = Dimension()
  @Transient
  private var buffer: Image? = null

  override fun paintComponent(g: Graphics) {
    // super.paintComponent(g);
    val w = getWidth()
    val h = getHeight()
    if (buffer == null || dim.width != w || dim.height != h) {
      dim.setSize(w, h)
      buffer = updateImage(dim)
    }
    g.drawImage(buffer, 0, 0, this)
  }

  private fun updateImage(d: Dimension): BufferedImage {
    val img = BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
    val g2 = img.createGraphics()
    g2.setFont(getFont())
    g2.setPaint(getForeground())
    val frc = g2.getFontRenderContext()
    val tl = TextLayout(getText(), getFont(), frc)
    val baseline = getBaseline(d.width, d.height).toFloat()
    tl.draw(g2, getInsets().left.toFloat(), baseline)
    g2.dispose()

    val spx = Math.max(0, d.width - LENGTH)
    (0 until LENGTH).forEach { x ->
      val factor = 1.0 - x / LENGTH.toDouble()
      (0 until d.height).forEach { y ->
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
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
