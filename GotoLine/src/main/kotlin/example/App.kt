// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@

package example

import java.awt.*
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.BadLocationException
import javax.swing.text.Document
import javax.swing.text.Element

class MainPanel : JPanel(BorderLayout()) {

  init {
    val spinner = JSpinner(SpinnerNumberModel(100, 1, 2000, 1))
    // val textArea = JTextArea(Collections.nCopies(2000, "aaaaaaaaaaaaa").joinToString("\n"))
    val textArea = JTextArea("aaaaaaaaaaaaa\n".repeat(2000))
    textArea.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 0))
    val scroll = JScrollPane(textArea)
    scroll.setRowHeaderView(LineNumberView(textArea))

    val button = JButton("Goto Line")
    button.addActionListener {
      val doc = textArea.getDocument()
      val root = doc.getDefaultRootElement()
      val i = Math.max(1, Math.min(root.getElementCount(), spinner.getValue() as Int))
      try {
        val elem = root.getElement(i - 1)
        val rect = textArea.modelToView(elem.getStartOffset())
        val vr = scroll.getViewport().getViewRect()
        rect.setSize(10, vr.height)
        textArea.scrollRectToVisible(rect)
        textArea.setCaretPosition(elem.getStartOffset())
        // textArea.requestFocus();
      } catch (ex: BadLocationException) {
        UIManager.getLookAndFeel().provideErrorFeedback(textArea)
      }
    }
    // frame.getRootPane().setDefaultButton(button);
    EventQueue.invokeLater { getRootPane().setDefaultButton(button) }

    val p = JPanel(BorderLayout())
    p.add(spinner)
    p.add(button, BorderLayout.EAST)
    add(p, BorderLayout.NORTH)
    add(scroll)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class LineNumberView(private val textArea: JTextArea) : JComponent() {
  private val fontMetrics: FontMetrics
  private val fontAscent: Int
  private val fontHeight: Int
  private val fontDescent: Int
  private val fontLeading: Int

  private// Document doc = textArea.getDocument();
  // Element root = doc.getDefaultRootElement();
  // int lineCount = root.getElementIndex(doc.getLength());
  // return 48;
  val componentWidth: Int
    get() {
      val lineCount = textArea.getLineCount()
      val maxDigits = Math.max(3, lineCount.toString().length)
      val i = getBorder().getBorderInsets(this)
      return maxDigits * fontMetrics.stringWidth("0") + i.left + i.right
    }

  init {
    val font = textArea.getFont()
    fontMetrics = getFontMetrics(font)
    fontHeight = fontMetrics.getHeight()
    fontAscent = fontMetrics.getAscent()
    fontDescent = fontMetrics.getDescent()
    fontLeading = fontMetrics.getLeading()
    // topInset = textArea.getInsets().top;

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
      override fun componentResized(e: ComponentEvent?) {
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

  override fun getPreferredSize() = Dimension(componentWidth, textArea.getHeight())

  protected override fun paintComponent(g: Graphics) {
    g.setColor(getBackground())
    val clip = g.getClipBounds()
    g.fillRect(clip.x, clip.y, clip.width, clip.height)

    g.setColor(getForeground())
    val base = clip.y
    val start = getLineAtPoint(base)
    val end = getLineAtPoint(base + clip.height)
    var y = start * fontHeight
    val rmg = getBorder().getBorderInsets(this).right
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
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
