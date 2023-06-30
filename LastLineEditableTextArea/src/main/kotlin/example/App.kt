package example

import java.awt.*
import javax.swing.*
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.BadLocationException
import javax.swing.text.DocumentFilter

private const val MESSAGE = "Can only edit last line, version 0.0\n"

fun makeUI(): Component {
  val textArea = JTextArea()
  textArea.margin = Insets(2, 5, 2, 2)
  textArea.text = MESSAGE + NonEditableLineDocumentFilter.PROMPT
  (textArea.document as? AbstractDocument)?.documentFilter = NonEditableLineDocumentFilter()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class NonEditableLineDocumentFilter : DocumentFilter() {
  @Throws(BadLocationException::class)
  override fun insertString(
    fb: FilterBypass,
    offset: Int,
    text: String?,
    attr: AttributeSet?
  ) {
    if (text != null) {
      replace(fb, offset, 0, text, attr)
    }
  }

  @Throws(BadLocationException::class)
  override fun remove(
    fb: FilterBypass,
    offset: Int,
    length: Int
  ) {
    replace(fb, offset, length, "", null)
  }

  @Throws(BadLocationException::class)
  override fun replace(
    fb: FilterBypass,
    offset: Int,
    length: Int,
    text: String?,
    attrs: AttributeSet?
  ) {
    val doc = fb.document
    val root = doc.defaultRootElement
    val count = root.elementCount
    val index = root.getElementIndex(offset)
    val cur = root.getElement(index)
    val promptPosition = cur.startOffset + PROMPT.length
    if (index == count - 1 && offset - promptPosition >= 0) {
      var str = text
      if (LB == str) {
        val line = doc.getText(promptPosition, offset - promptPosition)
        val args = line.split(",").map { it.trim() }
          .filter { it.isNotEmpty() }
        str = if (args.isEmpty() || args[0].isEmpty()) {
          "%n%s".format(PROMPT)
        } else {
          "%n%s: command not found%n%s".format(args[0], PROMPT)
        }
      }
      fb.replace(offset, length, str, attrs)
    }
  }

  companion object {
    const val LB = "\n"
    const val PROMPT = "> "
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
