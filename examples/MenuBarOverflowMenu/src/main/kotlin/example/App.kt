package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import javax.swing.*

fun makeUI(): Component {
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
  val menuBar = JMenuBar()
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
  val b = menuBar.getComponentOrientation().isLeftToRight
  listOf(
    JRadioButtonMenuItem("LEFT_TO_RIGHT", b),
    JRadioButtonMenuItem("RIGHT_TO_LEFT", !b),
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
  private val popupButton = JMenu("...")

  override fun layoutContainer(target: Container) {
    super.layoutContainer(target)
    val r = SwingUtilities.calculateInnerArea(target as? JComponent, null)
    val num = target.componentCount
    if (target.getComponent(num - 1).getY() > r.y + getVgap()) {
      target.add(popupButton)
      popupButton.size = popupButton.getPreferredSize()
      val popupX = if (target.getComponentOrientation().isLeftToRight) {
        r.x + r.width - popupButton.size.width
      } else {
        r.x
      }
      popupButton.setLocation(popupX, r.y)
      popupButton.isVisible = true
    }
    if (target.isAncestorOf(popupButton)) {
      target
        .components
        .filter { shouldMoveToPopup(target, it) }
        .forEach { popupButton.getPopupMenu().add(it) }
    }
  }

  private fun shouldMoveToPopup(target: Container, c: Component): Boolean {
    val insets = target.insets
    val y = insets.top + getVgap()
    val pt = popupButton.location
    if (!target.getComponentOrientation().isLeftToRight) {
      pt.x += popupButton.getWidth()
    }
    val b = c != popupButton && c.bounds.contains(pt)
    return c.getY() > y || b
  }

  override fun preferredLayoutSize(target: Container): Dimension? {
    popupButton.isVisible = false
    target.remove(popupButton)
    for (c in popupButton.getPopupMenu().components) {
      target.add(c)
    }
    popupButton.getPopupMenu().removeAll()
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
