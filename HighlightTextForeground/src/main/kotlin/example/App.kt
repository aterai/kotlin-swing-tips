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
import javax.swing.text.StyleConstants
import javax.swing.text.StyleContext

private const val TEXT = """
Trail: Creating a GUI with JFC/Swing
https://docs.oracle.com/javase/tutorial/uiswing/learn/index.htmllis
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
private val WARNING_COLOR = Color(0xFF_C8_C8)
private val CURRENT_COLOR = Color(0xAA_00_64_00.toInt(), true)
private val HIGHLIGHT_COLOR = Color(0x64_DD_DD_00, true)
private val currentPainter = DefaultHighlightPainter(CURRENT_COLOR)
private val highlightPainter = DefaultHighlightPainter(HIGHLIGHT_COLOR)
private val textPane = JTextPane()
private val field = JTextField("Swing")
private val checkCase = JCheckBox("Match case")
private val checkWord = JCheckBox("Match whole word only")
private val layerUI = PlaceholderLayerUI<JTextComponent>()
private val handler = HighlightHandler()

fun makeUI(): Component {
  textPane.isEditable = false
  textPane.text = TEXT

  val prevButton = JButton("⋀")
  prevButton.actionCommand = "prev"

  val nextButton = JButton("⋁")
  nextButton.actionCommand = "next"

  val doc = textPane.styledDocument
  val def = doc.getStyle(StyleContext.DEFAULT_STYLE)
  val htf = doc.addStyle("highlight-text-foreground", def)
  StyleConstants.setForeground(htf, Color(0xFF_DD_FF))
  field.document.addDocumentListener(handler)
  listOf(prevButton, nextButton, checkCase, checkWord).forEach {
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
  EventQueue.invokeLater { changeHighlight(0) }

  return JPanel(BorderLayout()).also {
    it.add(sp, BorderLayout.NORTH)
    it.add(JScrollPane(textPane))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getRegex(): Regex? {
  val text = field.text
  if (text.isEmpty()) {
    return null
  }
  val cw = if (checkWord.isSelected) "\\b" else ""
  val pattern = "%s%s%s".format(cw, text, cw)
  val op = if (checkCase.isSelected) emptySet() else setOf(RegexOption.IGNORE_CASE)
  return runCatching {
    pattern.toRegex(op)
  }.onFailure {
    field.background = WARNING_COLOR
  }.getOrNull()
}

fun changeHighlight(index: Int): Int {
  field.background = Color.WHITE
  val doc = textPane.styledDocument
  val s = doc.getStyle("highlight-text-foreground")
  val def = doc.getStyle(StyleContext.DEFAULT_STYLE)

  // clear the previous highlight:
  val highlighter = textPane.highlighter
  for (h in highlighter.highlights) {
    doc.setCharacterAttributes(h.startOffset, h.endOffset - h.startOffset, def, true)
  }
  highlighter.removeAllHighlights()
  // doc.setCharacterAttributes(0, doc.getLength(), def, true)
  // match highlighting:
  val text = doc.getText(0, doc.length)
  getRegex()?.also { pattern ->
    pattern.findAll(text).map { it.range }.filterNot { it.isEmpty() }.forEach {
      highlighter.addHighlight(it.first(), it.last() + 1, highlightPainter)
    }
  }
  val label = layerUI.hint
  val array = highlighter.highlights
  val hits = array.size
  var idx = index
  if (hits == 0) {
    idx = -1
    label.isOpaque = true
  } else {
    idx = (idx + hits) % hits
    label.isOpaque = false
    val hh = highlighter.highlights[idx]
    highlighter.removeHighlight(hh)
    runCatching {
      highlighter.addHighlight(hh.startOffset, hh.endOffset, currentPainter)
      doc.setCharacterAttributes(hh.startOffset, hh.endOffset - hh.startOffset, s, true)
      scrollToCenter(textPane, hh.startOffset)
    }
  }
  label.text = "%02d / %02d%n".format(idx + 1, hits)
  field.repaint()
  return idx
}

private class HighlightHandler : DocumentListener, ActionListener {
  private var current = 0

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }

  override fun insertUpdate(e: DocumentEvent) {
    current = changeHighlight(current)
  }

  override fun removeUpdate(e: DocumentEvent) {
    current = changeHighlight(current)
  }

  override fun actionPerformed(e: ActionEvent) {
    val o = e.source
    if (o is AbstractButton) {
      val cmd = o.actionCommand
      if ("prev" == cmd) {
        current--
      } else if ("next" == cmd) {
        current++
      }
    }
    current = changeHighlight(current)
  }
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
