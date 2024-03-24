package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.text.Utilities

fun makeUI(): Component {
  val txt = "The quick brown fox jumps over the lazy dog\n"
  val textArea = object : JTextArea(txt) {
    override fun getToolTipText(e: MouseEvent) = runCatching {
      val pos = viewToModel(e.point)
      val start = Utilities.getWordStart(this, pos)
      val end = Utilities.getWordEnd(this, pos)
      val word = getText(start, end - start)
      if (word.isNotBlank()) "%s(%d-%d)".format(word, start, end) else null
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(this)
    }.getOrNull()
  }
  textArea.lineWrap = true
  ToolTipManager.sharedInstance().registerComponent(textArea)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
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
