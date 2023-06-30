package example

import java.awt.*
import java.awt.event.ActionEvent
import java.text.BreakIterator
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.JTextComponent
import javax.swing.text.Segment
import javax.swing.text.TextAction
import javax.swing.text.Utilities

private val TEXT = """
  AA-BB_CC
  AA-bb_CC
  aa1-bb2_cc3
  aa_(bb)_cc;
  11-22_33
""".trimIndent()

fun makeUI(): Component {
  val textArea = JTextArea(TEXT)
  val action = object : TextAction(DefaultEditorKit.selectWordAction) {
    override fun actionPerformed(e: ActionEvent) {
      getTextComponent(e)?.also { target ->
        runCatching {
          val offs = target.caretPosition
          val begOffs = TextUtils.getWordStart(target, offs)
          val endOffs = TextUtils.getWordEnd(target, offs)
          target.caretPosition = begOffs
          target.moveCaretPosition(endOffs)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(target)
        }
      }
    }
  }
  textArea.actionMap.put(DefaultEditorKit.selectWordAction, action)
  val split = JSplitPane()
  split.resizeWeight = .5
  split.leftComponent = makeTitledPanel("Default", JTextArea(TEXT))
  split.rightComponent = makeTitledPanel("Break words: _ and -", textArea)
  split.preferredSize = Dimension(320, 240)
  return split
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.add(JLabel(title), BorderLayout.NORTH)
  p.add(JScrollPane(c))
  return p
}

private object TextUtils {
  // @see javax.swing.text.Utilities.getWordStart(...)
  @Throws(BadLocationException::class)
  fun getWordStart(c: JTextComponent, offs: Int): Int {
    val line = Utilities.getParagraphElement(c, offs)
      ?: throw BadLocationException("No word at $offs", offs)
    val doc = c.document
    val lineStart = line.startOffset
    val lineEnd = line.endOffset.coerceAtMost(doc.length)
    var offs2 = offs
    val seg = Segment()
    doc.getText(lineStart, lineEnd - lineStart, seg)
    if (seg.count <= 0) {
      return offs2
    }
    val words = BreakIterator.getWordInstance(c.locale)
    words.text = seg
    var wordPosition = seg.offset + offs - lineStart
    if (wordPosition >= words.last()) {
      wordPosition = words.last() - 1
      words.following(wordPosition)
      offs2 = lineStart + words.previous() - seg.offset
    } else {
      words.following(wordPosition)
      offs2 = lineStart + words.previous() - seg.offset
      for (i in offs downTo offs2 + 1) {
        val ch = seg[i - seg.offset]
        if (ch == '_' || ch == '-') {
          offs2 = i + 1
          break
        }
      }
    }
    return offs2
  }

  // @see javax.swing.text.Utilities.getWordEnd(...)
  @Throws(BadLocationException::class)
  fun getWordEnd(c: JTextComponent, offs: Int): Int {
    val line = Utilities.getParagraphElement(c, offs)
      ?: throw BadLocationException("No word at $offs", offs)
    val doc = c.document
    val lineStart = line.startOffset
    val lineEnd = line.endOffset.coerceAtMost(doc.length)
    var offs2 = offs
    val seg = Segment()
    doc.getText(lineStart, lineEnd - lineStart, seg)
    if (seg.count > 0) {
      val words = BreakIterator.getWordInstance(c.locale)
      words.text = seg
      var wordPosition = offs - lineStart + seg.offset
      if (wordPosition >= words.last()) {
        wordPosition = words.last() - 1
      }
      offs2 = lineStart + words.following(wordPosition) - seg.offset
      for (i in offs until offs2) {
        val ch = seg[i - seg.offset]
        if (ch == '_' || ch == '-') {
          offs2 = i
          break
        }
      }
    }
    return offs2
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
