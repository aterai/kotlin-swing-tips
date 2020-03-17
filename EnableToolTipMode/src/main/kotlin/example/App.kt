package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

private const val TOOLTIP_MODE = "ToolTipManager.enableToolTipMode"

fun makeUI(): Component {
  val mode = UIManager.getString(TOOLTIP_MODE)
  println(mode)
  val allWindows = "allWindows"
  val radio1 = JRadioButton(allWindows, allWindows == mode)
  radio1.setToolTipText("ToolTip: $allWindows")
  radio1.addItemListener {
    UIManager.put(TOOLTIP_MODE, allWindows)
  }
  val activeApplication = "activeApplication"
  val radio2 = JRadioButton(activeApplication, activeApplication == mode)
  radio2.setToolTipText("ToolTip: $activeApplication")
  radio2.addItemListener {
    UIManager.put(TOOLTIP_MODE, activeApplication)
  }
  val panel = JPanel()
  panel.setBorder(BorderFactory.createTitledBorder(TOOLTIP_MODE))
  val group = ButtonGroup()
  listOf(radio1, radio2).forEach {
    group.add(it)
    panel.add(it)
  }
  val p = JPanel()
  p.add(panel, BorderLayout.NORTH)
  p.add(makePanel())
  p.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
  p.setPreferredSize(Dimension(320, 240))
  return p
}

private fun makePanel(): Component {
  val label = JLabel("label")
  label.setToolTipText("JLabel")
  val field = JTextField(20)
  field.setToolTipText("JTextField")
  val button = JButton("button")
  button.setToolTipText("JButton")
  val p = JPanel()
  p.setBorder(BorderFactory.createTitledBorder("test: $TOOLTIP_MODE"))
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
