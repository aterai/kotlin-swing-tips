package example

import java.awt.*
import javax.swing.*

fun createUI(): Component {
  val model = SpinnerNumberModel(10, 0, 1000, 1)

  val disabledSpinner = JSpinner(model)
  disabledSpinner.isEnabled = false

  val readOnlySpinner = JSpinner(model)
  (readOnlySpinner.editor as? JSpinner.DefaultEditor)?.textField?.also {
    it.isEditable = false
    // A non-editable text field is painted with the inactive background color,
    // so restore the normal background to look the same as the editable one
    it.background = UIManager.getColor("FormattedTextField.background")
  }

  val box = Box.createVerticalBox()
  box.add(makeTitledPanel("Default", JSpinner(model)))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("spinner.setEnabled(false)", disabledSpinner))
  box.add(Box.createVerticalStrut(10))
  box.add(makeTitledPanel("field.setEditable(false)", readOnlySpinner))

  val p = JPanel(BorderLayout())
  p.add(box, BorderLayout.NORTH)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeTitledPanel(
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
