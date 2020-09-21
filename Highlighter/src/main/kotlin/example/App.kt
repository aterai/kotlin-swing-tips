package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.JTextComponent

private const val PATTERN = "Swing"
private const val TEXT = """
Trail: Creating a GUI with JFC/Swing
Lesson: Learning Swing by Example
This lesson explains the concepts you need to use Swing components in building a user inter face.
First we examine the simplest Swing application you can write.
Then we present several progressively complicated examples of creating user interfaces using components
 in the javax.swing package.
We cover several Swing components, such as buttons, labels, and text areas.
The handling of events is also discussed, as are layout management and accessibility.
This lesson ends with a set of questions and  exercises so you can test yourself on what you've learned.
https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
"""
private val HIGHLIGHT = DefaultHighlightPainter(Color.YELLOW)

fun makeUI(): Component {
  val jta = JTextArea()
  jta.lineWrap = true
  jta.text = TEXT

  val highlight = JButton("highlight: $PATTERN")
  highlight.addActionListener {
    jta.isEditable = false
    setHighlight(jta, PATTERN)
  }

  val clear = JButton("clear")
  clear.addActionListener {
    jta.isEditable = true
    jta.highlighter.removeAllHighlights()
  }

  val box = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(highlight)
    it.add(clear)
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(jta))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun setHighlight(jtc: JTextComponent, pattern: String) {
  jtc.highlighter.removeAllHighlights()
  runCatching {
    val highlighter = jtc.highlighter
    val doc = jtc.document
    val text = doc.getText(0, doc.length)
    val matcher = Pattern.compile(pattern).matcher(text)
    var pos = 0
    while (matcher.find(pos) && matcher.group().isNotEmpty()) {
      pos = matcher.end()
      highlighter.addHighlight(matcher.start(), pos, HIGHLIGHT)
    }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(jtc)
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
