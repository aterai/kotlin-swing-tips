package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.text.JTextComponent
import javax.swing.text.Utilities
import kotlin.math.min

private val TEXT = """
  Trail: Creating a GUI with JFC/Swing
  Lesson: Learning Swing by Example
  This lesson explains the concepts you need to
    use Swing components in building a user interface.
  First we examine the simplest Swing application you can write.
  Then we present several progressively complicated examples of creating
    user interfaces using components in the javax.swing package.
  We cover several Swing components, such as buttons, labels, and text areas.
  The handling of events is also discussed,
    as are layout management and accessibility.
  This lesson ends with a set of questions and exercises
    so you can test yourself on what you've learned.
  https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
""".trimIndent()
private val HELP = """
  cursor-end: ctrl E
  caret-end: ctrl END(default)
  caret-end-line: END(default)
  caret-next-word: ctrl RIGHT, ctrl KP_RIGHT(default)
  caret-end-paragraph: ctrl P
  caret-end-word: ctrl D
  --------
""".trimIndent()

fun makeUI(): Component {
  val textArea = JTextArea(HELP + TEXT)
  textArea.lineWrap = true
  textArea.wrapStyleWord = true
  textArea.actionMap.put("cursor-end", CursorEndAction())
  val im = textArea.getInputMap(JComponent.WHEN_FOCUSED)
  im.put(KeyStroke.getKeyStroke("ctrl E"), "cursor-end")
  im.put(KeyStroke.getKeyStroke("ctrl P"), "caret-end-paragraph")
  im.put(KeyStroke.getKeyStroke("ctrl D"), "caret-end-word")
  val check = JCheckBox("line wrap:", true)
  check.addActionListener {
    val b = (it.source as? JCheckBox)?.isSelected == true
    textArea.lineWrap = b
    textArea.wrapStyleWord = b
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class CursorEndAction : AbstractAction() {
  override fun actionPerformed(e: ActionEvent) {
    val c = e.source
    if (c is JTextComponent) {
      val offs = c.caretPosition
      val rowEndOffs = runCatching {
        Utilities.getRowEnd(c, offs)
      }.getOrNull() ?: return
      if (rowEndOffs == offs) {
        val length = c.document.length
        val end = Utilities.getParagraphElement(c, offs).endOffset
        c.caretPosition = min(length, end - 1)
      } else {
        c.caretPosition = rowEndOffs
      }
    }
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
