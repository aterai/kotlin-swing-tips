package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

fun createUI(): Component {
  val scroll = JScrollPane(JTextArea())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater {
      it.rootPane.jMenuBar = makeMenuBar()
    }
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenuBar(): JMenuBar {
  val menuBar = object : JMenuBar() {
    override fun updateUI() {
      super.updateUI()
      (layout as? OverflowMenuLayout)?.also {
        // The overflow menu may not be in the menu bar, update it explicitly
      SwingUtilities.updateComponentTreeUI(it.overflowMenu)
      }
    }
  }
  menuBar.setLayout(OverflowMenuLayout())
  menuBar.add(makeOrientationMenu(menuBar))
  menuBar.add(makeMenu("JMenu1"))
  menuBar.add(makeMenu("JMenu2"))
  menuBar.add(makeMenu("JMenu3"))
  menuBar.add(makeMenu("JMenu4"))
  menuBar.add(makeMenu("JMenu5"))
  menuBar.add(makeMenu("JMenu6"))
  menuBar.add(makeMenu("JMenu7"))
  menuBar.add(makeMenu("JMenu8"))
  return menuBar
}

private fun makeOrientationMenu(menuBar: JMenuBar): JMenu {
  val menu = JMenu("ComponentOrientation")
  val bg = ButtonGroup()
  val handler = ItemListener { e ->
    if (e.getStateChange() == ItemEvent.SELECTED) {
      val name = bg.getSelection().actionCommand
      val o = if (name == "LEFT_TO_RIGHT") {
        ComponentOrientation.LEFT_TO_RIGHT
      } else {
        ComponentOrientation.RIGHT_TO_LEFT
      }
      menuBar.applyComponentOrientation(o)
      menuBar.revalidate()
    }
  }
  val isLeftToRight = menuBar.componentOrientation.isLeftToRight
  listOf(
    JRadioButtonMenuItem("LEFT_TO_RIGHT", isLeftToRight),
    JRadioButtonMenuItem("RIGHT_TO_LEFT", !isLeftToRight),
  ).forEach {
    bg.add(it)
    menu.add(it)
    it.actionCommand = it.text
    it.addItemListener(handler)
  }
  return menu
}

private fun makeMenu(text: String): JMenu {
  val menu = JMenu(text)
  menu.add("JMenuItem1")
  menu.add("JMenuItem2")
  menu.addSeparator()
  menu.add(JCheckBoxMenuItem("JCheckBoxMenuItem"))
  menu.add(JRadioButtonMenuItem("JRadioButtonMenuItem"))
  return menu
}

private class OverflowMenuLayout : FlowLayout(LEADING, 0, 0) {
  val overflowMenu = JMenu("...")

  override fun layoutContainer(target: Container) {
    // Move all menus back to the menu bar before laying them out, even if
    // preferredLayoutSize(...) has not been called since the last layout
    restoreOverflowedMenus(target)
    super.layoutContainer(target)
    val r = SwingUtilities.calculateInnerArea(target as? JComponent, null)
    val num = target.componentCount
    // The menus wrapped to the second row or later overflow the menu bar
    if (num > 0 && target.getComponent(num - 1).y > r.y + vgap) {
      // Add the overflow menu before getting its preferred size, since a JMenu
      // that is not in a JMenuBar is measured as a (larger) submenu item
      target.add(overflowMenu)
      val d = overflowMenu.preferredSize
      val x = if (target.componentOrientation.isLeftToRight) {
        r.x + r.width - d.width
      } else {
        r.x
      }
      overflowMenu.setBounds(x, r.y, d.width, d.height)
      target.components
        .filter { shouldMoveToOverflowMenu(r, it) }
        .forEach { overflowMenu.add(it) }
      // `popupMenu.pack()` is not necessary in Java 8,
      // but if you do not do this in Java 21,
      // the JPopupMenu may not be displayed correctly.
      overflowMenu.popupMenu.pack()
    }
  }

  private fun shouldMoveToOverflowMenu(r: Rectangle, c: Component): Boolean {
    // Rectangle#intersects(...) excludes menus that are only adjacent to the overflow menu
    val overlapped = c != overflowMenu && c.bounds.intersects(overflowMenu.bounds)
    return c.y > r.y + vgap || overlapped
  }

  private fun restoreOverflowedMenus(target: Container) {
    target.remove(overflowMenu)
    for (c in overflowMenu.menuComponents) {
      target.add(c)
    }
  }

  override fun preferredLayoutSize(target: Container): Dimension? {
    restoreOverflowedMenus(target)
    return super.preferredLayoutSize(target)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
