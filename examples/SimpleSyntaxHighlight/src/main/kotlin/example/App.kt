package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Element
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

fun createUI() = JPanel(BorderLayout()).also {
  val doc = SimpleSyntaxDocument()
  val def = doc.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setForeground(doc.addStyle("red", def), Color.RED)
  StyleConstants.setForeground(doc.addStyle("green", def), Color.GREEN)
  StyleConstants.setForeground(doc.addStyle("blue", def), Color.BLUE)
  val textPane = JTextPane(doc)
  textPane.text = "red green, blue.\n  red-green;blue."
  it.add(JScrollPane(textPane))
  it.preferredSize = Dimension(320, 240)
}

private class SimpleSyntaxDocument : DefaultStyledDocument() {
  @Throws(BadLocationException::class)
  override fun insertString(
    offset: Int,
    text: String,
    a: AttributeSet?,
  ) {
    super.insertString(offset, text, a)
    processChangedLines(offset, text.length)
  }

  @Throws(BadLocationException::class)
  override fun remove(
    offset: Int,
    length: Int,
  ) {
    super.remove(offset, length)
    processChangedLines(offset, 0)
  }

  @Throws(BadLocationException::class)
  private fun processChangedLines(
    offset: Int,
    length: Int,
  ) {
    val root = defaultRootElement
    val startLine = root.getElementIndex(offset)
    val endLine = root.getElementIndex(offset + length)
    for (i in startLine..endLine) {
      applyHighlighting(root.getElement(i))
    }
  }

  @Throws(BadLocationException::class)
  private fun applyHighlighting(line: Element) {
    val startOffset = line.startOffset
    // Exclude the trailing newline; the last line element also ends with
    // an implied newline at getLength() + 1, so this never exceeds the length
    val endOffset = line.endOffset - 1
    // Fetch only the changed line instead of the whole document text
    val text = getText(startOffset, endOffset - startOffset)
    setCharacterAttributes(
      startOffset,
      text.length,
      getStyle(StyleContext.DEFAULT_STYLE),
      true,
    )
    highlightKeywords(text, startOffset)
  }

  private fun highlightKeywords(
    text: String,
    lineOffset: Int,
  ) {
    val length = text.length
    var index = 0
    while (index < length) {
      val tokenStart = index
      while (index < length && !isDelimiter(text[index])) {
        index++
      }
      if (index > tokenStart) {
        getStyle(text.substring(tokenStart, index))?.also {
          setCharacterAttributes(lineOffset + tokenStart, index - tokenStart, it, false)
        }
      }
      index++ // skip the delimiter
    }
  }

  fun isDelimiter(ch: Char) = Character.isWhitespace(ch) || DELIMITERS.indexOf(ch) >= 0

  companion object {
    private const val DELIMITERS = ".,"
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
