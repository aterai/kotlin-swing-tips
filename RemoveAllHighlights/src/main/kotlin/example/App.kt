package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.event.KeyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.CaretEvent
import javax.swing.event.CaretListener
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.JTextComponent
import javax.swing.text.Utilities

private const val TEXT = """
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
"""
private val HIGHLIGHT = DefaultHighlightPainter(Color.YELLOW)

fun makeUI(): Component {
  val textArea = object : JTextArea() {
    private var handler: WordHighlightListener? = null

    override fun updateUI() {
      removeCaretListener(handler)
      removeMouseListener(handler)
      removeKeyListener(handler)
      super.updateUI()
      handler = WordHighlightListener()
      addCaretListener(handler)
      addMouseListener(handler)
      addKeyListener(handler)
    }
  }
  textArea.selectedTextColor = Color.BLACK
  textArea.lineWrap = true
  textArea.text = TEXT

  val button1 = JButton("removeAllHighlights")
  button1.isFocusable = false
  button1.addActionListener { textArea.highlighter.removeAllHighlights() }

  val button2 = JButton("removeWordHighlights")
  button2.isFocusable = false
  button2.addActionListener { removeWordHighlights(textArea) }

  val box = Box.createHorizontalBox().also {
    it.add(button1)
    it.add(Box.createHorizontalStrut(2))
    it.add(button2)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class WordHighlightListener :
  MouseAdapter(),
  CaretListener,
  KeyListener {
  private var dragActive = false
  private var shiftActive = false

  override fun caretUpdate(e: CaretEvent) {
    if (!dragActive && !shiftActive) {
      fire(e.source)
    }
  }

  override fun mousePressed(e: MouseEvent) {
    dragActive = true
  }

  override fun mouseReleased(e: MouseEvent) {
    dragActive = false
    fire(e.source)
  }

  override fun keyPressed(e: KeyEvent) {
    shiftActive = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0
  }

  override fun keyReleased(e: KeyEvent) {
    shiftActive = e.modifiersEx and InputEvent.SHIFT_DOWN_MASK != 0
    if (!shiftActive) {
      fire(e.source)
    }
  }

  override fun keyTyped(e: KeyEvent) {
    // empty
  }

  private fun fire(c: Any) {
    if (c is JTextComponent) {
      val caret = c.caret
      val p0 = caret.dot.coerceAtMost(caret.mark)
      val p1 = caret.dot.coerceAtLeast(caret.mark)
      val offs = c.caretPosition
      runCatching {
        val word = if (p0 == p1) {
          val begOffs = Utilities.getWordStart(c, offs)
          val endOffs = Utilities.getWordEnd(c, offs)
          c.getText(begOffs, endOffs - begOffs)
        } else {
          c.selectedText
        }?.trim() ?: ""
        if (word.isEmpty()) {
          removeWordHighlights(c)
        } else {
          setHighlight(c, word)
        }
      }.onFailure {
        c.highlighter.removeAllHighlights()
      }
    }
  }
}

fun setHighlight(
  tc: JTextComponent,
  pattern: String,
) {
  removeWordHighlights(tc)
  runCatching {
    val highlighter = tc.highlighter
    val doc = tc.document
    val text = doc.getText(0, doc.length)
    pattern.toRegex().findAll(text).map { it.range }.filterNot { it.isEmpty() }.forEach {
      highlighter.addHighlight(it.first(), it.last() + 1, HIGHLIGHT)
    }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(tc)
  }
}

fun removeWordHighlights(tc: JTextComponent) {
  val highlighter = tc.highlighter
  for (hh in highlighter.highlights) {
    if (hh.painter == HIGHLIGHT) {
      highlighter.removeHighlight(hh)
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
