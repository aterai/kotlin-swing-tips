package example

import java.awt.*
import java.lang.Character.UnicodeBlock
import javax.swing.*

fun makeUI(): Component {
  val label = JTextField()
  label.isEditable = false
  label.font = label.font.deriveFont(32f)
  val labelUnicodeBlock = JTextField()
  label.isEditable = false

  val textArea = JTextArea("??????‚Ä‚·‚ÆƒeƒXƒg‚s‚d‚r‚stestŽŽŒ±A??„¸¨")
  textArea.addCaretListener { e ->
    runCatching {
      val loc = e.dot.coerceAtMost(e.mark)
      val doc = textArea.document
      var txt = doc.getText(loc, 1)
      var code = txt.codePointAt(0)
      if (Character.isHighSurrogate(code.toChar())) {
        txt = doc.getText(loc, 2)
        code = txt.codePointAt(0)
      }
      label.text = "%s: U+%04X".format(txt, code)
      labelUnicodeBlock.text = UnicodeBlock.of(code)?.toString() ?: ""
    }
  }

  val box = Box.createVerticalBox()
  box.add(label)
  box.add(labelUnicodeBlock)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
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
