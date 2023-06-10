package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.Document
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.Position
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyledDocument
import javax.swing.text.StyledEditorKit
import javax.swing.text.TabSet
import javax.swing.text.TabStop
import javax.swing.text.View
import javax.swing.text.ViewFactory

private val TAB_TXT = """
  1	aaa
  12	aaa
  123	aaa
  1234	aaa						
""".trimIndent()
private const val IDEOGRAPHIC_SPACE = """
123456789012
bbb2　　1 3 ccc3


00000　12345　
　日本語　
"""

fun makeUI(): Component {
  val editor = JTextPane()
  editor.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  editor.editorKit = CustomEditorKit()
  editor.text = IDEOGRAPHIC_SPACE + TAB_TXT
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(editor))
    it.preferredSize = Dimension(320, 240)
  }
}

private class CustomEditorKit : StyledEditorKit() {
  override fun install(c: JEditorPane) {
    val fm = c.getFontMetrics(c.font)
    val tabLength = fm.charWidth('m') * 4
    val tabs = arrayOfNulls<TabStop>(100)
    for (j in tabs.indices) {
      tabs[j] = TabStop((j + 1f) * tabLength)
    }
    val tabSet = TabSet(tabs)
    StyleConstants.setTabSet(ATTRS, tabSet)
    super.install(c)
  }

  override fun getViewFactory() = CustomViewFactory()

  override fun createDefaultDocument(): Document {
    val d = super.createDefaultDocument()
    if (d is StyledDocument) {
      d.setParagraphAttributes(0, d.getLength(), ATTRS, false)
    }
    return d
  }

  companion object {
    private val ATTRS = SimpleAttributeSet()
  }
}

private class CustomViewFactory : ViewFactory {
  override fun create(elem: Element) = when (elem.name) {
    AbstractDocument.ParagraphElementName -> ParagraphWithEopmView(elem)
    AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
    StyleConstants.ComponentElementName -> ComponentView(elem)
    StyleConstants.IconElementName -> IconView(elem)
    else -> WhitespaceLabelView(elem)
  }
}

private class ParagraphWithEopmView(elem: Element) : ParagraphView(elem) {
  override fun paint(g: Graphics, allocation: Shape) {
    super.paint(g, allocation)
    paintCustomParagraph(g, allocation)
  }

  private fun paintCustomParagraph(g: Graphics, a: Shape) {
    runCatching {
      val paragraph = modelToView(endOffset, a, Position.Bias.Backward)
      val r = paragraph?.bounds ?: a.bounds
      val x = r.x
      val y = r.y
      val h = r.height
      val old = g.color
      g.color = MARK_COLOR
      g.drawLine(x + 1, y + h / 2, x + 1, y + h - 4)
      g.drawLine(x + 2, y + h / 2, x + 2, y + h - 5)
      g.drawLine(x + 3, y + h - 6, x + 3, y + h - 6)
      g.color = old
    }
  }

  companion object {
    private val MARK_COLOR = Color(0x78_82_6E)
  }
}

private class WhitespaceLabelView(elem: Element) : LabelView(elem) {
  override fun paint(g: Graphics, a: Shape) {
    super.paint(g, a)
    val g2 = g.create() as? Graphics2D ?: return
    val alloc = if (a is Rectangle) a else a.bounds
    val fontMetrics = g.fontMetrics
    val spaceWidth = fontMetrics.stringWidth(IDEOGRAPHIC_SPACE)
    var sumOfTabs = 0
    val text = getText(startOffset, endOffset).toString()
    for (i in text.indices) {
      val s = text.substring(i, i + 1)
      val prevStrWidth = fontMetrics.stringWidth(text.substring(0, i)) + sumOfTabs
      val sx = alloc.x + prevStrWidth
      val sy = alloc.y + alloc.height - fontMetrics.descent
      if (IDEOGRAPHIC_SPACE == s) {
        g2.stroke = DASHED
        g2.paint = MARK_COLOR
        g2.drawLine(sx + 1, sy - 1, sx + spaceWidth - 2, sy - 1)
        g2.drawLine(sx + 2, sy, sx + spaceWidth - 2, sy)
      } else if ("\t" == s) {
        val tabWidth = tabExpander.nextTabStop(sx.toFloat(), i).toInt() - sx
        g2.paint = MARK_COLOR
        g2.drawLine(sx + 2, sy, sx + 2 + 2, sy)
        g2.drawLine(sx + 2, sy - 1, sx + 2 + 1, sy - 1)
        g2.drawLine(sx + 2, sy - 2, sx + 2, sy - 2)
        g2.stroke = DASHED
        g2.drawLine(sx + 2, sy, sx + tabWidth - 2, sy)
        sumOfTabs += tabWidth
      }
    }
    g2.dispose()
  }

  companion object {
    private const val IDEOGRAPHIC_SPACE = "　" // \u3000
    private val MARK_COLOR = Color(0x78_82_6E)
    private val DASHED = BasicStroke(
      1f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,
      10f,
      floatArrayOf(1f),
      0f
    )
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
