package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val spinner1 = JSpinner(SpinnerNumberModel(0.0, 0.0, 1.0, .01))
  val editor1 = JSpinner.NumberEditor(spinner1, "0%")
  spinner1.editor = editor1

  val spinner2 = JSpinner(SpinnerNumberModel(0.0, 0.0, 1.0, .01))
  val editor2 = JSpinner.NumberEditor(spinner2, "0%")
  editor2.textField.isEditable = false
  editor2.textField.background = UIManager.getColor("FormattedTextField.background")
  spinner2.editor = editor2

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("JSpinner", spinner1))
    it.add(makeTitledPanel("getTextField().setEditable(false)", spinner2))
    it.border = BorderFactory.createEmptyBorder(10, 5, 10, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  cmp: Component,
): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  p.add(cmp, c)
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
