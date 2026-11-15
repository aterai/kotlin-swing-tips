package example

import java.awt.*
import javax.swing.*

fun createUI(): Component {
  val p1 = JPanel(GridLayout(1, 0, 2, 2)).also {
    it.border = BorderFactory.createTitledBorder("GridLayout")
    createComponents().forEach(it::add)
  }

  val p2 = JPanel(GridBagLayout()).also {
    it.border = BorderFactory.createTitledBorder("GridBagLayout")
    val c = GridBagConstraints()
    c.insets = Insets(5, 5, 5, 0)
    c.fill = GridBagConstraints.BOTH
    c.weightx = 1.0
    c.weighty = 1.0
    // Keep gridx and gridy at the default GridBagConstraints.RELATIVE,
    // otherwise changing the z-order does not change the layout position
    createComponents().forEach { child -> it.add(child, c) }
  }

  val button = JButton("rotate")
  button.isFocusable = false
  button.addActionListener {
    rotateChildComponents(p1)
    rotateChildComponents(p2)
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

private fun createComponents(): List<Component> = listOf(
  JScrollPane(JTable(6, 3)),
  JScrollPane(JTree()),
  JScrollPane(JTextArea("JTextArea")),
)

// Moves the last child component to the front (z-order index 0),
// which shifts the other child components back by one
private fun rotateChildComponents(parent: Container) {
  val count = parent.componentCount
  if (count > 0) {
    parent.setComponentZOrder(parent.getComponent(count - 1), 0)
    parent.revalidate()
    parent.repaint()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
