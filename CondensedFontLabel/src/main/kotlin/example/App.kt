package example

import java.awt.* // ktlint-disable no-wildcard-imports
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
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(GridLayout(0, 1)) {
  init {
    val textArea = object : JTextArea(TEXT) {
      override fun updateUI() {
        super.updateUI()
        setEditable(false)
        setLineWrap(true)
        setWrapStyleWord(true)
        setOpaque(false)
        setBackground(Color(0x0, true))
      }
    }
    val lbl1 = WrappedLabel(TEXT)
    val lbl2 = WrappingLabel(TEXT)

    val b = BorderFactory.createLineBorder(Color.GRAY, 5)
    textArea.setBorder(BorderFactory.createTitledBorder(b, "JTextArea(condensed: 0.9)"))
    lbl1.setBorder(BorderFactory.createTitledBorder(b, "GlyphVector(condensed: 0.9)"))
    lbl2.setBorder(BorderFactory.createTitledBorder(b, "LineBreakMeasurer(condensed: 0.9)"))

    val font = Font(Font.MONOSPACED, Font.PLAIN, 18).deriveFont(AffineTransform.getScaleInstance(.9, 1.0))
    listOf<JComponent>(textArea, lbl1, lbl2)
      .forEach {
        it.setFont(font)
        add(it)
      }
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val TEXT = "1234567890 ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  }
}

class WrappingLabel(text: String?) : JLabel(text) {
  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(getForeground())
    g2.setFont(getFont())
    val i = getInsets()
    val x = i.left.toFloat()
    var y = i.top.toFloat()
    val w = getWidth() - i.left - i.right
    val attr = AttributedString(text)
    attr.addAttribute(TextAttribute.FONT, getFont())
    val aci = attr.getIterator()
    val frc = g2.getFontRenderContext()
    val lbm = LineBreakMeasurer(aci, frc)
    while (lbm.getPosition() < aci.getEndIndex()) {
      val tl = lbm.nextLayout(w.toFloat())
      tl.draw(g2, x, y + tl.getAscent())
      y += tl.getDescent() + tl.getLeading() + tl.getAscent()
    }
    g2.dispose()
  }
}

class WrappedLabel(str: String?) : JLabel(str) {
  @Transient
  private var gvText: GlyphVector? = null
  private var prevWidth = -1

  override fun doLayout() {
    val i = getInsets()
    val w = getWidth() - i.left - i.right
    if (w != prevWidth) {
      val fm = getFontMetrics(getFont())
      val frc = fm.getFontRenderContext()
      gvText = getWrappedGlyphVector(text, w.toDouble(), getFont(), frc)
      prevWidth = w
    }
    super.doLayout()
  }

  override fun paintComponent(g: Graphics) {
    if (gvText != null) {
      val i = getInsets()
      val g2 = g.create() as? Graphics2D ?: return
      g2.drawGlyphVector(gvText, i.left.toFloat(), font.getSize2D() + i.top)
      g2.dispose()
    } else {
      super.paintComponent(g)
    }
  }

  private fun getWrappedGlyphVector(
    str: String?,
    width: Double,
    font: Font,
    frc: FontRenderContext
  ): GlyphVector {
    val gmPos = Point2D.Float()
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
