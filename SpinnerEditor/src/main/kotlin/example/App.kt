package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val model = SpinnerNumberModel(10, 0, 1000, 1)

  val spinner1 = JSpinner(model)
  spinner1.isEnabled = false
  // UIManager.put("FormattedTextField.inactiveBackground", Color.RED)

  val spinner2 = JSpinner(model)
  (spinner2.editor as? JSpinner.NumberEditor)?.textField?.also {
    it.isEditable = false
    it.background = UIManager.getColor("FormattedTextField.background")
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default", JSpinner(model)))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("spinner.setEnabled(false)", spinner1))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("editor.setEditable(false)", spinner2))

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeTitledPanel(title: String, c: Component): Component {
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
