package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

fun makeUI() = JPanel(BorderLayout()).also {
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
    val content = getText(0, getLength())
    val startLine: Int = root.getElementIndex(offset)
    val endLine: Int = root.getElementIndex(offset + length)
    for (i in startLine..endLine) {
      applyHighlighting(content, i)
    }
  }

  @Throws(BadLocationException::class)
  private fun applyHighlighting(
    content: String,
    line: Int,
  ) {
    val root = defaultRootElement
    val startOffset = root.getElement(line).startOffset
    var endOffset = root.getElement(line).endOffset - 1
    val lineLength = endOffset - startOffset
    val contentLength = content.length
    endOffset = if (endOffset >= contentLength) contentLength - 1 else endOffset
    setCharacterAttributes(startOffset, lineLength, getStyle(StyleContext.DEFAULT_STYLE), true)
    checkForTokens(content, startOffset, endOffset)
  }

  private fun checkForTokens(
    content: String,
    startOffset: Int,
    endOffset: Int,
  ) {
    var index = startOffset
    while (index <= endOffset) {
      while (isDelimiter(content.substring(index, index + 1))) {
        if (index < endOffset) {
          index++
        } else {
          return
        }
      }
      index = getOtherToken(content, index, endOffset)
    }
  }

  private fun getOtherToken(
    content: String,
    startOffset: Int,
    endOffset: Int,
  ): Int {
    var endOfToken = startOffset + 1
    while (endOfToken <= endOffset) {
      if (isDelimiter(content.substring(endOfToken, endOfToken + 1))) {
        break
      }
      endOfToken++
    }
    val token = content.substring(startOffset, endOfToken)
    getStyle(token)?.also {
      setCharacterAttributes(startOffset, endOfToken - startOffset, it, false)
    }
    return endOfToken + 1
  }

  private fun isDelimiter(character: String) =
    Character.isWhitespace(character[0]) || OPERANDS.contains(character)

  companion object {
    private const val OPERANDS = ".,"
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
