package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tree = JTree()
  tree.isEditable = true

  val check = JCheckBox("InvokesStopCellEditing")
  check.addActionListener { e ->
    tree.invokesStopCellEditing = (e.source as? JCheckBox)?.isSelected == true
  }

  return JPanel(BorderLayout(5, 5)).also {
    it.add(check, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.add(JTextField(), BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
