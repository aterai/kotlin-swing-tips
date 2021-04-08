package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p1 = JPanel(GridLayout(1, 0, 2, 2)).also {
    it.border = BorderFactory.createTitledBorder("GridLayout")
    it.add(JScrollPane(JTable(6, 3)))
    it.add(JScrollPane(JTree()))
    it.add(JScrollPane(JTextArea("JTextArea")))
  }

  val p2 = JPanel(GridBagLayout()).also {
    it.border = BorderFactory.createTitledBorder("GridBagLayout")
    val c = GridBagConstraints()
    c.insets = Insets(5, 5, 5, 0)
    c.fill = GridBagConstraints.BOTH
    c.weightx = 1.0
    c.weighty = 1.0
    it.add(JScrollPane(JTable(6, 3)), c)
    it.add(JScrollPane(JTree()), c)
    it.add(JScrollPane(JTextArea("JTextArea")), c)
  }

  val button = JButton("rotate")
  button.isFocusable = false
  button.addActionListener {
    rotateChildComponent(p1)
    rotateChildComponent(p2)
  }

  val p = JPanel(GridLayout(2, 1)).also {
    it.add(p1)
    it.add(p2)
  }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun rotateChildComponent(p: Container) {
  // p.setComponentZOrder(p.getComponent(p.componentCount - 1), 0)
  p.setComponentZOrder(p.components.last(), 0)
  p.revalidate()
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
