package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStreamWriter
import java.io.Reader
import java.io.StringReader
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.JTextComponent
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext
import javax.swing.text.html.MinimalHTMLWriter

fun makeUI(): Component {
  val doc = SimpleSyntaxDocument()
  val def = doc.getStyle(StyleContext.DEFAULT_STYLE)
  StyleConstants.setForeground(doc.addStyle("red", def), Color.RED)
  StyleConstants.setForeground(doc.addStyle("green", def), Color.GREEN)
  StyleConstants.setForeground(doc.addStyle("blue", def), Color.BLUE)

  val textPane = JTextPane(doc)
  textPane.text = "JTextPane(StyledDocument)\nred green, blue."
  textPane.selectedTextColor = null
  textPane.selectionColor = Color(0x64_32_64_FF, true)
  textPane.componentPopupMenu = TextComponentPopupMenu()

  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(textPane))
    it.add(JScrollPane(JEditorPane("text/html", "JEditorPane")))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TextComponentPopupMenu : JPopupMenu() {
  init {
    add(DefaultEditorKit.CutAction())
    add(DefaultEditorKit.CopyAction())
    add(DefaultEditorKit.PasteAction())
    add("delete").addActionListener {
      (invoker as? JTextComponent)?.replaceSelection(null)
    }
    addSeparator()
    add("copy-html-and-text-to-clipboard").addActionListener {
      val c = invoker
      if (c is JTextPane) {
        copyHtmlTextToClipboard(c)
      }
    }
    add("copy-html-to-clipboard").addActionListener {
      val c = invoker
      if (c is JTextPane) {
        copyHtmlToClipboard(c)
      }
    }
  }

  fun copyHtmlTextToClipboard(textPane: JTextPane) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val start = textPane.selectionStart
    val end = textPane.selectionEnd
    val length = end - start
    val doc = textPane.styledDocument
    runCatching {
      ByteArrayOutputStream().use { os ->
        OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
          val w = MinimalHTMLWriter(writer, doc, start, length)
          w.write()
          writer.flush()
          val contents = os.toString()
          val plain = doc.getText(start, length)
          val transferable: Transferable = BasicTransferable(plain, contents)
          clipboard.setContents(transferable, null)
        }
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(textPane)
      it.printStackTrace()
    }
  }

  fun copyHtmlToClipboard(textPane: JTextPane) {
    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
    val start = textPane.selectionStart
    val end = textPane.selectionEnd
    val length = end - start
    val styledDocument = textPane.styledDocument
    runCatching {
      ByteArrayOutputStream().use { os ->
        OutputStreamWriter(os, StandardCharsets.UTF_8).use { writer ->
          val w = MinimalHTMLWriter(writer, styledDocument, start, length)
          w.write()
          writer.flush()
          val contents = os.toString()
          val htmlTransferable: Transferable = HtmlTransferable(contents)
          clipboard.setContents(htmlTransferable, null)
        }
      }
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(textPane)
      it.printStackTrace()
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTextComponent) {
      val hasSelectedText = c.selectedText != null
      for (menuElement in subElements) {
        val m = menuElement.component
        if (m is JMenuItem && m.action is DefaultEditorKit.PasteAction) {
          continue
        }
        m.isEnabled = hasSelectedText
      }
      super.show(c, x, y)
    }
  }
}

private class BasicTransferable(
  private var plainData: String,
  private var htmlData: String
) : Transferable {
  private val htmlFlavors = arrayOf(
    DataFlavor("text/html;class=java.lang.String"),
    DataFlavor("text/html;class=java.io.Reader"),
    DataFlavor("text/html;charset=unicode;class=java.io.InputStream")
  )
  private val plainFlavors = arrayOf(
    DataFlavor("text/plain;class=java.lang.String"),
    DataFlavor("text/plain;class=java.io.Reader"),
    DataFlavor("text/plain;charset=unicode;class=java.io.InputStream")
  )
  private val stringFlavors = arrayOf(
    DataFlavor(DataFlavor.javaJVMLocalObjectMimeType + ";class=java.lang.String"),
    DataFlavor.stringFlavor
  )
  private val isHtmlSupported = true
  private val isPlainSupported = true

  fun getTextCharset(flavor: DataFlavor): String =
    flavor.getParameter("charset") ?: Charset.defaultCharset().name()

  override fun getTransferDataFlavors(): Array<DataFlavor> {
    val flavors = mutableListOf<DataFlavor>()
    if (isHtmlSupported) {
      flavors.addAll(htmlFlavors)
    }
    if (isPlainSupported) {
      flavors.addAll(plainFlavors)
      flavors.addAll(stringFlavors)
    }
    return flavors.toTypedArray()
  }

  override fun isDataFlavorSupported(flavor: DataFlavor) = transferDataFlavors.any {
    it.equals(flavor)
  }

  @Throws(UnsupportedFlavorException::class, IOException::class)
  override fun getTransferData(flavor: DataFlavor): Any {
    return when {
      // richerFlavors.any { it.equals(flavor) } -> getRicherData
      htmlFlavors.any { it.equals(flavor) } -> getHtmlTransferData(flavor)
      plainFlavors.any { it.equals(flavor) } -> getPlaneTransferData(flavor)
      stringFlavors.any { it.equals(flavor) } -> plainData
      else -> throw UnsupportedFlavorException(flavor)
    }
  }

  private fun createInputStream(flavor: DataFlavor, data: String) =
    ByteArrayInputStream(data.toByteArray(charset(getTextCharset(flavor))))

  @Throws(IOException::class, UnsupportedFlavorException::class)
  private fun getHtmlTransferData(flavor: DataFlavor?): Any = when (flavor?.representationClass) {
    String::class.java -> htmlData
    Reader::class.java -> StringReader(htmlData)
    InputStream::class.java -> createInputStream(flavor, htmlData)
    else -> throw UnsupportedFlavorException(flavor)
  }

  @Throws(IOException::class, UnsupportedFlavorException::class)
  fun getPlaneTransferData(flavor: DataFlavor?): Any = when (flavor?.representationClass) {
    String::class.java -> plainData
    Reader::class.java -> StringReader(plainData)
    InputStream::class.java -> createInputStream(flavor, plainData)
    else -> UnsupportedFlavorException(flavor)
  }
}

private class SimpleSyntaxDocument : DefaultStyledDocument() {
  @Throws(BadLocationException::class)
  override fun insertString(offset: Int, text: String, a: AttributeSet?) {
    super.insertString(offset, text, a)
    processChangedLines(offset, text.length)
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
    val endLine = root.getElementIndex(offset + length) + 1
    for (i in startLine until endLine) {
      applyHighlighting(content, i)
    }
  }

  @Throws(BadLocationException::class)
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

private class HtmlTransferable(private val htmlFormattedText: String) : Transferable {
  override fun getTransferDataFlavors() = arrayOf(DataFlavor.allHtmlFlavor)

  override fun isDataFlavorSupported(flavor: DataFlavor): kotlin.Boolean {
    for (supportedFlavor in transferDataFlavors) {
      if (supportedFlavor.equals(flavor)) {
        return true
      }
    }
    return false
  }

  @Throws(UnsupportedFlavorException::class, IOException::class)
  override fun getTransferData(flavor: DataFlavor): Any {
    if (flavor == DataFlavor.allHtmlFlavor) {
      return htmlFormattedText
    }
    throw UnsupportedFlavorException(flavor)
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
      minimumSize = Dimension(256, 200)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
