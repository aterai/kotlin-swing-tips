package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val check = JCheckBox("JCheckBox#setIcon(...)")
  check.icon = CheckIcon()
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(check, BorderLayout.SOUTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

fun createMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  val menu = JMenu("JMenu")
  menuBar.add(menu)
  menu.add(JCheckBoxMenuItem("default"))

  val key = "CheckBoxMenuItem.checkIcon"
  val defIcon = UIManager.getIcon(key)
  UIManager.put(key, CheckIcon())
  menu.add(JCheckBoxMenuItem("checkIcon test"))
  UIManager.put(key, defIcon)

  val check = JCheckBoxMenuItem("setIcon").also {
    it.icon = CheckIcon()
  }
  val menu2 = JMenu("JMenu2")
  menu2.add(check)
  menuBar.add(menu)
  menuBar.add(menu2)
  return menuBar
}

private class CheckIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = if ((c as? AbstractButton)?.isSelected == true) Color.ORANGE else Color.GRAY
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.fillOval(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 14

  override fun getIconHeight() = 14
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
