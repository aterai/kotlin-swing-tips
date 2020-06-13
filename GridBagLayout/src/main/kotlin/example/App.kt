package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val GAP = 5

private fun makeBorderLayoutPanel(cmp: JComponent, btn: JButton): Component {
  val panel = JPanel(BorderLayout(GAP, GAP))
  panel.border = BorderFactory.createEmptyBorder(GAP, GAP, GAP, GAP)
  panel.add(JLabel("BorderLayout:"), BorderLayout.WEST)
  panel.add(cmp)
  panel.add(btn, BorderLayout.EAST)
  val d = panel.preferredSize
  panel.maximumSize = Dimension(Int.MAX_VALUE, d.height)
  return panel
}

fun makeGridBagLayoutPanel(cmp: JComponent, btn: JButton): Component {
  val c = GridBagConstraints()
  val panel = JPanel(GridBagLayout())

  c.insets = Insets(GAP, GAP, GAP, 0)
  c.anchor = GridBagConstraints.LINE_END
  panel.add(JLabel("GridBagLayout:"), c)

  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  panel.add(cmp, c)

  c.weightx = 0.0
  c.insets = Insets(GAP, GAP, GAP, GAP)
  panel.add(btn, c)
  return panel
}

fun makeUI(): Component {
  val model = arrayOf("000", "1111", "22222", "333333")
  val box = Box.createVerticalBox()
  box.add(Box.createVerticalStrut(20))
  box.add(makeBorderLayoutPanel(JComboBox(model), JButton("Open")))
  box.add(Box.createVerticalStrut(20))
  box.add(makeGridBagLayoutPanel(JComboBox(model), JButton("Open")))
  box.add(Box.createVerticalStrut(20))
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
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
      minimumSize = Dimension(300, 120)
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
