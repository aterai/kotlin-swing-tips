package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val group1 = ButtonGroup()
  val p1 = makePanel(group1)
  p1.isFocusTraversalPolicyProvider = true
  p1.focusTraversalPolicy = ContainerOrderFocusTraversalPolicy()

  val group2 = ButtonGroup()
  val p2 = makePanel(group2)
  p2.isFocusTraversalPolicyProvider = true
  p2.focusTraversalPolicy = object : ContainerOrderFocusTraversalPolicy() {
    override fun getDefaultComponent(focusCycleRoot: Container): Component {
      val selection = group2.selection
      return focusCycleRoot.components
        .filterIsInstance<JRadioButton>()
        .firstOrNull { it.model == selection }
        ?: super.getDefaultComponent(focusCycleRoot)
    }
  }

  val tabbedPane = JTabbedPane()
  tabbedPane.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  tabbedPane.border = BorderFactory.createTitledBorder("FocusTraversalPolicy")
  tabbedPane.addTab("Layout", makePanel(ButtonGroup()))
  tabbedPane.addTab("ContainerOrder", p1)
  tabbedPane.addTab("ContainerOrder + ButtonGroup", p2)

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(group: ButtonGroup): Container {
  val p = JPanel(GridBagLayout())
  p.isFocusable = false
  val gbc = GridBagConstraints()
  gbc.fill = GridBagConstraints.HORIZONTAL
  gbc.gridx = 1
  val list = listOf<JComponent>(
    JRadioButton("JRadioButton1"),
    JRadioButton("JRadioButton2"),
    JRadioButton("JRadioButton3", true),
    JLabel("JLabel1"),
    JLabel("JLabel2"),
    JCheckBox("JCheckBox1"),
    JCheckBox("JCheckBox2"),
  )
  for (c in list) {
    if (c is JRadioButton) {
      group.add(c)
    } else if (c is JLabel) {
      c.setFocusable(false)
    }
    p.add(c, gbc)
  }
  gbc.gridx = 2
  gbc.weightx = 1.0
  list.forEach { p.add(JTextField(), gbc) }
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
