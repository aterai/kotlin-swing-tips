package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val p = JPanel(GridLayout(3, 1, 5, 5))
  // val url1 = javaClass.classLoader.getResource("example/test.png")
  // val url2 = javaClass.getResource("test.png")
  val url1 = Thread.currentThread().contextClassLoader.getResource("example/test.png")
  url1?.also {
    p.add(JLabel(ImageIcon(it)))
    p.add(makeTitledPanel("ClassLoader.getResource(\"example/test.png\")", JLabel(it.toString())))
  }
  // p.add(makeTitledPanel("getClass().getResource(\"test.png\")", JLabel(url2.toString())))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
