package example

import com.sun.java.swing.plaf.windows.WindowsScrollBarUI
import java.awt.*
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

fun createUI(): Component {
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
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val range = scrollbar.model
    val trackHeight = iconHeight
    val viewHeight = range.maximum - range.minimum
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y + scrollbar.insets.top)
    g2.paint = Color.RED
    HighlightMarkPainter.paintMarks(g2, textArea, iconWidth, trackHeight, viewHeight)
    if (scrollbar.isVisible) {
      g2.paint = THUMB_COLOR
      val thumbY = HighlightMarkPainter.scale(range.value, trackHeight, viewHeight)
      val thumbHeight = HighlightMarkPainter.scale(range.extent, trackHeight, viewHeight)
      g2.fillRect(0, thumbY, iconWidth, thumbHeight)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 4

  override fun getIconHeight(): Int {
    val viewport = SwingUtilities.getAncestorOfClass(JViewport::class.java, textArea)
    return (viewport as? JViewport)?.height ?: scrollbar.height
  }

  companion object {
    private val THUMB_COLOR = Color(0, 0, 255, 50)
  }
}

private object HighlightMarkPainter {
  private const val MARK_HEIGHT = 2

  fun scale(
    value: Int,
    trackHeight: Int,
    viewHeight: Int,
  ) = if (viewHeight <= 0) {
    0
  } else {
    (value * trackHeight / viewHeight.toDouble()).toInt()
  }

  fun paintMarks(
    g: Graphics,
    textArea: JTextComponent,
    width: Int,
    trackHeight: Int,
    viewHeight: Int,
  ) {
    runCatching {
      for (h in textArea.highlighter.highlights) {
        // Java 9: val r = textArea.modelToView2D(h.startOffset).bounds
        val r = textArea.modelToView(h.startOffset)
        g.fillRect(0, scale(r.y, trackHeight, viewHeight), width, MARK_HEIGHT)
      }
    }
  }

  fun paintTrackMarks(
    g: Graphics,
    c: JComponent,
    trackBounds: Rectangle,
  ) {
    val scroll = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, c)
    val view = (scroll as? JScrollPane)?.viewport?.view
    if (view is JTextComponent) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(trackBounds.x, trackBounds.y)
      g2.paint = Color.YELLOW
      paintMarks(g2, view, trackBounds.width, trackBounds.height, view.height)
      g2.dispose()
    }
  }
}

private class WindowsHighlightScrollBarUI : WindowsScrollBarUI() {
  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    trackBounds: Rectangle,
  ) {
    super.paintTrack(g, c, trackBounds)
    HighlightMarkPainter.paintTrackMarks(g, c, trackBounds)
  }
}

private class MetalHighlightScrollBarUI : MetalScrollBarUI() {
  override fun paintTrack(
    g: Graphics,
    c: JComponent,
    trackBounds: Rectangle,
  ) {
    super.paintTrack(g, c, trackBounds)
    HighlightMarkPainter.paintTrackMarks(g, c, trackBounds)
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
