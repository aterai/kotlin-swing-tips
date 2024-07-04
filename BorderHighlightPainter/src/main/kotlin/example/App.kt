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
import javax.swing.text.JTextComponent
import javax.swing.text.View

private val WARNING_COLOR = Color(0xFF_C8_C8)
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

private val currentPainter = object : DefaultHighlightPainter(Color.ORANGE) {
  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape?,
    c: JTextComponent?,
    view: View?,
  ) = super.paintLayer(g, offs0, offs1, bounds, c, view)?.also {
    val g2 = g.create()
    if (it is Rectangle && g2 is Graphics2D) {
      g2.paint = Color.RED
      val r = it.getBounds()
      g2.drawRect(r.x, r.y, r.width - 1, r.height - 1)
    }
    g2.dispose()
  }
}
private val highlightPainter = DefaultHighlightPainter(Color.ORANGE)
private val textArea = JTextArea(INIT_TXT)
private val field = JTextField("Swing")
private val checkCase = JCheckBox("Match case")
private val checkWord = JCheckBox("Match whole word only")
private val layerUI = PlaceholderLayerUI<JTextComponent>()
private var current = 0

fun makeUI(): Component {
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
  val bp = JPanel(GridLayout(1, 2))
  bp.add(prevButton)
  bp.add(nextButton)

  val cp = JPanel(FlowLayout(FlowLayout.RIGHT))
  cp.add(checkCase)
  cp.add(checkWord)

  val sp = JPanel(BorderLayout(5, 5))
  sp.border = BorderFactory.createTitledBorder("Search")
  sp.add(JLayer(field, layerUI))
  sp.add(bp, BorderLayout.EAST)
  sp.add(cp, BorderLayout.SOUTH)

  EventQueue.invokeLater { changeHighlight() }

  return JPanel(BorderLayout()).also {
    it.add(sp, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightHandler : DocumentListener, ActionListener {
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
  field.background = Color.WHITE
  val highlighter = textArea.highlighter
  highlighter.removeAllHighlights()
  val doc = textArea.document
  val text = doc.getText(0, doc.length)
  getRegex()?.also { pattern ->
    pattern.findAll(text).map { it.range }.filterNot { it.isEmpty() }.forEach {
      highlighter.addHighlight(it.first(), it.last() + 1, highlightPainter)
    }
  }
  val label = layerUI.hint
  val array = highlighter.highlights
  val hits = array.size
  if (hits == 0) {
    current = -1
    label.isOpaque = true
  } else {
    current = (current + hits) % hits
    label.isOpaque = false
    val hh = highlighter.highlights[current]
    highlighter.removeHighlight(hh)
    runCatching {
      highlighter.addHighlight(hh.startOffset, hh.endOffset, currentPainter)
      scrollToCenter(textArea, hh.startOffset)
    }.onFailure {
      it.printStackTrace()
      UIManager.getLookAndFeel().provideErrorFeedback(field)
    }
  }
  label.text = "%02d / %02d%n".format(current + 1, hits)
  field.repaint()
}

private fun getRegex(): Regex? {
  val text = field.text
  if (text == null || text.isEmpty()) {
    return null
  }
  val cw = if (checkWord.isSelected) "\\b" else ""
  val pattern = "%s%s%s".format(cw, text, cw)
  val op = if (checkCase.isSelected) {
    emptySet<RegexOption>()
  } else {
    setOf(RegexOption.IGNORE_CASE)
  }
  return runCatching {
    pattern.toRegex(op)
  }.onFailure {
    field.background = WARNING_COLOR
  }.getOrNull()
}

@Throws(BadLocationException::class)
private fun scrollToCenter(
  tc: JTextComponent,
  pos: Int,
) {
  val rect = tc.modelToView(pos)
  // Java 9: val rect = tc.modelToView2D(pos).getBounds()
  val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, tc)
  if (rect != null && c is JViewport) {
    rect.x = rect.x - c.width / 2
    rect.width = c.width
    rect.height = c.height / 2
    tc.scrollRectToVisible(rect)
  }
}

private class PlaceholderLayerUI<V : JTextComponent> : LayerUI<V>() {
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
      g2.paint = hint.foreground
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
