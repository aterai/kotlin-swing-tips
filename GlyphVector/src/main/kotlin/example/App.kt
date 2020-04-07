package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.FontRenderContext
import java.awt.font.GlyphMetrics
import java.awt.font.GlyphVector
import java.awt.font.LineBreakMeasurer
import java.awt.font.TextAttribute
import java.awt.geom.Point2D
import java.text.AttributedString
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val TEXT = "あいうえお かきくけこ さしすせそ たちつてと なにぬねの はひふへほ まみむめも"

fun makeUI(): Component {
  val lbl1 = JLabel(TEXT)
  lbl1.border = makeTitledColorBorder("JLabel", Color.YELLOW)
  val lbl2: JLabel = WrappedLabel(TEXT)
  lbl2.border = makeTitledColorBorder("GlyphVector", Color.GREEN)
  val lbl3: JLabel = WrappingLabel(TEXT)
  lbl3.border = makeTitledColorBorder("LineBreakMeasurer", Color.CYAN)
  val lbl4 = JTextArea(TEXT)
  lbl4.border = makeTitledColorBorder("JTextArea", Color.ORANGE)

  lbl4.font = lbl1.font
  lbl4.isEditable = false
  lbl4.lineWrap = true
  lbl4.background = lbl1.background
  return JPanel(GridLayout(4, 1, 0, 0)).also {
    it.add(lbl1)
    it.add(lbl2)
    it.add(lbl3)
    it.add(lbl4)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledColorBorder(title: String, color: Color) =
  BorderFactory.createTitledBorder(BorderFactory.createLineBorder(color, 5), title)

private class WrappingLabel(text: String?) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = foreground
    val i = insets
    val x = i.left.toFloat()
    var y = i.top.toFloat()
    val w = width - i.left - i.right
    val attrs = AttributedString(text)
    attrs.addAttribute(TextAttribute.FONT, font)
    val aci = attrs.iterator
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

private class WrappedLabel(str: String?) : JLabel(str) {
  @Transient
  private var gvText: GlyphVector? = null
  private var prevWidth = -1
  override fun doLayout() {
    val i = insets
    val w = width - i.left - i.right
    if (w != prevWidth) {
      val font = font
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
      g2.paint = Color.RED
      g2.drawGlyphVector(gvText, i.left.toFloat(), font.size2D + i.top)
      g2.dispose()
    } else {
      super.paintComponent(g)
    }
  }

  companion object {
    private fun getWrappedGlyphVector(
      str: String,
      width: Double,
      font: Font,
      frc: FontRenderContext
    ): GlyphVector {
      val gmPos: Point2D = Point2D.Float()
      val gv = font.createGlyphVector(frc, str)
      val lineHeight = gv.logicalBounds.height.toFloat()
      var pos = 0f
      var lineCount = 0
      var gm: GlyphMetrics
      for (i in 0 until gv.numGlyphs) {
        gm = gv.getGlyphMetrics(i)
        val advance = gm.advance
        if (pos < width && width <= pos + advance) {
          lineCount++
          pos = 0f
        }
        gmPos.setLocation(pos.toDouble(), lineHeight * lineCount.toDouble())
        gv.setGlyphPosition(i, gmPos)
        pos += advance
      }
      return gv
    }
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
