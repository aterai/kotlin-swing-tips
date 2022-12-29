package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.DefaultMenuLayout

fun makeUI(): Component {
  val mb = JMenuBar()
  makeMenu(mb.add(JMenu("Default")))

  makeMenu(mb.add(JMenu("BoxHStrut"))).also {
    it.add(Box.createHorizontalStrut(200))
  }

  makeMenu(mb.add(JMenu("Override"))).also { m ->
    val item = object : JMenuItem("PreferredSize") {
      override fun getPreferredSize() = super.getPreferredSize()?.also {
        it.width = 200
      }
    }
    m.add(item)
  }

  makeMenu(mb.add(JMenu("Layout"))).also {
    val popup = it.popupMenu
    popup.layout = object : DefaultMenuLayout(popup, BoxLayout.Y_AXIS) {
      override fun preferredLayoutSize(target: Container): Dimension {
        val d = super.preferredLayoutSize(target)
        d.width = 200.coerceAtLeast(d.width)
        return d
      }
    }
  }

  makeMenu(mb.add(JMenu("Html"))).also {
    val item = it.add("<html><table cellpadding='0' cellspacing='0' style='width:200'>Table")
    item.mnemonic = KeyEvent.VK_T
  }

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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
