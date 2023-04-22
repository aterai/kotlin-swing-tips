package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val textArea = object : JTextArea() {
    // https://stackoverflow.com/questions/32679335/java-jtextarea-allow-scrolling-beyond-end-of-text
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this)
      if (c is JScrollPane && isEditable) {
        val r = c.viewportBorderBounds
        d.height += r.height - rowHeight - insets.bottom
      }
      return d
    }
  }
  textArea.text = "1111111111\n222222222222\n\n\n\n\n\n\n\n\n\n333333333333333"
  textArea.caretPosition = 0
  textArea.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)

  val scroll = JScrollPane(textArea)
  scroll.setRowHeaderView(LineNumberView(textArea))

  val check = JCheckBox("editable", true)
  check.addActionListener { textArea.isEditable = check.isSelected }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

// Advice for editor gutter implementation...
// https://community.oracle.com/thread/1479759
private class LineNumberView(private val textArea: JTextArea) : JComponent() {
  private val fontMetrics: FontMetrics
  private val fontAscent: Int
  private val fontHeight: Int
  private val fontDescent: Int
  private val fontLeading: Int

  init {
    val font = textArea.font
    fontMetrics = getFontMetrics(font)
    fontHeight = fontMetrics.height
    fontAscent = fontMetrics.ascent
    fontDescent = fontMetrics.descent
    fontLeading = fontMetrics.leading

    val documentListener = object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun removeUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun changedUpdate(e: DocumentEvent) {
        // not needed
      }
    }
    textArea.document.addDocumentListener(documentListener)

    val componentListener = object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        revalidate()
        repaint()
      }
    }
    textArea.addComponentListener(componentListener)

    val i = textArea.insets
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
      BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1)
    )
    isOpaque = true
    background = Color.WHITE
    setFont(font)
  }

  private fun getComponentWidth(): Int {
    val lineCount = textArea.lineCount
    val maxDigits = maxOf(3, lineCount.toString().length)
    val i = insets
    return maxDigits * fontMetrics.stringWidth("0") + i.left + i.right
  }

  override fun getPreferredSize() = Dimension(getComponentWidth(), textArea.height)

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
      val x = getComponentWidth() - rmg - fontMetrics.stringWidth(text)
      y += fontAscent
      g.drawString(text, x, y)
      y += fontDescent + fontLeading
    }
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.document.defaultRootElement
    val pos = textArea.viewToModel(Point(0, y))
    return root.getElementIndex(pos)
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
