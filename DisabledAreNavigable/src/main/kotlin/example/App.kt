package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.WindowEvent
import javax.swing.*
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
    EventQueue.invokeLater { it.rootPane.jMenuBar = MenuBarUtils.createMenuBar() }
    val popup = JPopupMenu()
    MenuBarUtils.initMenu(popup)
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

private object MenuBarUtils {
  fun createMenuBar(): JMenuBar {
    val mb = JMenuBar()
    val file = JMenu("File")
    initMenu(file)
    mb.add(file)

    val edit = JMenu("Edit")
    listOf("Cut", "Copy", "Paste", "Delete").map { edit.add(it) }.forEach { it.isEnabled = false }
    mb.add(edit)

    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
