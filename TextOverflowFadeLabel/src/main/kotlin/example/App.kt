package example

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.image.BufferedImage
import javax.swing.*

class MainPanel : JPanel(BorderLayout()) {
  init {
    val text = "012345678901234567890123456789012345678901234567890123456789"
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    box.add(makeTitledPanel("defalut JLabel ellipsis", JLabel(text)))
    box.add(Box.createVerticalStrut(15))
    box.add(makeTitledPanel("html JLabel fade out", FadeOutLabel("<html>$text")))
    box.add(Box.createVerticalStrut(15))
    box.add(makeTitledPanel("JTextField fade out", FadingOutLabel(text)))
    box.add(Box.createVerticalGlue())

    add(box, BorderLayout.NORTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val box = Box.createVerticalBox()
    box.setBorder(BorderFactory.createTitledBorder(title))
    box.add(Box.createVerticalStrut(2))
    box.add(c)
    return box
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
    for (x in w - LENGTH until w) {
      rect.x = x
      alpha = Math.max(0f, alpha - DIFF)
      g2.setComposite(AlphaComposite.SrcOver.derive(alpha))
      g2.setClip(rect)
      super.paintComponent(g2)
    }
    g2.dispose()
  }

  companion object {
    private val LENGTH = 20
    private val DIFF = .05f
  }
}

internal class FadingOutLabel(text: String) : JTextField(text) {
  private val dim = Dimension()
  @Transient
  private var buffer: Image? = null

  override fun updateUI() {
    super.updateUI()
    setOpaque(false)
    setEditable(false)
    setFocusable(false)
    setEnabled(false)
    setBorder(BorderFactory.createEmptyBorder())
  }

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
    val baseline = getBaseline(d.width, d.height)
    tl.draw(g2, getInsets().left.toFloat(), baseline.toFloat())
    g2.dispose()

    val spx = Math.max(0, d.width - LENGTH)
    for (x in 0 until LENGTH) {
      val factor = 1.0 - x / LENGTH.toDouble()
      for (y in 0 until d.height) {
        val argb = img.getRGB(spx + x, y)
        val rgb = argb and 0x00FFFFFF
        val a = argb shr 24 and 0xFF
        img.setRGB(spx + x, y, (a * factor).toInt() shl 24 or rgb)
      }
    }
    return img
  }

  companion object {
    private val LENGTH = 20
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
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
  })
}
