package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val p = JPanel(GridLayout(0, 1, 5, 5))
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val path = "example/test.png"
  Thread.currentThread().contextClassLoader.getResource(path)?.also {
    p.add(JLabel(ImageIcon(it)))
    val title = "ClassLoader.getResource(\"$path\")"
    p.add(makeTitledPanel(title, JLabel(it.toString())))
  }

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
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
