package example

import java.awt.*
import java.awt.font.FontRenderContext
import java.awt.font.GlyphVector
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.Point2D
import java.text.AttributedString
import javax.swing.*

private const val TEXT = "あいうえお かきくけこ さしすせそ たちつてと なにぬねの はひふへほ まみむめも"

fun createUI(): Component {
  val label1 = JLabel(TEXT)
  label1.border = makeTitledColorBorder("JLabel", Color.YELLOW)
  val label2 = GlyphVectorWrapLabel(TEXT)
  label2.border = makeTitledColorBorder("GlyphVector", Color.GREEN)
  val label3 = LineBreakMeasurerWrapLabel(TEXT)
  label3.border = makeTitledColorBorder("LineBreakMeasurer", Color.CYAN)
  val textArea = JTextArea(TEXT)
  textArea.border = makeTitledColorBorder("JTextArea", Color.ORANGE)
  textArea.font = label1.font
  textArea.isEditable = false
  textArea.lineWrap = true
  textArea.background = label1.background
  return JPanel(GridLayout(4, 1, 0, 0)).also {
    it.add(label1)
    it.add(label2)
    it.add(label3)
    it.add(textArea)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledColorBorder(
  title: String,
  color: Color,
) = BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, 5), title)

private class LineBreakMeasurerWrapLabel(
  text: String?,
) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val text = text
    if (text.isNullOrEmpty()) {
      // AttributedString#addAttribute(...) throws for 0-length text
      super.paintComponent(g)
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = foreground
    val attrs = AttributedString(text)
    attrs.addAttribute(TextAttribute.FONT, font)
    val aci = attrs.iterator
    val frc = g2.fontRenderContext
    val lbm = LineBreakMeasurer(aci, frc)
    val r = SwingUtilities.calculateInnerArea(this, null)
    val x = r.x.toFloat()
    var y = r.y.toFloat()
    val bottom = (r.y + r.height).toFloat()
    // Stop once the lines go below the inner area since they are invisible
    while (lbm.position < aci.endIndex && y < bottom) {
      val tl = lbm.nextLayout(r.width.toFloat())
      tl.draw(g2, x, y + tl.ascent)
      y += tl.ascent + tl.descent + tl.leading
    }
    g2.dispose()
  }
}

private class GlyphVectorWrapLabel(
  text: String?,
) : JLabel(text) {
  private var wrappedGlyphs: GlyphVector? = null
  private var cachedWidth = -1
  private var cachedText: String? = null
  private var cachedFont: Font? = null

  override fun paintComponent(g: Graphics) {
    val text = text
    if (text.isNullOrEmpty()) {
      super.paintComponent(g)
      return
    }
    val r = SwingUtilities.calculateInnerArea(this, null)
    val font = font
    // Rebuild the glyphs only if the width, text, or font has changed
    val glyphs = wrappedGlyphs
      ?.takeIf { r.width == cachedWidth && text == cachedText && font == cachedFont }
      ?: createWrappedGlyphVector(
        text,
        font,
        getFontMetrics(font).fontRenderContext,
        r.width.toFloat(),
      ).also {
        wrappedGlyphs = it
        cachedWidth = r.width
        cachedText = text
        cachedFont = font
      }
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = Color.RED
    g2.drawGlyphVector(glyphs, r.x.toFloat(), r.y.toFloat())
    g2.dispose()
  }
}

private fun createWrappedGlyphVector(
  text: String,
  font: Font,
  frc: FontRenderContext,
  width: Float,
): GlyphVector {
  // Font#layoutGlyphVector(...) applies kerning and ligatures,
  // unlike Font#createGlyphVector(...) which maps chars one-to-one
  val chars = text.toCharArray()
  val gv = font.layoutGlyphVector(frc, chars, 0, chars.size, Font.LAYOUT_LEFT_TO_RIGHT)
  val lm = font.getLineMetrics(text, frc)
  val ascent = lm.ascent
  val lineHeight = lm.height
  val glyphPos = Point2D.Float()
  var x = 0f
  var lineCount = 0
  for (i in 0..<gv.numGlyphs) {
    val advance = gv.getGlyphMetrics(i).advance
    // Wrap only when the glyph overflows the width, and never before the
    // first glyph of a line to avoid a blank line for a too wide glyph
    if (x > 0f && x + advance > width) {
      lineCount++
      x = 0f
    }
    // Place each glyph on its baseline so that the vector is drawn from the top-left
    glyphPos.setLocation(x, ascent + lineHeight * lineCount)
    gv.setGlyphPosition(i, glyphPos)
    x += advance
  }
  return gv
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
