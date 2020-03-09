package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JSpinner.DefaultEditor

fun makeUI(): Component {
  val field = JTextField(20)
  field.toolTipText = "JTextField#setColumns(20)"

  val password = JPasswordField(20)
  password.toolTipText = "JPasswordField#setColumns(20)"

  val spinner = JSpinner()
  spinner.toolTipText = "JSpinner#setColumns(20)"
  (spinner.editor as DefaultEditor).textField.columns = 20

  val combo1 = JComboBox<String>()
  combo1.isEditable = true
  combo1.toolTipText = "JComboBox setEditable(true), setColumns(20)"
  (combo1.editor.editorComponent as JTextField).columns = 20

  val combo2 = JComboBox<String>()
  combo2.toolTipText = "JComboBox setEditable(true), default"
  combo2.isEditable = true

  val combo3 = JComboBox<String>()
  combo3.toolTipText = "JComboBox setEditable(false), default"

  val layout = SpringLayout()
  val p = JPanel(layout)
  layout.putConstraint(SpringLayout.WEST, field, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.WEST, password, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.WEST, spinner, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.WEST, combo1, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.WEST, combo2, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.WEST, combo3, 10, SpringLayout.WEST, p)
  layout.putConstraint(SpringLayout.NORTH, field, 10, SpringLayout.NORTH, p)
  layout.putConstraint(SpringLayout.NORTH, password, 10, SpringLayout.SOUTH, field)
  layout.putConstraint(SpringLayout.NORTH, spinner, 10, SpringLayout.SOUTH, password)
  layout.putConstraint(SpringLayout.NORTH, combo1, 10, SpringLayout.SOUTH, spinner)
  layout.putConstraint(SpringLayout.NORTH, combo2, 10, SpringLayout.SOUTH, combo1)
  layout.putConstraint(SpringLayout.NORTH, combo3, 10, SpringLayout.SOUTH, combo2)
  listOf(field, password, spinner, combo1, combo2, combo3).forEach { p.add(it) }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
