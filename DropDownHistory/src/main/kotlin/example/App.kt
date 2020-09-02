package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.regex.Pattern
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.JTextComponent

private const val TEXT =
"""
Trail: Creating a GUI with JFC/Swing
 Lesson: Learning Swing by Example
 This lesson explains the concepts you need to
  use Swing components in building a user interface.
  First we examine the simplest Swing application you can write.
  Then we present several progressively complicated examples of creating
  user interfaces using components in the javax.swing package.
  We cover several Swing components, such as buttons, labels, and text areas.
  The handling of events is also discussed, as are layout management and accessibility.
  This lesson ends with a set of questions and exercises
  so you can test yourself on what you've learned.
https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
"""
private val textArea = JTextArea()
private val combo = JComboBox<String>()
private val HIGHLIGHT = DefaultHighlightPainter(Color.YELLOW)

fun makeUI(): Component {
  textArea.text = TEXT
  textArea.lineWrap = true
  textArea.isEditable = false
  val model = DefaultComboBoxModel<String>()
  model.addElement("swing")
  combo.model = model
  combo.isEditable = true
  val searchButton = JButton("Search")
  searchButton.addActionListener {
    val pattern = combo.editor.item.toString()
    if (addItem(combo, pattern, 4)) {
      setHighlight(textArea, pattern)
    } else {
      textArea.highlighter.removeAllHighlights()
    }
  }
  EventQueue.invokeLater { combo.rootPane.defaultButton = searchButton }

  val p = JPanel(BorderLayout(5, 5))
  p.border = BorderFactory.createEmptyBorder(0, 5, 5, 0)
  p.add(JLabel("Search History:"), BorderLayout.WEST)
  p.add(combo)
  p.add(searchButton, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

fun addItem(combo: JComboBox<String>, str: String?, max: Int): Boolean {
  if (str?.isEmpty() == true) {
    return false
  }
  combo.isVisible = false
  (combo.model as? DefaultComboBoxModel<String>)?.also {
    it.removeElement(str)
    it.insertElementAt(str, 0)
    if (it.size > max) {
      it.removeElementAt(max)
    }
  }
  combo.selectedIndex = 0
  combo.isVisible = true
  return true
}

private fun setHighlight(jtc: JTextComponent, pattern: String) {
  val highlighter = jtc.highlighter
  highlighter.removeAllHighlights()
  runCatching {
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
