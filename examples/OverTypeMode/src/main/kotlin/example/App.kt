package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.Caret
import javax.swing.text.DefaultCaret

fun createUI(): Component {
  val textArea = OverTypeTextArea()
  textArea.font = Font(Font.MONOSPACED, Font.PLAIN, 12)
  textArea.text =
    """
      Press the INSERT key to toggle the overwrite mode.
      ●▽◇■
      1234567890
    """.trimIndent()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

// https://community.oracle.com/tech/developers/discussion/1385467
// JTextPane edit mode (insert or overwrite)???
private class OverTypeTextArea : JTextArea() {
  private var overTypeMode = true
  private var defaultCaret: Caret? = null
  private var overTypeCaret: Caret? = null

  override fun updateUI() {
    // The UI only replaces a caret that is null or a UIResource, so restore
    // the default caret first to avoid keeping the OverTypeCaret as the default one
    if (caret is OverTypeCaret) {
      caret = defaultCaret
    }
    super.updateUI()
    // Deferred because this method is called from the super constructor
    // before the property initializer of overTypeMode is executed
    EventQueue.invokeLater {
      defaultCaret = caret
      overTypeCaret = OverTypeCaret().also {
        it.blinkRate = caret.blinkRate
      }
      setOverTypeMode(overTypeMode)
    }
  }

  fun isOverTypeMode() = overTypeMode

  // Set the caret to use depending on over-type/insert mode
  private fun setOverTypeMode(overType: Boolean) {
    this.overTypeMode = overType
    val pos = caretPosition
    caret = if (overType) overTypeCaret else defaultCaret
    caretPosition = pos
  }

  // Returns the number of chars of the character at the given position
  // that can be overwritten: 2 for a surrogate pair, 0 for a line feed
  // or the end of the document, otherwise 1.
  fun getOverTypeLength(pos: Int): Int {
    val len = minOf(2, document.length - pos)
    val codePoint = if (len > 0) getText(pos, len).codePointAt(0) else '\n'.code
    return if (codePoint == '\n'.code) 0 else Character.charCount(codePoint)
  }

  override fun replaceSelection(text: String?) {
    // Implement over-type mode by selecting the character at the current
    // caret position (line feeds are not overwritten)
    if (isOverTypeMode() && selectionStart == selectionEnd) {
      val pos = caretPosition
      moveCaretPosition(pos + getOverTypeLength(pos))
    }
    super.replaceSelection(text)
  }

  override fun processKeyEvent(e: KeyEvent) {
    super.processKeyEvent(e)
    // Handle release of Insert key to toggle over-type/insert mode
    if (e.id == KeyEvent.KEY_RELEASED && e.keyCode == KeyEvent.VK_INSERT) {
      // Clear the selection, otherwise its highlight is left behind
      // by the uninstalled caret
      caretPosition = caretPosition
      setOverTypeMode(!isOverTypeMode())
      repaint()
    }
  }

  private inner class OverTypeCaret : DefaultCaret() {
    override fun paint(g: Graphics) {
      if (isVisible) {
        runCatching {
          val c = component
          val r = c.ui.modelToView(c, dot)
          // Java 9:
          // val r = c.ui.modelToView2D(c, dot, Position.Bias.Forward).bounds
          g.color = c.caretColor
          val width = getOverTypeCaretWidth(g.fontMetrics)
          val y = r.y + r.height - 2
          g.drawLine(r.x, y, r.x + width - 2, y)
        }
      }
    }

    private fun getOverTypeCaretWidth(fm: FontMetrics): Int {
      val pos = dot
      val len = if (dot == mark) getOverTypeLength(pos) else 0
      // A patch for full width characters and surrogate pairs
      return if (len > 0) fm.stringWidth(getText(pos, len)) else fm.charWidth('w')
    }

    @Synchronized
    override fun damage(r: Rectangle?) {
      if (r != null) {
        val c = component
        x = r.x
        y = r.y
        // width = c.getFontMetrics(c.getFont()).charWidth('w')
        // A full width character is wider than 'w'
        width = c.getFontMetrics(c.font).charWidth('■')
        height = r.height
        // Repaint only the caret area instead of the whole component
        repaint()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
