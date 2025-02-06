package example

import java.awt.*
import java.awt.event.KeyEvent
import javax.swing.*

fun makeUI(): Component {
  val key = "Menu.crossMenuMnemonic"
  val b = UIManager.getBoolean(key)
  // println("$key: $b")
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
      it.rootPane.jMenuBar = MenuBarUtils.createMenuBar()
    }
    it.add(check)
    it.preferredSize = Dimension(320, 240)
  }
}

private object MenuBarUtils {
  fun createMenuBar(): JMenuBar {
    val mb = JMenuBar()
    mb.add(createFileMenu())
    mb.add(createEditMenu())
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
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
