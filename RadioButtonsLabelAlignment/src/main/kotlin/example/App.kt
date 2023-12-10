package example

import java.awt.*
import javax.swing.*

fun makeUI(): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
  p.focusTraversalPolicy = ContainerOrderFocusTraversalPolicy()
  p.isFocusTraversalPolicyProvider = true
  p.isFocusable = false

  val gbc = GridBagConstraints()
  gbc.fill = GridBagConstraints.HORIZONTAL
  gbc.gridx = 1
  gbc.insets.top = 5
  val group = ButtonGroup()
  val list = listOf<JComponent>(
    JRadioButton("JRadioButton1"),
    JRadioButton("JRadioButton2"),
    JRadioButton("JRadioButton3"),
    JLabel("JLabel1"),
    JLabel("JLabel2"),
    JCheckBox("JCheckBox1"),
    JCheckBox("JCheckBox2"),
  )
  val icon = UIManager.getIcon("RadioButton.icon")
  val iconWidth = icon?.iconWidth ?: 0
  var left = 0
  for (c in list) {
    gbc.insets.left = 0
    if (c is JRadioButton) {
      group.add(c)
      if (left == 0) {
        c.isSelected = true
        left = c.insets.left + iconWidth + c.iconTextGap
      }
    } else if (c is JLabel) {
      gbc.insets.left = left
    }
    p.add(c, gbc)
  }
  gbc.gridx = 2
  gbc.weightx = 1.0
  gbc.insets.left = 5
  list.forEach { p.add(JTextField(), gbc) }

  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(p, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
