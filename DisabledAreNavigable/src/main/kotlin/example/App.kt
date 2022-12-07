package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val key = "MenuItem.disabledAreNavigable"
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
  }

  return JPanel().also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = MenuBarUtil.createMenuBar() }
    val popup = JPopupMenu()
    MenuBarUtil.initMenu(popup)
    it.componentPopupMenu = popup
    it.add(check)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ExitAction : AbstractAction("Exit") {
  override fun actionPerformed(e: ActionEvent) {
    val parent = SwingUtilities.getUnwrappedParent(e.source as? Component)
    val root = if (parent is JPopupMenu) {
      SwingUtilities.getRoot(parent.invoker)
    } else if (parent is JToolBar) {
      if ((parent.ui as? BasicToolBarUI)?.isFloating == true) {
        SwingUtilities.getWindowAncestor(parent).owner
      } else {
        SwingUtilities.getRoot(parent)
      }
    } else {
      SwingUtilities.getRoot(parent)
    }
    if (root is Window) {
      root.dispatchEvent(WindowEvent(root, WindowEvent.WINDOW_CLOSING))
    }
  }
}

private object MenuBarUtil {
  fun createMenuBar(): JMenuBar {
    val mb = JMenuBar()
    val file = JMenu("File")
    initMenu(file)
    mb.add(file)

    val edit = JMenu("Edit")
    listOf("Cut", "Copy", "Paste", "Delete").map { edit.add(it) }.forEach { it.isEnabled = false }
    mb.add(edit)

    mb.add(LookAndFeelUtil.createLookAndFeelMenu())
    mb.add(Box.createGlue())

    val help = JMenu("Help")
    help.add("About")
    mb.add(help)
    return mb
  }

  fun initMenu(p: Container) {
    var item = JMenuItem("Open(disabled)")
    item.isEnabled = false
    p.add(item)
    item = JMenuItem("Save(disabled)")
    item.isEnabled = false
    p.add(item)
    p.add(JSeparator())
    p.add(JMenuItem(ExitAction()))
  }
}

private object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.name, lafInfo.className, lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(
    lafName: String,
    lafClassName: String,
    lafGroup: ButtonGroup
  ): JMenuItem {
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
