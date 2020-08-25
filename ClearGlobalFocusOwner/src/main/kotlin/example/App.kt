package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel()
  p.add(JTextField(16))
  p.add(JButton("button"))
  p.add(JTextField(16))
  p.add(JButton("button"))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(JLabel("JFrame:mouseClicked -> clearGlobalFocusOwner"), BorderLayout.SOUTH)
    EventQueue.invokeLater {
      val root = it.rootPane
      root.jMenuBar = createMenuBar()
      val ml = object : MouseAdapter() {
        override fun mouseClicked(e: MouseEvent) {
          println("clicked")
          KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner()
        }
      }
      root.addMouseListener(ml)
    }
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar() = JMenuBar().also {
  val fileMenu = JMenu("File")
  fileMenu.add("dummy")
  it.add(fileMenu)
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
