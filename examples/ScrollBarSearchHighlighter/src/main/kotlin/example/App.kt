package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
import java.awt.geom.AffineTransform
import javax.swing.*
import javax.swing.plaf.metal.MetalScrollBarUI
import javax.swing.plaf.synth.SynthScrollBarUI
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.JTextComponent

private val HIGHLIGHT = DefaultHighlightPainter(Color.YELLOW)
private const val INIT_TXT = """
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
"""

fun makeUI(): Component {
  val textArea = JTextArea()
  textArea.isEditable = false
  textArea.text = INIT_TXT.repeat(3)

  val scroll = JScrollPane(textArea)
  val scrollbar = object : JScrollBar(VERTICAL) {
    override fun updateUI() {
      super.updateUI()
      if (ui is WindowsScrollBarUI) {
        setUI(WindowsHighlightScrollBarUI())
      } else if (ui !is SynthScrollBarUI) {
        setUI(MetalHighlightScrollBarUI())
      }
      unitIncrement = 10
    }
  }
  scroll.verticalScrollBar = scrollbar

  val label = JLabel(HighlightIcon(textArea, scroll.verticalScrollBar))
  scroll.setRowHeaderView(label)

  val check = JCheckBox("LineWrap")
  check.addActionListener { e ->
    textArea.lineWrap = (e.source as? JCheckBox)?.isSelected == true
  }

  val highlight1 = JButton("Swing")
  highlight1.addActionListener { setHighlight(textArea, "Swing") }

  val highlight2 = JButton("swing")
  highlight2.addActionListener { setHighlight(textArea, "swing") }

  val clear = JButton("clear")
  clear.addActionListener {
    textArea.highlighter.removeAllHighlights()
    scroll.repaint()
  }

  val box = Box.createHorizontalBox()
  box.add(check)
  box.add(Box.createHorizontalGlue())
  box.add(JLabel("highlight: "))
  box.add(highlight1)
  box.add(Box.createHorizontalStrut(2))
  box.add(highlight2)
  box.add(Box.createHorizontalStrut(2))
  box.add(clear)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun setHighlight(
  jtc: JTextComponent,
  pattern: String,
) {
  val highlighter = jtc.highlighter
  highlighter.removeAllHighlights()
  val doc = jtc.document
  runCatching {
    val text = doc.getText(0, doc.length)
    pattern
      .toRegex()
      .findAll(
        text,
      ).map { it.range }
      .filterNot { it.isEmpty() }
      .forEach {
        highlighter.addHighlight(it.first(), it.last() + 1, HIGHLIGHT)
      }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(jtc)
  }
  jtc.rootPane.repaint()
}

private class HighlightIcon(
  private val textArea: JTextComponent,
  private val scrollbar: JScrollBar,
) : Icon {
  private val thumbRect = Rectangle()

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val top = scrollbar.insets.top
    val range = scrollbar.model
    val sy = range.extent / (range.maximum - range.minimum).toDouble()
    val at = AffineTransform.getScaleInstance(1.0, sy)
    val highlighter = textArea.highlighter

    // paint Highlight
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.RED

    runCatching {
      for (hh in highlighter.highlights) {
        val r = textArea.modelToView(hh.startOffset)
        val s = at.createTransformedShape(r).bounds
        val h = 2 // Math.max(2, s.height - 2)
        g2.fillRect(0, top + s.y, iconWidth, h)
      }
    }

    // paint Thumb
    if (scrollbar.isVisible) {
      thumbRect.height = range.extent
      thumbRect.y = range.value // viewport.getViewPosition().y
      g2.color = THUMB_COLOR
      val s = at.createTransformedShape(thumbRect).bounds
      g2.fillRect(0, top + s.y, iconWidth, s.height)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 4

  override fun getIconHeight(): Int {
    val c = SwingUtilities.getAncestorOfClass(JViewport::class.java, textArea)
    return (c as? JViewport)?.height ?: 0
  }

  companion object {
    private val THUMB_COLOR = Color(0, 0, 255, 50)
  }
}

private class WindowsHighlightScrollBarUI : WindowsScrollBarUI() {
  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    trackBounds: Rectangle,
  ) {
    super.paintTrack(g, c, trackBounds)
    val s = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c)
    val v = (s as? JScrollPane)?.viewport?.view
    if (v is JTextComponent) {
      val textArea = v
      val rect = textArea.bounds
      val sy = trackBounds.getHeight() / rect.getHeight()
      val at = AffineTransform.getScaleInstance(1.0, sy)
      val highlighter = textArea.highlighter
      g.color = Color.YELLOW
      runCatching {
        for (hh in highlighter.highlights) {
          val r = textArea.modelToView(hh.startOffset)
          val by = at.createTransformedShape(r).bounds.y
          val h = 2
          g.fillRect(trackBounds.x, trackBounds.y + by, trackBounds.width, h)
        }
      }
    }
  }
}

private class MetalHighlightScrollBarUI : MetalScrollBarUI() {
  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    trackBounds: Rectangle,
  ) {
    super.paintTrack(g, c, trackBounds)
    val s = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c)
    val v = (s as? JScrollPane)?.viewport?.view
    if (v is JTextComponent) {
      val textArea = v
      val rect = textArea.bounds
      val sy = trackBounds.getHeight() / rect.getHeight()
      val at = AffineTransform.getScaleInstance(1.0, sy)
      val highlighter = textArea.highlighter
      g.color = Color.YELLOW
      runCatching {
        for (hh in highlighter.highlights) {
          val r = textArea.modelToView(hh.startOffset)
          val by = at.createTransformedShape(r).bounds.y
          val h = 2
          g.fillRect(trackBounds.x, trackBounds.y + by, trackBounds.width, h)
        }
      }
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
