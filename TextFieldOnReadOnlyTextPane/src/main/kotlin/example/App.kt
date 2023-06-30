package example

import java.awt.*
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import javax.swing.*
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

fun makeUI(): Component {
  val textPane = JTextPane()
  textPane.isEditable = false
  textPane.margin = Insets(0, 10, 0, 0)
  insertQuestion(textPane, "111 / 37 = ")
  insertQuestion(textPane, "222 / 37 = ")
  insertQuestion(textPane, "333 / 37 = ")
  insertQuestion(textPane, "444 / 37 = ")
  insertQuestion(textPane, "555 / 37 = ")
  insertQuestion(textPane, "666 / 37 = ")
  insertQuestion(textPane, "777 / 37 = ")
  insertQuestion(textPane, "888 / 37 = ")
  insertQuestion(textPane, "999 / 37 = ")

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun insertQuestion(textPane: JTextPane, str: String) {
  val doc = textPane.document
  runCatching {
    doc.insertString(doc.length, str, null)
    val pos = doc.length
    // println(pos)
    val field = object : JTextField(4) {
      override fun getMaximumSize() = preferredSize
    }
    field.border = BorderFactory.createMatteBorder(0, 0, 1, 0, Color.BLACK)
    val fl = object : FocusAdapter() {
      override fun focusGained(e: FocusEvent) {
        runCatching {
          val rect = textPane.modelToView(pos)
          rect.grow(0, 4)
          rect.size = field.size
          textPane.scrollRectToVisible(rect)
        }
      }
    }
    field.addFocusListener(fl)
    val d = field.preferredSize
    val baseline = field.getBaseline(d.width, d.height)
    field.alignmentY = baseline / d.height.toFloat()

    val a = textPane.getStyle(StyleContext.DEFAULT_STYLE)
    StyleConstants.setLineSpacing(a, 1.5f)
    textPane.setParagraphAttributes(a, true)
    textPane.insertComponent(field)
    doc.insertString(doc.length, "\n", null)
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
