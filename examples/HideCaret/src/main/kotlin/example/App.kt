package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.text.DefaultCaret

private val textArea = JTextArea("11111111111111\n22222\n3333")

fun makeUI(): Component {
  val hidingCaret = object : DefaultCaret() {
    override fun isVisible() = false
  }
  val defaultCaret = textArea.caret
  val defaultHighlighter = textArea.highlighter

  val hidingCaretCheck = JCheckBox("Hide Caret")
  hidingCaretCheck.addActionListener { e ->
    textArea.caret = if (isSelected(e)) hidingCaret else defaultCaret
  }

  val hidingHighlighterCheck = JCheckBox("Hide Highlighter")
  hidingHighlighterCheck.addActionListener { e ->
    textArea.highlighter = if (isSelected(e)) null else defaultHighlighter
  }

  val editableCheck = JCheckBox("Editable", true)
  editableCheck.addActionListener { e ->
    textArea.isEditable = isSelected(e)
  }

  val focusableCheck = JCheckBox("Focusable", true)
  focusableCheck.addActionListener { e ->
    textArea.isFocusable = isSelected(e)
  }

  val p1 = JPanel()
  p1.add(hidingCaretCheck)
  p1.add(hidingHighlighterCheck)

  val p2 = JPanel()
  p2.add(editableCheck)
  p2.add(focusableCheck)

  val p = JPanel(BorderLayout(0, 0))
  p.add(p1, BorderLayout.NORTH)
  p.add(p2, BorderLayout.SOUTH)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.add(JTextField(), BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun isSelected(e: ActionEvent) = (e.source as? JCheckBox)?.isSelected == true

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
