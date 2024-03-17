package example

import java.awt.*
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener

fun makeUI(): Component {
  val p = JPanel(BorderLayout())
  val mainMenuBar = makeMenuBar()
  val button = makeHamburgerMenuButton(mainMenuBar, p)
  val wrappingMenuBar = JMenuBar()
  wrappingMenuBar.add(makeToolBar(button))
  EventQueue.invokeLater { p.rootPane.jMenuBar = wrappingMenuBar }

  val handler = object : PopupMenuListener {
    override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
      // not need
    }

    override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
      EventQueue.invokeLater {
        if (MenuSelectionManager.defaultManager().selectedPath.size == 0) {
          p.rootPane.jMenuBar = wrappingMenuBar
        }
      }
    }

    override fun popupMenuCanceled(e: PopupMenuEvent) {
      EventQueue.invokeLater { p.rootPane.jMenuBar = wrappingMenuBar }
    }
  }
  for (i in 0 until mainMenuBar.menuCount) {
    mainMenuBar.getMenu(i).popupMenu.addPopupMenuListener(handler)
  }
  p.add(JScrollPane(JTextArea()))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeHamburgerMenuButton(menuBar: JMenuBar, p: JComponent): JButton {
  val button = object : JButton("ƒ¬") {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = menuBar.getMenu(0).preferredSize.height
      return d
    }

    override fun updateUI() {
      super.updateUI()
      isContentAreaFilled = false
      isFocusPainted = false
      border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
    }
  }
  button.addActionListener {
    val root = p.rootPane
    root.jMenuBar = menuBar
    root.revalidate()
    EventQueue.invokeLater { menuBar.getMenu(0).doClick() }
  }
  button.setMnemonic('\\')
  button.toolTipText = "Main Menu(Alt+\\)"
  return button
}

private fun makeToolBar(button: JButton): JToolBar {
  val toolBar = JToolBar()
  toolBar.isFloatable = false
  toolBar.add(button)
  toolBar.add(Box.createHorizontalStrut(5))
  toolBar.add(JLabel("<- Switch to JMenuBar"))
  val check = JCheckBox("JCheckBox")
  check.isOpaque = false
  toolBar.add(check)
  return toolBar
}

private fun makeMenuBar(): JMenuBar {
  val menuBar = JMenuBar()
  menuBar.add(makeMenu("JMenu1"))
  menuBar.add(makeMenu("JMenu2"))
  menuBar.add(makeMenu("JMenu3"))
  menuBar.add(makeMenu("JMenu4"))
  menuBar.add(makeMenu("JMenu5"))
  return menuBar
}

private fun makeMenu(title: String): JMenu {
  val menu = JMenu(title)
  menu.add("1")
  menu.add("22")
  menu.add("333")
  menu.addSeparator()
  menu.add("4444")
  menu.add("55555")
  val sub = JMenu("sub")
  sub.add("666")
  sub.add("777")
  menu.add(sub)
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
