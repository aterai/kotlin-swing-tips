package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.plaf.LayerUI
import javax.swing.text.BadLocationException
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter
import javax.swing.text.JTextComponent
import kotlin.math.roundToInt

private val WARNING = Color(0xFF_C8_C8)
private val INIT_TXT = """
  Trail: Creating a GUI with JFC/Swing
  https://docs.oracle.com/javase/tutorial/uiswing/learn/index.html
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

private val currentPainter = DefaultHighlightPainter(Color.ORANGE)
private val matchPainter = DefaultHighlightPainter(Color.YELLOW)
private val textArea = JTextArea(INIT_TXT)
private val field = JTextField("Swing")
private val checkCase = JCheckBox("Match case")
private val checkWord = JCheckBox("Match whole word only")
private val layerUI = MatchCountLayerUI<JTextComponent>()
private var current = 0

fun createUI(): Component {
  textArea.isEditable = false

  val prevButton = JButton("⋀")
  prevButton.actionCommand = "prev"

  val nextButton = JButton("⋁")
  nextButton.actionCommand = "next"

  val handler = HighlightHandler()
  field.document.addDocumentListener(handler)
  listOf<AbstractButton>(prevButton, nextButton, checkCase, checkWord)
    .forEach {
      it.isFocusable = false
      it.addActionListener(handler)
    }
  val buttonPanel = JPanel(GridLayout(1, 2))
  buttonPanel.add(prevButton)
  buttonPanel.add(nextButton)

  val checkPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
  checkPanel.add(checkCase)
  checkPanel.add(checkWord)

  val searchPanel = JPanel(BorderLayout(5, 5))
  searchPanel.border = BorderFactory.createTitledBorder("Search")
  searchPanel.add(JLayer(field, layerUI))
  searchPanel.add(buttonPanel, BorderLayout.EAST)
  searchPanel.add(checkPanel, BorderLayout.SOUTH)

  EventQueue.invokeLater { changeHighlight() }

  return JPanel(BorderLayout()).also {
    it.add(searchPanel, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightHandler :
  DocumentListener,
  ActionListener {
  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  override fun insertUpdate(e: DocumentEvent) {
    changeHighlight()
  }

  override fun removeUpdate(e: DocumentEvent) {
    changeHighlight()
  }

  override fun actionPerformed(e: ActionEvent) {
    (e.source as? AbstractButton)?.also {
      val cmd = it.actionCommand
      if ("prev" == cmd) {
        current--
      } else if ("next" == cmd) {
        current++
      }
    }
    changeHighlight()
  }
}

private fun changeHighlight() {
  // Restore the background color that may have been changed by an invalid pattern
  field.background = UIManager.getColor("TextField.background")
  val regex = getRegex(field.text)
  current = if (regex == null) {
    // Clear the stale highlights of the previous search
    textArea.highlighter.removeAllHighlights()
    -1
  } else {
    highlightCurrentMatch(textArea, regex, current)
  }
  val hits = textArea.highlighter.highlights.size
  val label = layerUI.hint
  label.isOpaque = hits == 0
  label.text = "%02d / %02d".format(current + 1, hits)
  field.repaint()
}

private fun getRegex(text: String?): Regex? {
  if (text.isNullOrEmpty()) {
    return null
  }
  // Wrap the input in a non-capturing group so that the word boundaries
  // also apply to every alternative of a pattern such as "a|b"
  val pattern = if (checkWord.isSelected) "\\b(?:$text)\\b" else text
  val op = if (checkCase.isSelected) {
    emptySet<RegexOption>()
  } else {
    // Regex adds UNICODE_CASE automatically when IGNORE_CASE is specified
    setOf(RegexOption.IGNORE_CASE)
  }
  return runCatching {
    pattern.toRegex(op)
  }.onFailure {
    field.background = WARNING
  }.getOrNull()
}

private fun highlightAllMatches(
  editor: JTextComponent,
  regex: Regex,
): Highlighter {
  val highlighter = editor.highlighter
  highlighter.removeAllHighlights()
  val doc = editor.document
  val text = doc.getText(0, doc.length)
  // Skip empty matches (e.g. "a*"), Regex#findAll(...) advances past them by itself
  regex.findAll(text).map { it.range }.filterNot { it.isEmpty() }.forEach {
    highlighter.addHighlight(it.first, it.last + 1, matchPainter)
  }
  return highlighter
}

private fun highlightCurrentMatch(
  editor: JTextComponent,
  regex: Regex,
  index: Int,
): Int {
  val highlighter = highlightAllMatches(editor, regex)
  val highlights = highlighter.highlights
  val hits = highlights.size
  var current = -1
  if (hits > 0) {
    // Wrap around in both directions (the previous button decrements the index)
    current = Math.floorMod(index, hits)
    val hh = highlights[current]
    highlighter.removeHighlight(hh)
    val start = hh.startOffset
    highlighter.addHighlight(start, hh.endOffset, currentPainter)
    scrollToCenter(editor, start)
  }
  return current
}

// JTextComponent#modelToView(int) is kept for Java 8 compatibility
@Suppress("DEPRECATION")
@Throws(BadLocationException::class)
private fun scrollToCenter(
  tc: JTextComponent,
  pos: Int,
) {
  val rect = tc.modelToView(pos)
  // Java 9: val rect = tc.modelToView2D(pos).getBounds()
  val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, tc)
  if (rect != null && c is JViewport) {
    // Expand the rectangle to the size of the viewport around its center
    // so that the target position is scrolled to the center of the viewport
    val w = c.width
    val h = c.height
    rect.x = (rect.x + rect.width / 2f - w / 2f).roundToInt()
    rect.y = (rect.y + rect.height / 2f - h / 2f).roundToInt()
    rect.setSize(w, h)
    tc.scrollRectToVisible(rect)
  }
}

private class MatchCountLayerUI<V : JTextComponent> : LayerUI<V>() {
  val hint = object : JLabel() {
    override fun updateUI() {
      super.updateUI()
      foreground = UIManager.getColor("TextField.inactiveForeground")
      background = Color.RED
    }
  }

  override fun updateUI(l: JLayer<out V>) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(hint)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val layer = c as? JLayer<*>
    val tc = layer?.view
    if (tc is JTextComponent && tc.text.isNotEmpty()) {
      val g2 = g.create() as? Graphics2D ?: return
      val d = hint.preferredSize
      val r = SwingUtilities.calculateInnerArea(tc, null)
      val x = r.x + r.width - d.width - 1
      val y = r.y + (r.height - d.height) / 2
      SwingUtilities.paintComponent(g2, hint, tc, x, y, d.width, d.height)
      g2.dispose()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
