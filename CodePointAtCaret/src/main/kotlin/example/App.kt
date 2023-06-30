package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val u1F60x = "😀😁😂😃😄😅😆😇😈😉😊😋😌😍😎😏"
  val u1F61x = "😐😑😒😓😔😕😖😗😘😙😚😛😜😝😞😟"
  val u1F62x = "😠😡😢😣😤😥😦😧😨😩😪😫😬😭😮😯"
  val u1F63x = "😰😱😲😳😴😵😶😷😸😹😺😻😼😽😾😿"
  val u1F64x = "🙀🙁🙂🙃🙄🙅🙆🙇🙈🙉🙊🙋🙌🙍🙎🙏"

  val label = JTextField()
  label.isEditable = false
  label.font = label.font.deriveFont(32f)

  val str = listOf(u1F60x, u1F61x, u1F62x, u1F63x, u1F64x).joinToString(separator = "\n")
  val textArea = JTextArea(str)
  textArea.addCaretListener { e ->
    runCatching {
      val dot = e.dot
      val mark = e.mark
      if (dot - mark == 0) {
        val doc = textArea.document
        var txt = doc.getText(dot, 1)
        var code = txt.codePointAt(0)
        if (Character.isHighSurrogate(code.toChar())) {
          txt = doc.getText(dot, 2)
          code = txt.codePointAt(0)
        }
        label.text = "%s: U+%04X".format(txt, code)
      } else {
        label.text = ""
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(textArea))
    it.add(label, BorderLayout.SOUTH)
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
