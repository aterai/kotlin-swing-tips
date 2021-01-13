package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.lang.StringBuilder
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.BoxView
import javax.swing.text.ComponentView
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.Element
import javax.swing.text.IconView
import javax.swing.text.LabelView
import javax.swing.text.ParagraphView
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.StyledEditorKit
import javax.swing.text.View
import javax.swing.text.ViewFactory

fun makeUI(): Component {
  val str = "red green blue 111111111111111111111111111111111"
  val textPane = object : JTextPane() {
    override fun scrollRectToVisible(rect: Rectangle) {
      rect.grow(insets.right, 0)
      super.scrollRectToVisible(rect)
    }
  }

  textPane.editorKit = NoWrapEditorKit()
  val doc = SimpleSyntaxDocument()
  textPane.document = doc
  runCatching {
    doc.insertString(0, str, null)
  }
  val key = "Do-Nothing"
  val im = textPane.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), key)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0), key)
  val action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      // Do nothing
    }
  }
  textPane.actionMap.put(key, action)

  val ftk = KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS
  val forwardKeys = HashSet(textPane.getFocusTraversalKeys(ftk))
  forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0))
  forwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK))
  textPane.setFocusTraversalKeys(ftk, forwardKeys)

  val scrollPane = object : JScrollPane(textPane) {
    override fun updateUI() {
      super.updateUI()
      setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_NEVER)
      setHorizontalScrollBarPolicy(HORIZONTAL_SCROLLBAR_NEVER)
      border = BorderFactory.createLineBorder(Color.GRAY)
      viewportBorder = BorderFactory.createEmptyBorder()
    }

    override fun getMinimumSize() = super.getPreferredSize()
  }

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("JTextField", JTextField(str)))
    it.add(makeTitledPanel("JTextPane+StyledDocument+JScrollPane", scrollPane))
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, cmp: Component) = JPanel(GridBagLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  it.add(cmp, c)
}

private class SimpleSyntaxDocument : DefaultStyledDocument() {
  init {
    val def = getStyle(StyleContext.DEFAULT_STYLE)
    StyleConstants.setForeground(addStyle("red", def), Color.RED)
    StyleConstants.setForeground(addStyle("green", def), Color.GREEN)
    StyleConstants.setForeground(addStyle("blue", def), Color.BLUE)
  }

  @Throws(BadLocationException::class)
  override fun insertString(offset: Int, text: String?, a: AttributeSet?) {
    var length = 0
    var str = text
    if (str != null && str.indexOf(LB) >= 0) {
      val filtered = StringBuilder(str)
      for (i in filtered.indices) {
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
    val root = defaultRootElement
    val content = getText(0, getLength())
    val startLine = root.getElementIndex(offset)
    val endLine = root.getElementIndex(offset + length)
    for (i in startLine..endLine) {
      applyHighlighting(content, i)
    }
  }

  private fun applyHighlighting(content: String, line: Int) {
    val root = defaultRootElement
    val startOffset = root.getElement(line).startOffset
    var endOffset = root.getElement(line).endOffset - 1
    val lineLength = endOffset - startOffset
    val contentLength = content.length
    endOffset = if (endOffset >= contentLength) contentLength - 1 else endOffset
    setCharacterAttributes(startOffset, lineLength, getStyle(StyleContext.DEFAULT_STYLE), true)
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
    val s = getStyle(token)
    if (s != null) {
      setCharacterAttributes(startOffset, endOfToken - startOffset, s, false)
    }
    return endOfToken + 1
  }

  private fun isDelimiter(character: String) = Character.isWhitespace(character[0]) || OPERANDS.contains(character)

  companion object {
    private const val LB = '\n'
    private const val OPERANDS = ".,"
  }
}

private class NoWrapParagraphView(elem: Element?) : ParagraphView(elem) {
  override fun calculateMinorAxisRequirements(axis: Int, r: SizeRequirements?): SizeRequirements =
    super.calculateMinorAxisRequirements(axis, r).also {
      it.minimum = it.preferred
    }

  override fun getFlowSpan(index: Int) = Int.MAX_VALUE
}

private class NoWrapViewFactory : ViewFactory {
  override fun create(elem: Element) = when (elem.name) {
    AbstractDocument.ParagraphElementName -> NoWrapParagraphView(elem)
    AbstractDocument.SectionElementName -> BoxView(elem, View.Y_AXIS)
    StyleConstants.ComponentElementName -> ComponentView(elem)
    StyleConstants.IconElementName -> IconView(elem)
    else -> LabelView(elem)
  }
}

private class NoWrapEditorKit : StyledEditorKit() {
  override fun getViewFactory() = NoWrapViewFactory()
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
