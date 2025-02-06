package example

import java.awt.*
import javax.swing.*

fun makeUI() = JPanel(BorderLayout()).also {
  val checkbox = JCheckBox("setResizable:", true)
  checkbox.addActionListener {
    (checkbox.topLevelAncestor as? Frame)?.isResizable = checkbox.isSelected
  }

  val p = JPanel()
  p.add(checkbox)
  p.border = BorderFactory.createTitledBorder("JFrame#setResizable(boolean)")

  it.add(p, BorderLayout.NORTH)
  it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  it.preferredSize = Dimension(320, 240)
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
