package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val desktop = JDesktopPane()
  desktop.add(createFrame(0))
  desktop.add(createFrame(1))
  desktop.add(createFrame(2))

  val button = JToggleButton("InternalFrame.useTaskBar")
  button.addActionListener { e ->
    (e.source as? AbstractButton)?.also {
      UIManager.put("InternalFrame.useTaskBar", it.isSelected)
      SwingUtilities.updateComponentTreeUI(desktop.rootPane)
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(desktop)
    it.add(button, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createFrame(i: Int): JInternalFrame {
  val f = JInternalFrame("title: $i", true, true, true, true)
  f.setSize(160, 120)
  f.setLocation(10 + 20 * i, 10 + 20 * i)
  EventQueue.invokeLater { f.isVisible = true }
  return f
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
