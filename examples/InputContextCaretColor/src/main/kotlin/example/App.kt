package example

import java.awt.*
import javax.swing.*
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val textArea = object : JTextArea() {
    override fun updateUI() {
      super.updateUI()
      val caret = InputContextCaret()
      caret.blinkRate = UIManager.getInt("TextArea.caretBlinkRate")
      setCaret(caret)
    }
  }
  textArea.text = "When IME is enabled, change the color of the caret.\n\n12345"
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.preferredSize = Dimension(320, 240)
  }
}

private class InputContextCaret : DefaultCaret() {
  private val caretFg = UIManager.getColor("TextArea.caretForeground")

  override fun paint(g: Graphics?) {
    if (isVisible) {
      val c = getComponent()
      val b = c.getInputContext().isCompositionEnabled
      c.setCaretColor(if (b) Color.RED else caretFg)
    }
    super.paint(g)
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
