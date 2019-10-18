package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel() {
  init {
    val key = "Menu.crossMenuMnemonic"
    val b = UIManager.getBoolean(key)
    println("$key: $b")
    val check = object : JCheckBox(key, b) {
      override fun updateUI() {
        super.updateUI()
        setSelected(UIManager.getLookAndFeelDefaults().getBoolean(key))
        UIManager.put(key, isSelected())
      }
    }
    check.addActionListener { e: ActionEvent ->
      UIManager.put(key, (e.getSource() as? JCheckBox)?.isSelected() == true)
      SwingUtilities.updateComponentTreeUI(getRootPane().getJMenuBar())
    }
    add(check)
    setPreferredSize(Dimension(320, 240))
  }
}

object MenuBarUtil {
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
    menu.setMnemonic(KeyEvent.VK_F)
    menu.add("New").setMnemonic(KeyEvent.VK_N)
    menu.add("Open").setMnemonic(KeyEvent.VK_O)
    return menu
  }

  private fun createEditMenu(): JMenu {
    val menu = JMenu("Edit")
    menu.setMnemonic(KeyEvent.VK_E)
    menu.add("Cut").setMnemonic(KeyEvent.VK_T)
    menu.add("Copy").setMnemonic(KeyEvent.VK_C)
    menu.add("Paste").setMnemonic(KeyEvent.VK_P)
    menu.add("Delete").setMnemonic(KeyEvent.VK_D)
    return menu
  }

  private fun createHelpMenu(): JMenu {
    val menu = JMenu("Help")
    menu.setMnemonic(KeyEvent.VK_H)
    menu.add("About").setMnemonic(KeyEvent.VK_A)
    menu.add("Version").setMnemonic(KeyEvent.VK_V)
    return menu
  }
}

// @see https://java.net/projects/swingset3/sources/svn/content/trunk/SwingSet3/src/com/sun/swingset3/SwingSet3.java
object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    it.setMnemonic(KeyEvent.VK_L)
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setMnemonic(lafName.codePointAt(0))
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }
    lafRadioGroup.add(lafItem)
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
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setJMenuBar(MenuBarUtil.createMenuBar())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
