package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.text.DefaultCaret

fun makeUI(): Component {
  val txt = "The quick brown fox jumps over the lazy dog.\n".repeat(3)
  val txt1 = "Default:"
  val textArea1 = JTextArea("$txt1\n$txt")
  val txt2 = "Disable middle mouseClicked paste:"
  val textArea2: JTextArea = object : JTextArea("$txt2\n$txt") {
    override fun updateUI() {
      setCaret(null)
      super.updateUI()
      val oldCaret = caret
      val blinkRate = oldCaret.blinkRate
      val caret1 = DisableMiddleClickPasteCaret()
      caret1.blinkRate = blinkRate
      setCaret(caret1)
    }
  }
  return JPanel().also {
    it.add(JScrollPane(textArea1))
    it.add(JScrollPane(textArea2))
    it.preferredSize = Dimension(320, 240)
  }
}

private class DisableMiddleClickPasteCaret : DefaultCaret() {
  override fun mouseClicked(e: MouseEvent) {
    if (SwingUtilities.isMiddleMouseButton(e)) {
      e.consume()
    }
    super.mouseClicked(e)
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
