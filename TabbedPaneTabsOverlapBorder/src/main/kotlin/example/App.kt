package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabs = makeTabbedPane()
  tabs.tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
  val list = listOf(makeTabbedPane(), tabs)

  val key = "TabbedPane.tabsOverlapBorder"
  val check = object : JCheckBox(key, UIManager.getBoolean(key)) {
    override fun updateUI() {
      super.updateUI()
      val b = UIManager.getLookAndFeelDefaults().getBoolean(key)
      isSelected = b
      UIManager.put(key, b)
      list.forEach { SwingUtilities.updateComponentTreeUI(it) }
    }
  }

  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    UIManager.put(key, b)
    list.forEach { SwingUtilities.updateComponentTreeUI(it) }
  }

  val p = JPanel(GridLayout(2, 1))
  list.forEach { p.add(it) }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { p.rootPane.jMenuBar = mb }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(check, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTabbedPane(): JTabbedPane {
  val tabs = JTabbedPane()
  for (i in 0 until 10) {
    tabs.addTab("title$i", JLabel("label$i"))
  }
  return tabs
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
