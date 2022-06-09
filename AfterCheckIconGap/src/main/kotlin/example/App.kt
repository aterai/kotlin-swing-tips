package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater {
    it.rootPane.jMenuBar = createMenuBar()
  }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

fun createMenuBar(): JMenuBar {
  // UIManager.put("MenuItem.checkIconOffset", 20)
  // UIManager.put("MenuItem.afterCheckIconGap", 20)
  UIManager.put("MenuItem.minimumTextOffset", 20 + 20 + 31 - 9)

  UIManager.put("CheckBoxMenuItem.afterCheckIconGap", 20)
  UIManager.put("CheckBoxMenuItem.checkIconOffset", 20)
  // UIManager.put("CheckBoxMenuItem.minimumTextOffset", 100)

  val menuBar = JMenuBar()
  val menu = makeMenu("JMenu")
  menuBar.add(menu)

  menu.add(makeMenu("JMenu 1"))
  menu.add(makeMenu("JMenu 2"))

  menuBar.add(menu)
  menuBar.add(makeMenu("JMenu 3"))
  return menuBar
}

fun makeMenu(title: String): JMenu {
  val menu = JMenu(title)
  menu.add(JMenuItem("JMenuItem 1"))
  menu.add(JMenuItem("JMenuItem 2"))
  menu.add(JCheckBoxMenuItem("JCheckBoxMenuItem 1"))
  menu.add(JCheckBoxMenuItem("JCheckBoxMenuItem 2"))

  val rmi1 = JRadioButtonMenuItem("JRadioButtonMenuItem 1")
  val rmi2 = JRadioButtonMenuItem("JRadioButtonMenuItem 2")
  val bg = ButtonGroup()
  bg.add(rmi1)
  bg.add(rmi2)
  menu.add(rmi1)
  menu.add(rmi2)
  return menu
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      // UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
