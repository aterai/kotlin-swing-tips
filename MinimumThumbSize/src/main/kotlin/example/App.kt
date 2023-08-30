package example

import java.awt.*
import javax.swing.*

private const val LF = "\n"

fun makeUI(): Component {
  val buf = StringBuilder()
  for (i in 0 until 1000) {
    buf.append(i).append(LF)
  }
  val s1 = JScrollPane(JTextArea(buf.toString()))

  UIManager.put("ScrollBar.minimumThumbSize", Dimension(32, 32))
  val s2 = JScrollPane(JTextArea(buf.toString()))

  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s1, s2).also {
    it.resizeWeight = .5
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
