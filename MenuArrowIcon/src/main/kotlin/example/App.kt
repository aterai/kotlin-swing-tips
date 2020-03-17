package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Path2D
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    EventQueue.invokeLater {
      val menuBar = JMenuBar()
      menuBar.add(JMenu("Menu 1")).add(makeSubMenu("SubMenu 1"))
      UIManager.put("Menu.arrowIcon", ArrowIcon())
      menuBar.add(JMenu("Menu 2")).add(makeSubMenu("SubMenu 2"))
      getRootPane().setJMenuBar(menuBar)
    }
    add(JScrollPane(JTextArea()))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeSubMenu(title: String): JMenu {
    val menu = JMenu(title)
    menu.add("Item 1")
    menu.add("Item 2")
    menu.add("Item 3")
    return menu
  }
}

class ArrowIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    if (c is AbstractButton && c.getModel().isSelected()) {
      g2.setPaint(Color.WHITE)
    } else {
      g2.setPaint(Color.GRAY)
    }
    val w = getIconWidth() / 2.0
    val p = Path2D.Double()
    p.moveTo(0.0, 0.0)
    p.lineTo(w, w)
    p.lineTo(0.0, getIconHeight().toDouble())
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
