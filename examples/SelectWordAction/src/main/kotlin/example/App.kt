package example

import java.awt.*
import java.awt.event.ActionEvent
import java.text.BreakIterator
import javax.swing.*
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultEditorKit
import javax.swing.text.Document
import javax.swing.text.Element
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

fun createUI(): Component {
  val textArea = JTextArea(TEXT)
  val action = object : TextAction(DefaultEditorKit.selectWordAction) {
    override fun actionPerformed(e: ActionEvent) {
      getTextComponent(e)?.also { target ->
        runCatching {
          val pos = target.caretPosition
          val start = TextUtils.getWordStart(target, pos)
          val end = TextUtils.getWordEnd(target, pos)
          target.caretPosition = start
          target.moveCaretPosition(end)
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(target)
        }
      }
    }
  }
  textArea.actionMap.put(DefaultEditorKit.selectWordAction, action)
  val c1 = makeTitledPanel("Default", JTextArea(TEXT))
  val c2 = makeTitledPanel("Break words: _ and -", textArea)
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, c1, c2)
  split.resizeWeight = .5
  split.preferredSize = Dimension(320, 240)
  return split
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.add(JLabel(title), BorderLayout.NORTH)
  p.add(JScrollPane(c))
  return p
}

private object TextUtils {
  private const val DELIMITERS = "_-"

  // @see javax.swing.text.Utilities.getWordStart(...)
  @Throws(BadLocationException::class)
  fun getWordStart(
    c: JTextComponent,
    offs: Int,
  ): Int {
    val line = getParagraphElement(c, offs)
    val lineStart = line.startOffset
    val seg = getLineText(c.document, line)
    var start = offs
    if (seg.count > 0) {
      val words = BreakIterator.getWordInstance(c.locale)
      words.text = seg
      // Clamp to the last character of the line (e.g. clicked past the line end)
      val pos = (offs - lineStart).coerceAtMost(seg.count - 1)
      // BreakIterator indices start at seg.offset (the Segment's begin index),
      // while Segment#charAt(int) takes an index relative to the line start
      words.following(seg.offset + pos)
      start = lineStart + words.previous() - seg.offset
      for (i in lineStart + pos downTo start + 1) {
        if (isDelimiter(seg[i - lineStart])) {
          start = i + 1
          break
        }
      }
    }
    return start
  }

  // @see javax.swing.text.Utilities.getWordEnd(...)
  @Throws(BadLocationException::class)
  fun getWordEnd(
    c: JTextComponent,
    offs: Int,
  ): Int {
    val line = getParagraphElement(c, offs)
    val lineStart = line.startOffset
    val seg = getLineText(c.document, line)
    var end = offs
    if (seg.count > 0) {
      val words = BreakIterator.getWordInstance(c.locale)
      words.text = seg
      val pos = (offs - lineStart).coerceAtMost(seg.count - 1)
      end = lineStart + words.following(seg.offset + pos) - seg.offset
      for (i in offs..<end) {
        if (isDelimiter(seg[i - lineStart])) {
          end = i
          break
        }
      }
    }
    return end
  }

  private fun isDelimiter(ch: Char) = ch in DELIMITERS

  @Throws(BadLocationException::class)
  private fun getParagraphElement(
    c: JTextComponent,
    offs: Int,
  ): Element =
    Utilities.getParagraphElement(c, offs)
      ?: throw BadLocationException("No word at $offs", offs)

  // Excludes the implicit newline at the end of the document
  @Throws(BadLocationException::class)
  private fun getLineText(
    doc: Document,
    line: Element,
  ): Segment {
    val lineStart = line.startOffset
    val lineEnd = line.endOffset.coerceAtMost(doc.length)
    val seg = Segment()
    doc.getText(lineStart, lineEnd - lineStart, seg)
    return seg
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
