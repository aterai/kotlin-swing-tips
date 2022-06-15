package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val TOOLTIP_MODE = "ToolTipManager.enableToolTipMode"

fun makeUI(): Component {
  val mode = UIManager.getString(TOOLTIP_MODE)
  // println(mode)
  val allWindows = "allWindows"
  val radio1 = JRadioButton(allWindows, allWindows == mode)
  radio1.toolTipText = "ToolTip: $allWindows"
  radio1.addItemListener {
    UIManager.put(TOOLTIP_MODE, allWindows)
  }
  val activeApplication = "activeApplication"
  val radio2 = JRadioButton(activeApplication, activeApplication == mode)
  radio2.toolTipText = "ToolTip: $activeApplication"
  radio2.addItemListener {
    UIManager.put(TOOLTIP_MODE, activeApplication)
  }
  val panel = JPanel()
  panel.border = BorderFactory.createTitledBorder(TOOLTIP_MODE)
  val group = ButtonGroup()
  listOf(radio1, radio2).forEach {
    group.add(it)
    panel.add(it)
  }
  val p = JPanel()
  p.add(panel, BorderLayout.NORTH)
  p.add(makePanel())
  p.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makePanel(): Component {
  val label = JLabel("label")
  label.toolTipText = "JLabel"
  val field = JTextField(20)
  field.toolTipText = "JTextField"
  val button = JButton("button")
  button.toolTipText = "JButton"
  val p = JPanel()
  p.border = BorderFactory.createTitledBorder("test: $TOOLTIP_MODE")
  p.add(label)
  p.add(field)
  p.add(button)
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
