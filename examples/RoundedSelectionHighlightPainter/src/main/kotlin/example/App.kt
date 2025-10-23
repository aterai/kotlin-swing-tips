package example

import java.awt.*
import java.awt.geom.RoundRectangle2D
import java.util.regex.Pattern
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter
import javax.swing.text.JTextComponent
import javax.swing.text.Position
import javax.swing.text.View
import kotlin.math.max

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
private val field = JTextField("Swing")
private val textArea = object : JTextArea(TEXT) {
  override fun updateUI() {
    super.updateUI()
    val caret = object : DefaultCaret() {
      override fun getSelectionPainter() = RoundedSelectionHighlightPainter()
    }
    caret.blinkRate = UIManager.getInt("TextArea.caretBlinkRate")
    setCaret(caret)
  }
}

fun makeUI(): Component {
  val title = "DefaultHighlighter#setDrawsLayeredHighlights"
  val check = JCheckBox(title, true)
  check.addActionListener { e ->
    val dh = textArea.highlighter as? DefaultHighlighter
    dh?.drawsLayeredHighlights = (e.source as? JCheckBox)?.isSelected == true
    DocumentUtils.updateHighlight(textArea, field)
    textArea.select(textArea.selectionStart, textArea.selectionEnd)
  }

  field.document.addDocumentListener(HighlightDocumentListener())
  DocumentUtils.updateHighlight(textArea, field)

  val sp = JPanel(BorderLayout(5, 5))
  sp.add(JLabel("regex pattern:"), BorderLayout.WEST)
  sp.add(field)
  sp.add(Box.createVerticalStrut(2), BorderLayout.SOUTH)
  sp.border = BorderFactory.createTitledBorder("Search")

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(Box.createHorizontalStrut(2))

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(sp, BorderLayout.NORTH)
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightDocumentListener : DocumentListener {
  override fun insertUpdate(e: DocumentEvent) {
    DocumentUtils.updateHighlight(textArea, field)
  }

  override fun removeUpdate(e: DocumentEvent) {
    DocumentUtils.updateHighlight(textArea, field)
  }

  override fun changedUpdate(e: DocumentEvent) {
    // not needed
  }
}

private object DocumentUtils {
  private val WARNING_COLOR = Color(0xFF_C8_C8)

  fun updateHighlight(editor: JTextComponent, field: JTextField) {
    field.background = Color.WHITE
    val pattern = field.text.trim { it <= ' ' }
    val highlighter = editor.highlighter
    highlighter.removeAllHighlights()
    if (pattern.isNotEmpty()) {
      val doc = editor.document
      runCatching {
        val text = doc.getText(0, doc.length)
        val matcher = Pattern.compile(pattern).matcher(text)
        val highlightPainter: HighlightPainter = RoundedHighlightPainter()
        var pos = 0
        while (matcher.find(pos) && matcher.group().isNotEmpty()) {
          val start = matcher.start()
          val end = matcher.end()
          highlighter.addHighlight(start, end, highlightPainter)
          pos = end
        }
      }.onFailure {
        field.background = WARNING_COLOR
      }
      field.repaint()
      editor.repaint()
    }
  }
}

private class RoundedHighlightPainter : DefaultHighlightPainter(Color(0x0, true)) {
  override fun paint(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
  ) {
    runCatching {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val mapper = c.ui
      val p0 = mapper.modelToView(c, offs0)
      val p1 = mapper.modelToView(c, offs1)
      if (p0.y == p1.y) {
        val s = makeRoundRectangle(p0.union(p1))
        g2.color = Color.PINK
        g2.fill(s)
        g2.paint = Color.RED
        g2.draw(s)
      } else {
        val alloc = bounds.bounds
        val p0ToMarginWidth = alloc.x + alloc.width - p0.x
        g2.color = Color.PINK
        g2.fillRoundRect(p0.x, p0.y, p0ToMarginWidth, p0.height, 5, 5)
        g2.fillRect(p0.x + 5, p0.y, p0ToMarginWidth - 5, p0.height)
        val maxY = p0.y + p0.height
        if (maxY != p1.y) {
          g2.fillRect(alloc.x, maxY, alloc.width, p1.y - maxY)
        }
        g2.fillRect(alloc.x, p1.y, p1.x - alloc.x - 5, p1.height)
        g2.fillRoundRect(alloc.x, p1.y, p1.x - alloc.x, p1.height, 5, 5)
      }
      g2.dispose()
    }
  }

  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View,
  ): Shape {
    val s = super.paintLayer(g, offs0, offs1, bounds, c, view)
    val g2 = g.create()
    if (s is Rectangle && g2 is Graphics2D) {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.paint = Color.ORANGE
      val r = makeRoundRectangle(s.bounds)
      g2.fill(r)
      g2.paint = Color.RED
      g2.draw(r)
    }
    g2.dispose()
    return s
  }

  private fun makeRoundRectangle(r: Rectangle): RoundRectangle2D = RoundRectangle2D
    .Float(
      r.x.toFloat(),
      r.y.toFloat(),
      r.width - 1f,
      r.height - 1f,
      5f,
      5f,
    )
}

private class RoundedSelectionHighlightPainter : DefaultHighlightPainter(null) {
  override fun paint(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
  ) {
    runCatching {
      val mapper = c.ui
      val p0 = mapper.modelToView(c, offs0)
      val p1 = mapper.modelToView(c, offs1)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.color = c.selectionColor
      if (p0.y == p1.y) {
        val r = p0.union(p1)
        g2.fillRoundRect(r.x, r.y, r.width, r.height, 5, 5)
      } else {
        val alloc = bounds.bounds
        val p0ToMarginWidth = alloc.x + alloc.width - p0.x
        g2.fillRoundRect(p0.x, p0.y, p0ToMarginWidth, p0.height, 5, 5)
        g2.fillRect(p0.x + 5, p0.y, p0ToMarginWidth - 5, p0.height)
        val maxY = p0.y + p0.height
        if (maxY != p1.y) {
          g2.fillRect(alloc.x, maxY, alloc.width, p1.y - maxY)
        }
        g2.fillRect(alloc.x, p1.y, p1.x - alloc.x - 5, p1.height)
        g2.fillRoundRect(alloc.x, p1.y, p1.x - alloc.x, p1.height, 5, 5)
      }
      g2.dispose()
    }
  }

  override fun paintLayer(
    g: Graphics,
    offs0: Int,
    offs1: Int,
    bounds: Shape,
    c: JTextComponent,
    view: View,
  ): Shape? {
    val g2 = g.create() as? Graphics2D ?: return null
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val color = color
    if (color == null) {
      g2.color = c.selectionColor
    } else {
      g2.color = color
    }
    val r = if (offs0 == view.startOffset && offs1 == view.endOffset) {
      bounds as? Rectangle ?: bounds.bounds
    } else {
      runCatching {
        val shape = view.modelToView(
          offs0,
          Position.Bias.Forward,
          offs1,
          Position.Bias.Backward,
          bounds,
        )
        shape as? Rectangle ?: shape.bounds
      }.getOrNull()
    }
    if (r != null) {
      r.width = max(r.width.toDouble(), 1.0).toInt()
      g2.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, 5, 5)
      g2.color = c.selectionColor.darker()
      g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 5, 5)
    }
    g2.dispose()
    return r
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
