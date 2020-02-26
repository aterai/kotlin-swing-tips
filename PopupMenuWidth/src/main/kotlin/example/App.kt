package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.DefaultMenuLayout

fun makeUI(): Component {
  val mb = JMenuBar()
  var menu = makeMenu(mb.add(JMenu("Default")))
  println(menu.popupMenu.preferredSize)
  menu = makeMenu(mb.add(JMenu("BoxHStrut")))
  menu.add(Box.createHorizontalStrut(200))
  menu = makeMenu(mb.add(JMenu("Override")))
  menu.add(object : JMenuItem("PreferredSize") {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 200
      return d
    }
  })
  menu = makeMenu(mb.add(JMenu("Layout")))
  val popup = menu.popupMenu
  popup.layout = object : DefaultMenuLayout(popup, BoxLayout.Y_AXIS) {
    override fun preferredLayoutSize(target: Container): Dimension {
      val d = super.preferredLayoutSize(target)
      d.width = 200.coerceAtLeast(d.width)
      return d
    }
  }
  menu = mb.add(JMenu("Html"))
  val item = menu.add("<html><table cellpadding='0' cellspacing='0' style='width:200'>Table")
  item.mnemonic = KeyEvent.VK_T
  makeMenu(menu)
  return JPanel(BorderLayout()).also {
    it.add(mb, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeMenu(menu: JMenu): JMenu {
  menu.add("Open").mnemonic = KeyEvent.VK_O
  menu.addSeparator()
  val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
  menu.add("Exit").accelerator = KeyStroke.getKeyStroke(KeyEvent.VK_W, modifiers)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
