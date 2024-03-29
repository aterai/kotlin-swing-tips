package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.JTextComponent
import javax.swing.text.PlainDocument

fun makeUI(): Component {
  val txt1 = """
    1	aaa
    12	bbb
    123	ccc
  """.trimIndent()
  val textPane = JTextPane()
  textPane.text = "JTextPane:$txt1"

  val textArea = JTextArea("JTextArea:$txt1")
  textArea.tabSize = 4

  val p = JPanel(GridLayout(2, 1, 5, 5))
  p.add(JScrollPane(textPane))
  p.add(JScrollPane(textArea))

  val txt2 = "aaa\tbbb\tccc"
  val field1 = JTextField(txt2, 20)
  initActionInputMap(field1)
  val tabSize = 4
  val doc = PlainDocument()
  doc.putProperty(PlainDocument.tabSizeAttribute, tabSize)

  val field2 = JTextField(doc, txt2, 20)
  initActionInputMap(field2)
  val model = SpinnerNumberModel(tabSize, -2, 12, 1)
  model.addChangeListener { setTabSize(field2, model.number.toInt()) }
  val spinner = JSpinner(model)

  val box = Box.createVerticalBox().also {
    it.add(field1)
    it.add(Box.createVerticalStrut(5))
    it.add(field2)
    it.add(Box.createVerticalStrut(5))
    it.add(spinner)
  }

  return JPanel(BorderLayout(5, 5)).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initActionInputMap(editor: JTextComponent) {
  val mapKey = "insert-horizontal-tab"
  val a = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      runCatching {
        editor.document.insertString(editor.caretPosition, "\t", null)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(editor)
      }
    }
  }
  editor.actionMap.put(mapKey, a)
  val keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_I, InputEvent.CTRL_DOWN_MASK)
  editor.getInputMap(JPanel.WHEN_FOCUSED).put(keyStroke, mapKey)
}

private fun setTabSize(
  editor: JTextComponent,
  size: Int,
) {
  val doc = editor.document
  if (doc != null) {
    doc.putProperty(PlainDocument.tabSizeAttribute, size)
    editor.isEditable = false
    editor.isEditable = true
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
