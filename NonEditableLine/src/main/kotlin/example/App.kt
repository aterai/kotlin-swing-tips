package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.DocumentFilter

fun makeUI(): Component {
  val maskRange = 2
  val highlightPainter = DefaultHighlightPainter(Color.GRAY)
  val textArea = JTextArea()
  textArea.text = """
    1234567890987654321
    aaa bbb ccc ddd eee
    1234567890
    1234567890987654321
  """.trimIndent()
  val doc = textArea.document
  (doc as? AbstractDocument)?.documentFilter = NonEditableLineDocumentFilter(maskRange)
  runCatching {
    val highlighter = textArea.highlighter
    val root = textArea.document.defaultRootElement
    for (i in 0 until maskRange) {
      val elem = root.getElement(i)
      highlighter.addHighlight(elem.startOffset, elem.endOffset - 1, highlightPainter)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class NonEditableLineDocumentFilter(private val maskRange: Int) : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(fb: FilterBypass, offset: Int, text: String?, attr: AttributeSet?) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(fb: FilterBypass, offset: Int, length: Int) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String,
    attrs: AttributeSet?,
  ) {
    if (fb.document.defaultRootElement.getElementIndex(offset) >= maskRange) {
      fb.replace(offset, length, text, attrs)
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
