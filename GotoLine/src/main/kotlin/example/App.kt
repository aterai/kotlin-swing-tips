package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val model = SpinnerNumberModel(100, 1, 2000, 1)
  val textArea = JTextArea("1111111111111111\n".repeat(2000))
  textArea.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
  val scroll = JScrollPane(textArea)
  scroll.setRowHeaderView(LineNumberView(textArea))

  val button = JButton("Goto Line")
  button.addActionListener {
    val doc = textArea.document
    val root = doc.defaultRootElement
    val i = model.number.toInt()
    runCatching {
      val elem = root.getElement(i - 1)
      val rect = textArea.modelToView(elem.startOffset)
      val vr = scroll.viewport.viewRect
      rect.setSize(10, vr.height)
      textArea.scrollRectToVisible(rect)
      textArea.caretPosition = elem.startOffset
      // textArea.requestFocus()
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(textArea)
    }
  }
  EventQueue.invokeLater { button.rootPane.defaultButton = button }

  val p = JPanel(BorderLayout())
  p.add(JSpinner(model))
  p.add(button, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class LineNumberView(private val textArea: JTextArea) : JComponent() {
  private val fontMetrics: FontMetrics
  private val fontAscent: Int
  private val fontHeight: Int
  private val fontDescent: Int
  private val fontLeading: Int
  private val componentWidth: Int
    get() {
      val lineCount = textArea.lineCount
      val maxDigits = maxOf(3, lineCount.toString().length)
      val i = insets
      return maxDigits * fontMetrics.stringWidth("0") + i.left + i.right
    }

  init {
    val font = textArea.font
    fontMetrics = getFontMetrics(font)
    fontHeight = fontMetrics.height
    fontAscent = fontMetrics.ascent
    fontDescent = fontMetrics.descent
    fontLeading = fontMetrics.leading
    // topInset = textArea.getInsets().top;

    val dl = object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun removeUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun changedUpdate(e: DocumentEvent) {
        /* not needed */
      }
    }
    textArea.document.addDocumentListener(dl)
    val cmpListener = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        revalidate()
        repaint()
      }
    }
    textArea.addComponentListener(cmpListener)
    val i = textArea.insets
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
      BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1)
    )
    isOpaque = true
    background = Color.WHITE
    setFont(font)
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.document.defaultRootElement
    val pos = textArea.viewToModel(Point(0, y))
    return root.getElementIndex(pos)
  }

  override fun getPreferredSize() = Dimension(componentWidth, textArea.height)

  override fun paintComponent(g: Graphics) {
    g.color = background
    val clip = g.clipBounds
    g.fillRect(clip.x, clip.y, clip.width, clip.height)

    g.color = foreground
    val base = clip.y
    val start = getLineAtPoint(base)
    val end = getLineAtPoint(base + clip.height)
    var y = start * fontHeight
    val rmg = insets.right
    for (i in start..end) {
      val text = (i + 1).toString()
      val x = componentWidth - rmg - fontMetrics.stringWidth(text)
      y += fontAscent
      g.drawString(text, x, y)
      y += fontDescent + fontLeading
    }
  }

  companion object {
    private const val MARGIN = 5
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
