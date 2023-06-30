package example

import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultStyledDocument
import javax.swing.text.SimpleAttributeSet
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

private const val TEXT = "The quick brown fox jumps over the lazy dog."

private val textArea = JTextArea()

private fun append(str: String) {
  textArea.append("$str\n")
  textArea.caretPosition = textArea.document.length
}

fun makeUI(): Component {
  textArea.isEditable = false
  val style = StyleContext()
  val doc = DefaultStyledDocument(style)
  runCatching {
    doc.insertString(0, "$TEXT\n$TEXT", null)
  }
  val attr1 = SimpleAttributeSet()
  attr1.addAttribute(StyleConstants.Bold, true)
  attr1.addAttribute(StyleConstants.Foreground, Color.RED)
  doc.setCharacterAttributes(4, 11, attr1, false)
  val attr2 = SimpleAttributeSet()
  attr2.addAttribute(StyleConstants.Underline, true)
  doc.setCharacterAttributes(10, 20, attr2, false)
  val textPane = JTextPane(doc)
  textPane.addCaretListener { e ->
    if (e.dot == e.mark) {
      val a = doc.getCharacterElement(e.dot).attributes
      append("isBold: " + StyleConstants.isBold(a))
      append("isUnderline: " + StyleConstants.isUnderline(a))
      append("Foreground: " + StyleConstants.getForeground(a))
      append("FontFamily: " + StyleConstants.getFontFamily(a))
      append("FontSize: " + StyleConstants.getFontSize(a))
      append("Font: " + style.getFont(a))
      append("----")
    }
  }
  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.resizeWeight = .5
  sp.topComponent = JScrollPane(textPane)
  sp.bottomComponent = JScrollPane(textArea)
  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
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
