package example

import java.awt.*
import java.awt.event.FocusEvent
import javax.swing.*
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

fun makeUI(): Component {
  val editor0 = makeEditorPane("DefaultHighlightPainter")
  val caret0 = FocusCaret(
    DefaultHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
    DefaultHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true)),
  )
  caret0.blinkRate = editor0.caret.blinkRate
  editor0.caret = caret0

  val editor1 = makeEditorPane("ParagraphMarkHighlightPainter")
  val caret1 = FocusCaret(
    ParagraphMarkHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
    ParagraphMarkHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true)),
  )
  caret1.blinkRate = editor1.caret.blinkRate
  editor1.caret = caret1

  val editor2 = makeEditorPane("WholeLineHighlightPainter")
  val caret2 = FocusCaret(
    WholeLineHighlightPainter(Color(0xAA_CC_DD_FF.toInt(), true)),
    WholeLineHighlightPainter(Color(0xEE_EE_EE_EE.toInt(), true)),
  )
  caret2.blinkRate = editor2.caret.blinkRate
  editor2.caret = caret2

  return JPanel(GridLayout(0, 1)).also {
    it.add(JScrollPane(editor0))
    it.add(JScrollPane(editor1))
    it.add(JScrollPane(editor2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeEditorPane(txt: String): JEditorPane {
  val editor = JEditorPane()
  editor.editorKit = MyEditorKit()
  editor.text = """
    $txt

    123432543543
  """.trimIndent()
  editor.selectionColor = Color(0xAA_CC_DD_FF.toInt(), true)
  editor.selectedTextColor = null
  return editor
}

private class ParagraphMarkHighlightPainter(
  color: Color,
) : DefaultHighlightPainter(color) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View,
  ): Shape {
    val s = super.paintLayer(g, offs0, offs1, bounds, c, view)
    val r = s.bounds
    if (r.width - 1 <= 0) {
      g.fillRect(r.x + r.width, r.y, r.width + r.height / 2, r.height)
    }
    return s
  }
}

private class WholeLineHighlightPainter(
  color: Color,
) : DefaultHighlightPainter(color) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View,
  ): Shape {
    val rect = bounds.bounds.also {
      it.width = c.size.width
    }
    return super.paintLayer(g, offs0, offs1, rect, c, view)
  }
}

private class FocusCaret(
  private val selectionPainter: HighlightPainter,
  private val nonFocusPainter: HighlightPainter,
) : DefaultCaret() {
  override fun focusLost(e: FocusEvent?) {
    super.focusLost(e)
    isSelectionVisible = true
  }

  override fun focusGained(e: FocusEvent?) {
    super.focusGained(e)
    isSelectionVisible = false // removeHighlight
    isSelectionVisible = true // addHighlight
  }

  override fun getSelectionPainter() =
    if (component.hasFocus()) {
      selectionPainter
    } else {
      nonFocusPainter
    }

  override fun equals(other: Any?) =
    if (this === other) {
      true
    } else {
      super.equals(other) &&
        other is FocusCaret &&
        nonFocusPainter == other.nonFocusPainter &&
        getSelectionPainter() == other.getSelectionPainter()
    }

  override fun hashCode() =
    Objects.hash(super.hashCode(), nonFocusPainter, getSelectionPainter())

  override fun toString() = "FocusCaret{nonFocusPainter=%s, selectionPainter=%s}"
    .format(nonFocusPainter, selectionPainter)
}

private class MyEditorKit :
  StyledEditorKit(),
  ViewFactory {
  override fun getViewFactory() = this

  override fun create(elem: Element) =
    when (elem.name ?: LabelView(elem)) {
      AbstractDocument.ContentElementName -> LabelView(elem)
      AbstractDocument.ParagraphElementName -> ParagraphWithEndMarkView(elem)
      AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
      StyleConstants.ComponentElementName -> ComponentView(elem)
      StyleConstants.IconElementName -> IconView(elem)
      else -> LabelView(elem)
    }
}

private class ParagraphWithEndMarkView(
  elem: Element,
) : ParagraphView(elem) {
  private val paragraphMarkIcon = ParagraphMarkIcon()

  override fun paint(
    g: Graphics,
    allocation: Shape,
  ) {
    super.paint(g, allocation)
    runCatching {
      val para = modelToView(endOffset, allocation, Bias.Backward)
      val r = para?.bounds ?: allocation.bounds
      paragraphMarkIcon.paintIcon(null, g, r.x, r.y)
    }
  }
}

private class ParagraphMarkIcon : Icon {
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
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = MARK_COLOR
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
