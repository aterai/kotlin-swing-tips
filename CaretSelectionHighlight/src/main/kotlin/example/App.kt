package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.Caret
import javax.swing.text.DefaultCaret
import javax.swing.text.DefaultHighlighter
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.text.Highlighter.HighlightPainter

private fun makeInternalFrame(title: String, p: Point, c: Component): JInternalFrame {
  val f = JInternalFrame(title, true, true, true, true)
  f.add(c)
  f.setSize(200, 100)
  f.location = p
  return f
}

private fun makeTextArea(flag: Boolean): Component {
  val textArea: JTextArea = object : JTextArea() {
    override fun updateUI() {
      caret = null
      super.updateUI()
      if (flag) {
        val oldCaret = caret
        val blinkRate = oldCaret.blinkRate
        // int blinkRate = UIManager.getInt("TextField.caretBlinkRate")
        val caret: Caret = FocusCaret()
        caret.blinkRate = blinkRate
        setCaret(caret)
        caret.isSelectionVisible = true
      }
    }
  }
  textArea.text = "aaa\nbbb bbb\nccc ccc ccc ccc\n"
  textArea.selectAll()
  return JScrollPane(textArea)
}

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.add(makeInternalFrame("DefaultCaret", Point(10, 10), makeTextArea(false)))
  desktop.add(makeInternalFrame("FocusCaret", Point(50, 50), makeTextArea(true)))
  desktop.add(makeInternalFrame("FocusCaret", Point(90, 90), makeTextArea(true)))
  EventQueue.invokeLater {
    for (f in desktop.allFrames) {
      f.isVisible = true
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.preferredSize = Dimension(320, 240)
  }
}

private class FocusCaret : DefaultCaret() {
  override fun focusLost(e: FocusEvent) {
    super.focusLost(e)
    isSelectionVisible = true
  }

  override fun focusGained(e: FocusEvent) {
    super.focusGained(e)
    // https://stackoverflow.com/questions/18237317/how-to-retain-selected-text-in-jtextfield-when-focus-lost
    isSelectionVisible = false // removeHighlight
    isSelectionVisible = true // addHighlight
  }

  override fun getSelectionPainter(): HighlightPainter =
    if (component.hasFocus()) DefaultHighlighter.DefaultPainter else NO_FOCUS

  companion object {
    private val COLOR = Color.GRAY.brighter()
    private val NO_FOCUS = DefaultHighlightPainter(COLOR)
  }
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
