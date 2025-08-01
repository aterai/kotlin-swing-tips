package example

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.Line2D
import javax.swing.*

fun makeUI() = JPanel(GridLayout(2, 1)).also {
  it.add(TextLayoutPanel())
  it.add(GlyphVectorPanel())
  it.preferredSize = Dimension(320, 240)
}

private class TextLayoutPanel : JComponent() {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = width.toFloat()
    val baseline = height / 2f
    g2.paint = Color.RED
    g2.draw(Line2D.Float(0f, baseline, w, baseline))
    g2.paint = Color.GREEN
    val ascent = baseline - TEXT_LAYOUT.ascent
    g2.draw(Line2D.Float(0f, ascent, w, ascent))
    g2.paint = Color.BLUE
    val descent = baseline + TEXT_LAYOUT.descent
    g2.draw(Line2D.Float(0f, descent, w, descent))
    g2.paint = Color.ORANGE
    val leading =
      baseline + TEXT_LAYOUT.descent + TEXT_LAYOUT.leading
    g2.draw(Line2D.Float(0f, leading, w, leading))
    g2.paint = Color.CYAN
    val bounds = TEXT_LAYOUT.getBlackBoxBounds(23, 24).bounds
    val xh = baseline - bounds.height.toFloat()
    g2.draw(Line2D.Float(0f, xh, w, xh))
    g2.paint = Color.BLACK
    TEXT_LAYOUT.draw(g2, 0f, baseline)
    g2.dispose()
  }

  companion object {
    private const val TEXT = "abcdefghijklmnopqrstuvwxyz"
    private val FONT = Font(Font.SERIF, Font.ITALIC, 64)
    private val FRC = FontRenderContext(null, true, true)
    private val TEXT_LAYOUT = TextLayout(TEXT, FONT, FRC)
  }
}

private class GlyphVectorPanel : JComponent() {
  private val gv = FONT.createGlyphVector(FRC, TEXT)
  private val lm = FONT.getLineMetrics(TEXT, FRC)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    val w = width.toFloat()
    val baseline = height / 2f
    g2.paint = Color.RED
    g2.draw(Line2D.Float(0f, baseline, w, baseline))
    g2.paint = Color.GREEN
    val ascent = baseline - lm.ascent
    g2.draw(Line2D.Float(0f, ascent, w, ascent))
    g2.paint = Color.BLUE
    val descent = baseline + lm.descent
    g2.draw(Line2D.Float(0f, descent, w, descent))
    g2.paint = Color.ORANGE
    val leading = baseline + lm.descent + lm.leading
    g2.draw(Line2D.Float(0f, leading, w, leading))
    g2.paint = Color.CYAN
    val ht = gv.getGlyphMetrics(23).bounds2D.height
    val xh = baseline - ht.toFloat()
    g2.draw(Line2D.Float(0f, xh, w, xh))
    g2.paint = Color.BLACK
    g2.drawGlyphVector(gv, 0f, baseline)
    g2.dispose()
  }

  companion object {
    private const val TEXT = "abcdefghijklmnopqrstuvwxyz"
    private val FRC = FontRenderContext(null, true, true)
    private val FONT = Font(Font.SERIF, Font.ITALIC, 64)
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
