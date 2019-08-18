package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class MainPanel : JPanel(BorderLayout()) {
  init {
    val textArea = object : JTextArea() {
      // https://stackoverflow.com/questions/32679335/java-jtextarea-allow-scrolling-beyond-end-of-text
      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        val c = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this)
        if (c is JScrollPane && isEditable()) {
          val r = c.getViewportBorderBounds()
          d.height += r.height - getRowHeight() - getInsets().bottom
        }
        return d
      }
    }
    textArea.setText("aaaaaaaaa\nbbbbbbbbbbbbbb\n\n\n\n\n\n\n\n\n\nccccccccccccc")
    textArea.setCaretPosition(0)
    textArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0))

    val scroll = JScrollPane(textArea)
    scroll.setRowHeaderView(LineNumberView(textArea))

    val check = JCheckBox("editable", true)
    check.addActionListener { textArea.setEditable(check.isSelected()) }

    add(scroll)
    add(check, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

// Advice for editor gutter implementation...
// https://community.oracle.com/thread/1479759
class LineNumberView(private val textArea: JTextArea) : JComponent() {
  private val fontMetrics: FontMetrics
  private val fontAscent: Int
  private val fontHeight: Int
  private val fontDescent: Int
  private val fontLeading: Int

  protected fun getComponentWidth(): Int {
    val lineCount = textArea.getLineCount()
    val maxDigits = maxOf(3, lineCount.toString().length)
    val i = getInsets()
    return maxDigits * fontMetrics.stringWidth("0") + i.left + i.right
  }

  override fun getPreferredSize() = Dimension(getComponentWidth(), textArea.getHeight())

  init {
    val font = textArea.getFont()
    fontMetrics = getFontMetrics(font)
    fontHeight = fontMetrics.getHeight()
    fontAscent = fontMetrics.getAscent()
    fontDescent = fontMetrics.getDescent()
    fontLeading = fontMetrics.getLeading()

    textArea.getDocument().addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun removeUpdate(e: DocumentEvent) {
        repaint()
      }

      override fun changedUpdate(e: DocumentEvent) {
        /* not needed */
      }
    })
    textArea.addComponentListener(object : ComponentAdapter() {
      override fun componentResized(e: ComponentEvent) {
        revalidate()
        repaint()
      }
    })
    val i = textArea.getInsets()
    setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 0, 0, 1, Color.GRAY),
        BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1)))
    setOpaque(true)
    setBackground(Color.WHITE)
    setFont(font)
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.getDocument().getDefaultRootElement()
    val pos = textArea.viewToModel(Point(0, y))
    return root.getElementIndex(pos)
  }

  protected override fun paintComponent(g: Graphics) {
    g.setColor(getBackground())
    val clip = g.getClipBounds()
    g.fillRect(clip.x, clip.y, clip.width, clip.height)

    g.setColor(getForeground())
    val base = clip.y
    val start = getLineAtPoint(base)
    val end = getLineAtPoint(base + clip.height)
    var y = start * fontHeight
    val rmg = getInsets().right
    for (i in start..end) {
      val text = (i + 1).toString()
      val x = getComponentWidth() - rmg - fontMetrics.stringWidth(text)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
