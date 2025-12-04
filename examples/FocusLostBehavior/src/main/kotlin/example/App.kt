package example

import java.awt.*
import javax.swing.*
import javax.swing.text.MaskFormatter

fun makeUI(): Component {
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val formatter = runCatching { MaskFormatter("U".repeat(10)) }.getOrNull()
  val field0 = JFormattedTextField(formatter)
  box.add(makeTitledPanel("COMMIT_OR_REVERT(default)", field0))
  box.add(Box.createVerticalStrut(5))

  val field1 = JFormattedTextField(formatter)
  field1.focusLostBehavior = JFormattedTextField.REVERT
  box.add(makeTitledPanel("REVERT", field1))
  box.add(Box.createVerticalStrut(5))

  val field2 = JFormattedTextField(formatter)
  field2.focusLostBehavior = JFormattedTextField.COMMIT
  box.add(makeTitledPanel("COMMIT", field2))
  box.add(Box.createVerticalStrut(5))

  val field3 = JFormattedTextField(formatter)
  field3.focusLostBehavior = JFormattedTextField.PERSIST
  box.add(makeTitledPanel("PERSIST", field3))

  val check = JCheckBox("setCommitsOnValidEdit")
  check.addActionListener { e ->
    formatter?.commitsOnValidEdit = (e.source as? JCheckBox)?.isSelected == true
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(check, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
) = JPanel(BorderLayout()).also {
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
