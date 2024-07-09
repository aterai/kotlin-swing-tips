package example

import java.awt.*
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.text.AttributedString
import javax.swing.*

private val text = """
  This lesson provides an introduction to Graphical User Interface (GUI) programming with Swing
   and the NetBeans IDE. As you learned in the "Hello World!" lesson, the NetBeans IDE is a free,
   open-source, cross-platform integrated development environment with built-in support for
   the Java programming language.
""".trimIndent()

fun makeUI(): Component {
  val label = DropcapLabel(text)
  label.font = Font(Font.SERIF, Font.PLAIN, 17)
  label.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(label)
    it.border = BorderFactory.createLineBorder(Color(100, 200, 200, 100), 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DropcapLabel(
  text: String,
) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = background
    g2.fillRect(0, 0, width, height)

    val i = insets
    val x0 = i.left.toFloat()
    val y0 = i.top.toFloat()

    val font = font
    val txt = text

    val frc = g2.fontRenderContext
    val shape = TextLayout(txt.substring(0, 1), font, frc).getOutline(null)

    val at1 = AffineTransform.getScaleInstance(5.0, 5.0)
    val s1 = at1.createTransformedShape(shape)
    val r = s1.bounds
    r.grow(6, 2)
    val rw = r.width
    val rh = r.height

    val at2 = AffineTransform.getTranslateInstance(x0.toDouble(), (y0 + rh).toDouble())
    val s2 = at2.createTransformedShape(s1)
    g2.paint = foreground
    g2.fill(s2)

    var x = x0 + rw
    var y = y0
    val w0 = width - i.left - i.right
    var w = w0 - rw

    val attr = AttributedString(txt.substring(1))
    attr.addAttribute(TextAttribute.FONT, font)
    val aci = attr.iterator
    val lbm = LineBreakMeasurer(aci, frc)
    while (lbm.position < aci.endIndex) {
      val tl = lbm.nextLayout(w.toFloat())
      tl.draw(g2, x, y + tl.ascent)
      y += tl.descent + tl.leading + tl.ascent
      if (y0 + rh < y) {
        x = x0
        w = w0
      }
    }
    g2.dispose()
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
