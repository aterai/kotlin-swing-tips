package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.MaskFormatter

fun makeUI(): Component {
  val formatter = runCatching { MaskFormatter("U".repeat(10)) }.getOrNull()

  val field1 = JFormattedTextField(formatter)
  field1.focusLostBehavior = JFormattedTextField.REVERT

  val field2 = JFormattedTextField(formatter)
  field2.focusLostBehavior = JFormattedTextField.COMMIT

  val field3 = JFormattedTextField(formatter)
  field3.focusLostBehavior = JFormattedTextField.PERSIST

  val check = JCheckBox("setCommitsOnValidEdit")
  check.addActionListener { e ->
    formatter?.commitsOnValidEdit = (e.source as? JCheckBox)?.isSelected == true
  }

  val box = Box.createVerticalBox().also {
    it.add(makeTitledPanel("COMMIT_OR_REVERT(default)", JFormattedTextField(formatter)))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("REVERT", field1))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("COMMIT", field2))
    it.add(Box.createVerticalStrut(5))
    it.add(makeTitledPanel("PERSIST", field3))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
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
