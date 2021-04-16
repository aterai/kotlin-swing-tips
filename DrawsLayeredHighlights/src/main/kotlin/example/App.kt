package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter

private val WARNING_COLOR = Color(0xFF_C8_C8)
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
""".trimIndent()
private val highlightPainter = DefaultHighlightPainter(Color.YELLOW)
private val field = JTextField("Swing")
private val textArea = JTextArea(TEXT)

fun makeUI(): Component {
  textArea.isEditable = false

  val check = JCheckBox("DefaultHighlighter#setDrawsLayeredHighlights", true)
  check.isFocusable = false
  check.addActionListener { e ->
    (textArea.highlighter as? DefaultHighlighter)?.also {
      it.drawsLayeredHighlights = (e.source as? JCheckBox)?.isSelected == true
      fireDocumentChangeEvent()
      textArea.select(textArea.selectionStart, textArea.selectionEnd)
    }
  }

  val dl = object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun removeUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun changedUpdate(e: DocumentEvent) {
      /* not needed */
    }
  }
  field.document.addDocumentListener(dl)
  fireDocumentChangeEvent()

  EventQueue.invokeLater {
    textArea.requestFocusInWindow()
    textArea.selectAll()
  }

  val sp = JPanel(BorderLayout(5, 5))
  sp.add(JLabel("regex pattern:"), BorderLayout.WEST)
  sp.add(field)
  sp.add(Box.createVerticalStrut(2), BorderLayout.SOUTH)
  sp.border = BorderFactory.createTitledBorder("Search")

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(Box.createHorizontalStrut(2))

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(sp, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun fireDocumentChangeEvent() {
  field.background = Color.WHITE
  val pattern = field.text.trim()
  val highlighter = textArea.highlighter
  highlighter.removeAllHighlights()
  if (pattern.isEmpty()) {
    return
  }
  val doc = textArea.document
  runCatching {
    val text = doc.getText(0, doc.length)
    val matcher = Pattern.compile(pattern).matcher(text)
    var pos = 0
    while (matcher.find(pos) && matcher.group().isNotEmpty()) {
      val start = matcher.start()
      val end = matcher.end()
      highlighter.addHighlight(start, end, highlightPainter)
      pos = end
    }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(field)
    field.background = WARNING_COLOR
  }
  field.rootPane?.repaint()
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
