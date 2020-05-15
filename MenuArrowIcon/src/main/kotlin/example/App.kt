package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val p = JPanel(BorderLayout())
  EventQueue.invokeLater {
    val menuBar = JMenuBar()
    menuBar.add(JMenu("Menu 1")).add(makeSubMenu("SubMenu 1"))
    UIManager.put("Menu.arrowIcon", ArrowIcon())
    menuBar.add(JMenu("Menu 2")).add(makeSubMenu("SubMenu 2"))
    p.rootPane.jMenuBar = menuBar
  }
  p.add(JScrollPane(JTextArea()))
  p.preferredSize = Dimension(320, 240)
  return p
}

private fun makeSubMenu(title: String): JMenu {
  val menu = JMenu(title)
  menu.add("Item 1")
  menu.add("Item 2")
  menu.add("Item 3")
  return menu
}

private class ArrowIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    if (c is AbstractButton && c.model.isSelected) {
      g2.paint = Color.WHITE
    } else {
      g2.paint = Color.GRAY
    }
    val w = iconWidth / 2.0
    val p = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w, w)
    p.lineTo(0.0, iconHeight.toDouble())
    p.closePath()
    g2.translate(x, y)
    g2.fill(p)
    g2.dispose()
  }

  override fun getIconWidth() = 8

  override fun getIconHeight() = 8
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
