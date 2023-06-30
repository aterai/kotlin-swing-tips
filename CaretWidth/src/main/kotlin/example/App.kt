package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  UIManager.put("Caret.width", 2)
  val field1 = JTextField("Caret.width: 2")

  val field2 = JTextField("caretWidth: 4")
  field2.putClientProperty("caretWidth", 4)

  val field3 = JTextField("caretAspectRatio: 0.4")
  field3.putClientProperty("caretAspectRatio", .4f)

  val color = Color(0x64_FF_00_00, true)
  val font = field1.font.deriveFont(32f)
  val box = Box.createVerticalBox()
  listOf(field1, field2, field3).forEach {
    it.font = font
    it.caretColor = color
    box.add(it)
    box.add(Box.createVerticalStrut(10))
  }

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(box, BorderLayout.NORTH)
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
