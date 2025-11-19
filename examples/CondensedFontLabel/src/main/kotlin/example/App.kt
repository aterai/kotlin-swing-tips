package example

import java.awt.*
import java.awt.Font
import java.awt.RenderingHints
import java.awt.font.FontRenderContext
import java.awt.font.GlyphMetrics
import java.awt.font.GlyphVector
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.AffineTransform
import java.awt.geom.Point2D
import java.text.AttributedString
import javax.swing.*

private const val TEXT = "The quick brown fox jumps over the lazy dog."

fun makeUI(): Component {
  val textArea = object : JTextArea(TEXT) {
    override fun updateUI() {
      super.updateUI()
      isEditable = false
      lineWrap = true
      wrapStyleWord = true
      isOpaque = false
      background = Color(0x0, true)
    }
  }
  val lbl1 = WrappedLabel(TEXT)
  val lbl2 = WrappingLabel(TEXT)

  val b = BorderFactory.createLineBorder(Color.GRAY, 5)
  val title1 = "JTextArea(condensed: 0.9)"
  textArea.border = BorderFactory.createTitledBorder(b, title1)
  val title2 = "GlyphVector(condensed: 0.9)"
  lbl1.border = BorderFactory.createTitledBorder(b, title2)
  val title3 = "LineBreakMeasurer(condensed: 0.9)"
  lbl2.border = BorderFactory.createTitledBorder(b, title3)

  val p = JPanel(GridLayout(0, 1))
  val at = AffineTransform.getScaleInstance(.9, 1.0)
  val font = Font(Font.MONOSPACED, Font.PLAIN, 18).deriveFont(at)
  listOf<JComponent>(textArea, lbl1, lbl2)
    .forEach {
      it.font = font
      p.add(it)
    }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class WrappingLabel(
  text: String?,
) : JLabel(text) {
  private val rect = Rectangle()

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = foreground
    g2.font = font
    val w = SwingUtilities.calculateInnerArea(this, rect).width
    val x = rect.x.toFloat()
    var y = rect.y.toFloat()
    val attr = AttributedString(text)
    attr.addAttribute(TextAttribute.FONT, font)
    val aci = attr.iterator
    val frc = g2.fontRenderContext
    val lbm = LineBreakMeasurer(aci, frc)
    while (lbm.position < aci.endIndex) {
      val tl = lbm.nextLayout(w.toFloat())
      tl.draw(g2, x, y + tl.ascent)
      y += tl.descent + tl.leading + tl.ascent
    }
    g2.dispose()
  }
}

private class WrappedLabel(
  str: String?,
) : JLabel(str) {
  @Transient private var gvText: GlyphVector? = null
  private var prevWidth = -1
  private val rect = Rectangle()

  override fun doLayout() {
    val w = SwingUtilities.calculateInnerArea(this, rect).width
    if (w != prevWidth) {
      val fm = getFontMetrics(font)
      val frc = fm.fontRenderContext
      gvText = getWrappedGlyphVector(text, w.toDouble(), font, frc)
      prevWidth = w
    }
    super.doLayout()
  }

  override fun paintComponent(g: Graphics) {
    if (gvText != null) {
      val i = insets
      val g2 = g.create() as? Graphics2D ?: return
      g2.drawGlyphVector(gvText, i.left.toFloat(), font.size2D + i.top)
      g2.dispose()
    } else {
      super.paintComponent(g)
    }
  }

  private fun getWrappedGlyphVector(
    str: String?,
    width: Double,
    font: Font,
    frc: FontRenderContext,
  ): GlyphVector {
    val gmPos = Point2D.Float()
    val gv = font.createGlyphVector(frc, str)
    val lineHeight = gv.logicalBounds.height.toFloat()
    var pos = 0f
    var lineCount = 0
    var gm: GlyphMetrics
    for (i in 0..<gv.numGlyphs) {
      gm = gv.getGlyphMetrics(i)
      val advance = gm.advance
      val start = pos + 1
      val end = pos + advance
      if (width in start..end) {
        lineCount++
        pos = 0f
      }
      gmPos.setLocation(pos, lineHeight * lineCount)
      gv.setGlyphPosition(i, gmPos)
      pos += advance
    }
    return gv
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
