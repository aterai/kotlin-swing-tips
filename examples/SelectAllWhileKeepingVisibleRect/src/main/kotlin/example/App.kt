package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

fun makeUI(): Component {
  val txt = makeSampleText()
  return JPanel(GridLayout(1, 0)).also {
    it.add(makeTitledPanel("Default", JTextArea(txt)))
    it.add(makeTitledPanel("Override selectAll", CustomTextArea(txt)))
    it.add(makeTitledPanel("Caret Top", EmacsTextArea(txt)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeSampleText(): String {
  val s = """
    aaa
    bb bb
    c c c
    ddd
    
    1234567890
  """.trimIndent()
  return s.repeat(10)
}

private fun makeTitledPanel(title: String, editor: JTextArea): Component {
  editor.caretPosition = 0
  editor.border = BorderFactory.createEmptyBorder(0, 2, 0, 0)
  val scroll = JScrollPane(editor)
  scroll.setRowHeaderView(LineNumberView(editor))
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(scroll)
  return p
}

private class CustomTextArea(
  text: String?,
) : JTextArea(text) {
  private val selectAllAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      val r = visibleRect
      actionMap["select-all"].actionPerformed(e)
      EventQueue.invokeLater { scrollRectToVisible(r) }
    }
  }
  private val caretUpAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (selectedText == null) {
        actionMap["caret-up"].actionPerformed(e)
      } else {
        caret.moveDot(caret.mark)
      }
    }
  }
  private val caretForwardAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (selectedText == null) {
        actionMap["caret-forward"].actionPerformed(e)
      } else {
        caret.moveDot(caret.mark)
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater { this.initActionMap() }
  }

  private fun initActionMap() {
    val im = getInputMap(WHEN_FOCUSED)
    val am = actionMap
    val selectAllKey = "select-all2"
    im.put(KeyStroke.getKeyStroke("ctrl A"), selectAllKey)
    am.put(selectAllKey, selectAllAct)
    val caretUpKey = "caret-up"
    im.put(KeyStroke.getKeyStroke("UP"), caretUpKey + "2")
    am.put(caretUpKey + "2", caretUpAct)
    val caretForwardKey = "caret-forward"
    im.put(KeyStroke.getKeyStroke("LEFT"), caretForwardKey + "2")
    am.put(caretForwardKey + "2", caretForwardAct)
  }
}

private class EmacsTextArea(
  text: String?,
) : JTextArea(text) {
  private val selectAllAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      selectAll()
    }
  }
  private val caretDownAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (selectedText == null) {
        actionMap["caret-down"].actionPerformed(e)
      } else {
        caret.moveDot(caret.mark)
      }
    }
  }
  private val caretBackwardAct = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      if (selectedText == null) {
        actionMap["caret-backward"].actionPerformed(e)
      } else {
        caret.moveDot(caret.mark)
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater { this.initActionMap() }
  }

  private fun initActionMap() {
    val im = getInputMap(WHEN_FOCUSED)
    val am = actionMap
    val selectAllKey = "select-all2"
    im.put(KeyStroke.getKeyStroke("ctrl A"), selectAllKey)
    am.put(selectAllKey, selectAllAct)
    val caretDownKey = "caret-down"
    im.put(KeyStroke.getKeyStroke("DOWN"), caretDownKey + "2")
    am.put(caretDownKey + "2", caretDownAct)
    val caretBackwardKey = "caret-backward"
    im.put(KeyStroke.getKeyStroke("RIGHT"), caretBackwardKey + "2")
    am.put(caretBackwardKey + "2", caretBackwardAct)
  }

  override fun selectAll() {
    val r = visibleRect
    val doc = document
    if (doc != null) {
      caretPosition = doc.length
      moveCaretPosition(0)
    }
    EventQueue.invokeLater { scrollRectToVisible(r) }
  }
}

private class LineNumberView(
  private val textArea: JTextArea,
) : JComponent() {
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
    val dl = object : DocumentListener {
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
      BorderFactory.createEmptyBorder(i.top, MARGIN, i.bottom, MARGIN - 1),
    )
    isOpaque = true
    background = Color.WHITE
    setFont(font)
  }

  private fun getLineAtPoint(y: Int): Int {
    val root = textArea.document.defaultRootElement
    val pos = textArea.viewToModel(Point(0, y))
    // Java 9: val pos = textArea.viewToModel2D(Point(0, y))
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
