package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Element
import javax.swing.text.Highlighter.HighlightPainter
import javax.swing.text.IconView
import javax.swing.text.JTextComponent
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.Position.Bias
import javax.swing.text.StyleConstants
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

class MainPanel : JPanel(GridLayout(0, 1)) {
  init {
    val editor0 = makeEditorPane("DefaultHighlightPainter")
    val caret0 = FocusCaret(
      DefaultHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
      DefaultHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true))
    )
    caret0.setBlinkRate(editor0.getCaret().getBlinkRate())
    editor0.setCaret(caret0)

    val editor1 = makeEditorPane("ParagraphMarkHighlightPainter")
    val caret1 = FocusCaret(
      ParagraphMarkHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
      ParagraphMarkHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true))
    )
    caret1.setBlinkRate(editor1.getCaret().getBlinkRate())
    editor1.setCaret(caret1)

    val editor2 = makeEditorPane("WholeLineHighlightPainter")
    val caret2 = FocusCaret(
      WholeLineHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
      WholeLineHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true))
    )
    caret2.setBlinkRate(editor2.getCaret().getBlinkRate())
    editor2.setCaret(caret2)

    add(JScrollPane(editor0))
    add(JScrollPane(editor1))
    add(JScrollPane(editor2))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeEditorPane(txt: String): JEditorPane {
    val editor = JEditorPane()
    editor.setEditorKit(MyEditorKit())
    editor.setText("$txt\n\n123432543543\n")
    editor.setSelectionColor(Color(0xAA_CC_DD_FF.toInt(), true))
    editor.setSelectedTextColor(null)
    return editor
  }
}

class ParagraphMarkHighlightPainter(color: Color) : DefaultHighlightPainter(color) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View
  ): Shape {
    val s = super.paintLayer(g, offs0, offs1, bounds, c, view)
    val r = s.getBounds()
    if (r.width - 1 <= 0) {
      g.fillRect(r.x + r.width, r.y, r.width + r.height / 2, r.height)
    }
    return s
  }
}

internal class WholeLineHighlightPainter(color: Color) : DefaultHighlightPainter(color) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View
  ): Shape {
    val rect = bounds.getBounds().also {
      it.width = c.getSize().width
    }
    return super.paintLayer(g, offs0, offs1, rect, c, view)
  }
}

class FocusCaret(
  private val selectionPainter: HighlightPainter,
  private val nonFocusPainter: HighlightPainter
) : DefaultCaret() {
  override fun focusLost(e: FocusEvent?) {
    super.focusLost(e)
    setSelectionVisible(true)
  }

  override fun focusGained(e: FocusEvent?) {
    super.focusGained(e)
    setSelectionVisible(false) // removeHighlight
    setSelectionVisible(true) // addHighlight
  }

  override fun getSelectionPainter() = if (component.hasFocus()) selectionPainter else nonFocusPainter
}

class MyEditorKit : StyledEditorKit(), ViewFactory {
  override fun getViewFactory() = this

  override fun create(elem: Element) = when (elem.name ?: LabelView(elem)) {
    AbstractDocument.ContentElementName -> LabelView(elem)
    AbstractDocument.ParagraphElementName -> ParagraphWithEndMarkView(elem)
    AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
    StyleConstants.ComponentElementName -> ComponentView(elem)
    StyleConstants.IconElementName -> IconView(elem)
    else -> LabelView(elem)
  }
}

class ParagraphWithEndMarkView(elem: Element) : ParagraphView(elem) {
  override fun paint(g: Graphics, allocation: Shape) {
    super.paint(g, allocation)
    runCatching {
      val para = modelToView(endOffset, allocation, Bias.Backward)
      val r = para?.getBounds() ?: allocation.getBounds()
      paragraphMarkIcon.paintIcon(null, g, r.x, r.y)
    }
  }

  companion object {
    private val paragraphMarkIcon = ParagraphMarkIcon()
  }
}

class ParagraphMarkIcon : Icon {
  private val paragraphMark = Polygon()

  init {
    paragraphMark.addPoint(1, 7)
    paragraphMark.addPoint(3, 7)
    paragraphMark.addPoint(3, 11)
    paragraphMark.addPoint(4, 11)
    paragraphMark.addPoint(1, 14)
  }

  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(MARK_COLOR)
    g2.translate(x, y)
    g2.draw(paragraphMark)
    g2.dispose()
  }

  override fun getIconWidth() = 3

  override fun getIconHeight() = 7

  companion object {
    private val MARK_COLOR = Color(0x78_82_6E)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
