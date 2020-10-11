package example

import com.sun.java.swing.plaf.windows.WindowsMenuItemUI
import com.sun.java.swing.plaf.windows.WindowsMenuUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputListener
import javax.swing.plaf.basic.BasicMenuItemUI

fun makeUI(): Component {
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createMenuBar(): JMenuBar {
  val menu0 = JMenu("Default")
  initMenu(menu0)
  // TEST: menu0.setInheritsPopupMenu(true)
  val menu1 = object : JMenu("DisableRightClick") {
    override fun updateUI() {
      super.updateUI()
      if (ui is WindowsMenuUI) {
        setUI(CustomWindowsMenuUI())
      }
    }

    override fun add(s: String): JMenuItem {
      val item = object : JMenuItem(s) {
        override fun updateUI() {
          super.updateUI()
          if (ui is WindowsMenuItemUI) {
            setUI(CustomWindowsMenuItemUI())
          }
        }
      }
      return add(item)
    }
  }
  initMenu(menu1)
  // TEST: menu1.setInheritsPopupMenu(true)

  val popup = JPopupMenu()
  popup.add("MenuItem 1").addActionListener { println("PopupMenu") }
  popup.add("MenuItem 2")
  popup.add("MenuItem 3")

  val mb = JMenuBar()
  mb.componentPopupMenu = popup
  mb.add(menu0)
  mb.add(menu1)
  return mb
}

private fun initMenu(menu: JMenu) {
  menu.add("MenuItem 1").addActionListener { println("MenuBar") }
  menu.addSeparator()
  menu.add("MenuItem 2")
  menu.add("MenuItem 3")
  menu.add("MenuItem 4")
}

private class CustomWindowsMenuUI : WindowsMenuUI() {
  override fun createMouseInputListener(c: JComponent): MouseInputListener {
    return object : BasicMenuItemUI.MouseInputHandler() {
      override fun mousePressed(e: MouseEvent) {
        if (SwingUtilities.isRightMouseButton(e)) {
          return
        }
        super.mousePressed(e)
      }
    }
  }
}

private class CustomWindowsMenuItemUI : WindowsMenuItemUI() {
  override fun createMouseInputListener(c: JComponent): MouseInputListener {
    return object : MouseInputHandler() {
      override fun mouseReleased(e: MouseEvent) {
        if (!menuItem.isEnabled || SwingUtilities.isRightMouseButton(e)) {
          return
        }
        super.mouseReleased(e)
      }
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
