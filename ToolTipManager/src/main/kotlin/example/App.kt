package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val label = JLabel("label")
  label.toolTipText = "JLabel - ToolTip"

  val field = JTextField(20)
  field.toolTipText = "JTextField"

  val button = JButton("button")
  button.toolTipText = "JButton - ToolTip"

  val p = JPanel()
  p.add(label)
  p.add(field)
  p.add(button)
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val panel = JPanel(BorderLayout())
  panel.border = BorderFactory.createTitledBorder("ToolTip Test")
  panel.add(p, BorderLayout.NORTH)
  panel.add(JScrollPane(JTextArea("dummy")))

  return JPanel(BorderLayout()).also {
    it.add(makeToolPanel(), BorderLayout.NORTH)
    it.add(panel)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeToolPanel(): Component {
  val radio = JRadioButton("on", true)
  radio.addItemListener { e ->
    ToolTipManager.sharedInstance().isEnabled = e.stateChange == ItemEvent.SELECTED
  }

  val panel = JPanel()
  panel.border = BorderFactory.createTitledBorder("ToolTipManager")
  panel.add(JLabel("ToolTip enabled:"))

  val group = ButtonGroup()
  listOf(radio, JRadioButton("off")).forEach {
    group.add(it)
    panel.add(it)
  }
  return panel
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
