package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  val label = JLabel()
  label.text = "<html>dockable: NORTH, SOUTH<br>undockable: EAST, WEST"
  label.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  it.add(makeToolBar(), BorderLayout.NORTH)
  it.add(Box.createRigidArea(Dimension()), BorderLayout.WEST)
  it.add(Box.createRigidArea(Dimension()), BorderLayout.EAST)
  it.add(label)
  it.preferredSize = Dimension(320, 240)
}

private fun makeToolBar() = JToolBar().also {
  it.add(JLabel("label"))
  it.add(Box.createRigidArea(Dimension(5, 5)))

  val button = JButton("button")
  button.isFocusable = false
  it.add(button)
  it.add(Box.createRigidArea(Dimension(5, 5)))

  it.add(JComboBox(makeModel()))
  it.add(Box.createGlue())
}

private fun makeModel() = DefaultComboBoxModel<String>().also {
  it.addElement("1111111")
  it.addElement("22222")
  it.addElement("3333333333333333")
  it.addElement("44444444444")
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
