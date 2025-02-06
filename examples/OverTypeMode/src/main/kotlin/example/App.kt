package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.Caret
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val textArea = OvertypeTextArea()
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

private class OvertypeTextArea : JTextArea() {
  private var overtypeMode = true
  private var defaultCaret: Caret? = null
  private var overtypeCaret: Caret? = null

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      defaultCaret = caret
      overtypeCaret = OvertypeCaret().also {
        it.blinkRate = caret.blinkRate
      }
      setOvertypeMode(overtypeMode)
    }
  }

  fun isOvertypeMode() = overtypeMode

  private fun setOvertypeMode(overtypeMode: Boolean) {
    this.overtypeMode = overtypeMode
    val pos = caretPosition
    caret = if (isOvertypeMode()) {
      overtypeCaret
    } else {
      defaultCaret
    }
    caretPosition = pos
  }

  override fun replaceSelection(text: String) {
    if (isOvertypeMode()) {
      val pos = caretPosition
      if (selectionStart == selectionEnd && pos < document.length) {
        moveCaretPosition(pos + 1)
      }
    }
    super.replaceSelection(text)
  }

  override fun processKeyEvent(e: KeyEvent) {
    super.processKeyEvent(e)
    // Handle release of Insert key to toggle overtype/insert mode
    if (e.id == KeyEvent.KEY_RELEASED && e.keyCode == KeyEvent.VK_INSERT) {
      moveCaretPosition(caretPosition) // add
      setOvertypeMode(!isOvertypeMode())
      repaint() // add
    }
  }

  private inner class OvertypeCaret : DefaultCaret() {
    override fun paint(g: Graphics) {
      if (isVisible) {
        runCatching {
          val component = component
          val mapper = component.ui
          val r = mapper.modelToView(component, dot)
          g.color = component.caretColor
          var width = g.fontMetrics.charWidth('w')
          // A patch for full width characters >>>>
          if (isOvertypeMode()) {
            val pos = caretPosition
            if (pos < document.length) {
              width = if (selectionStart == selectionEnd) {
                val str = getText(pos, 1)
                g.fontMetrics.stringWidth(str)
              } else {
                0
              }
            }
          } // <<<<
          val y = r.y + r.height - 2
          g.drawLine(r.x, y, r.x + width - 2, y)
        }
      }
    }

    @Synchronized
    override fun damage(r: Rectangle?) {
      if (r != null) {
        val c = component
        x = r.x
        y = r.y
        // width = c.getFontMetrics(c.getFont()).charWidth('w')
        // width = c.getFontMetrics(c.getFont()).charWidth('\u3042')
        width = c.getFontMetrics(c.font).charWidth('■')
        height = r.height
        c.repaint()
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
