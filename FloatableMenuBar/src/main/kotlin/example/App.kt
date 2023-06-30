package example

import java.awt.*
import javax.swing.*
import javax.swing.event.MenuEvent
import javax.swing.event.MenuListener
import javax.swing.plaf.basic.BasicToolBarUI

fun makeUI(): Component {
  val menuBar = JMenuBar()
  menuBar.add(makeMenu("JMenu 1"))

  val menu2 = makeMenu("JMenu 2")
  menu2.addMenuListener(object : MenuListener {
    private fun isFloating(menu: JMenu): Boolean {
      val c = SwingUtilities.getAncestorOfClass(JToolBar::class.java, menu)
      return c is JToolBar && !(c.ui as BasicToolBarUI).isFloating
    }

    override fun menuSelected(e: MenuEvent) {
      val menu = e.source
      if (menu is JMenu && menu.isTopLevelMenu && isFloating(menu)) {
        val d = menu.preferredSize
        val p = menu.location
        val cp = menu.rootPane.contentPane
        val pt = SwingUtilities.convertPoint(menu.parent, p, cp)
        pt.y += d.height * 2
        if (!cp.bounds.contains(pt)) {
          EventQueue.invokeLater {
            val popup = menu.popupMenu
            val bounds = popup.bounds
            val loc = menu.locationOnScreen
            loc.y -= bounds.height + UIManager.getInt("Menu.menuPopupOffsetY")
            popup.location = loc
          }
        }
      }
    }

    override fun menuDeselected(e: MenuEvent) {
      // Do nothing
    }

    override fun menuCanceled(e: MenuEvent) {
      // Do nothing
    }
  })
  menuBar.add(menu2)

  val toolBar = JToolBar()
  toolBar.layout = BorderLayout()
  toolBar.add(menuBar)

  return JPanel(BorderLayout()).also {
    it.add(toolBar, BorderLayout.NORTH)
    it.add(Box.createRigidArea(Dimension()), BorderLayout.WEST)
    it.add(Box.createRigidArea(Dimension()), BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenu(title: String): JMenu {
  val menu = JMenu(title)
  menu.add("1")
  menu.add("22")
  menu.add("333")
  menu.addSeparator()
  menu.add("4444")
  menu.add("55555")
  return menu
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
