package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Style
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textPane = JTextPane(SimpleSyntaxDocument())
    textPane.setText("red green, blue. red-green;blue.")
    add(JScrollPane(textPane))
    setPreferredSize(Dimension(320, 240))
  }
}

class SimpleSyntaxDocument : DefaultStyledDocument() {
  private val def: Style? = getStyle(StyleContext.DEFAULT_STYLE)

  init {
    StyleConstants.setForeground(addStyle("red", def), Color.RED)
    StyleConstants.setForeground(addStyle("green", def), Color.GREEN)
    StyleConstants.setForeground(addStyle("blue", def), Color.BLUE)
  }

  @Throws(BadLocationException::class)
  override fun insertString(offset: Int, text: String, a: AttributeSet?) {
    var length = 0
    var str = text
    if (str.contains(LB)) {
      val filtered = StringBuilder(str)
      val n = filtered.length
      for (i in 0 until n) {
        if (filtered[i] == LB) {
          filtered.setCharAt(i, ' ')
        }
      }
      str = filtered.toString()
      length = str.length
    }
    super.insertString(offset, str, a)
    processChangedLines(offset, length)
  }

  @Throws(BadLocationException::class)
  override fun remove(offset: Int, length: Int) {
    super.remove(offset, length)
    processChangedLines(offset, 0)
  }

  @Throws(BadLocationException::class)
  private fun processChangedLines(offset: Int, length: Int) {
    val root = getDefaultRootElement()
    val content = getText(0, getLength())
    val startLine = root.getElementIndex(offset)
    val endLine = root.getElementIndex(offset + length)
    for (i in startLine..endLine) {
      applyHighlighting(content, i)
    }
  }

  @Throws(BadLocationException::class)
  private fun applyHighlighting(content: String, line: Int) {
    val root = getDefaultRootElement()
    val startOffset = root.getElement(line).getStartOffset()
    var endOffset = root.getElement(line).getEndOffset() - 1
    val lineLength = endOffset - startOffset
    val contentLength = content.length
    endOffset = if (endOffset >= contentLength) contentLength - 1 else endOffset
    setCharacterAttributes(startOffset, lineLength, def, true)
    checkForTokens(content, startOffset, endOffset)
  }

  private fun checkForTokens(content: String, startOffset: Int, endOffset: Int) {
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

  private fun getOtherToken(content: String, startOffset: Int, endOffset: Int): Int {
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

  private fun isDelimiter(character: String) = Character.isWhitespace(character[0]) || OPERANDS.contains(character)

  companion object {
    private const val LB = '\n'
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
