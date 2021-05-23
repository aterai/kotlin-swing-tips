package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val key = "Menu.crossMenuMnemonic"
  val b = UIManager.getBoolean(key)
  println("$key: $b")

  val check = object : JCheckBox(key, b) {
    override fun updateUI() {
      super.updateUI()
      isSelected = UIManager.getLookAndFeelDefaults().getBoolean(key)
      UIManager.put(key, isSelected)
    }
  }
  check.addActionListener { e ->
    UIManager.put(key, (e.source as? JCheckBox)?.isSelected == true)
    SwingUtilities.updateComponentTreeUI(check.rootPane.jMenuBar)
  }

  return JPanel().also {
    EventQueue.invokeLater {
      it.rootPane.jMenuBar = MenuBarUtil.createMenuBar()
    }
    it.add(check)
    it.preferredSize = Dimension(320, 240)
  }
}

private object MenuBarUtil {
  fun createMenuBar(): JMenuBar {
    val mb = JMenuBar()
    mb.add(createFileMenu())
    mb.add(createEditMenu())
    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    mb.add(Box.createGlue())
    mb.add(createHelpMenu())
    return mb
  }

  private fun createFileMenu(): JMenu {
    val menu = JMenu("File")
    menu.mnemonic = KeyEvent.VK_F
    menu.add("New").mnemonic = KeyEvent.VK_N
    menu.add("Open").mnemonic = KeyEvent.VK_O
    return menu
  }

  private fun createEditMenu(): JMenu {
    val menu = JMenu("Edit")
    menu.mnemonic = KeyEvent.VK_E
    menu.add("Cut").mnemonic = KeyEvent.VK_T
    menu.add("Copy").mnemonic = KeyEvent.VK_C
    menu.add("Paste").mnemonic = KeyEvent.VK_P
    menu.add("Delete").mnemonic = KeyEvent.VK_D
    return menu
  }

  private fun createHelpMenu(): JMenu {
    val menu = JMenu("Help")
    menu.mnemonic = KeyEvent.VK_H
    menu.add("About").mnemonic = KeyEvent.VK_A
    menu.add("Version").mnemonic = KeyEvent.VK_V
    return menu
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    it.mnemonic = KeyEvent.VK_L
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.actionCommand = lafClassName
    lafItem.hideActionText = true
    lafItem.addActionListener { e ->
      val m = lafGroup.selection
      runCatching {
        setLookAndFeel(m.actionCommand)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(e.source as? Component)
      }
    }
    lafGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
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
