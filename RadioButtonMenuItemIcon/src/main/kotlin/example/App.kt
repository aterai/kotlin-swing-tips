package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.Serializable
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.UIResource

fun makeUI() = JPanel(BorderLayout()).also {
  EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
  it.add(JScrollPane(JTextArea()))
  it.preferredSize = Dimension(320, 240)
}

private fun createMenuBar(): JMenuBar {
  val menu = JMenu("RadioButtonMenuItem-Test")
  menu.add(JRadioButtonMenuItem("default", true))
  UIManager.put("RadioButtonMenuItem.checkIcon", RadioButtonMenuItemIcon1())
  menu.add(JRadioButtonMenuItem("ANTIALIASING", true))
  UIManager.put("RadioButtonMenuItem.checkIcon", RadioButtonMenuItemIcon2())
  menu.add(JRadioButtonMenuItem("fillOval", true))
  val menuBar = JMenuBar()
  menuBar.add(menu)
  return menuBar
}

private class RadioButtonMenuItemIcon1 : Icon, UIResource, Serializable {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is AbstractButton) {
      val model = c.model
      if (model.isSelected) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g2.translate(x, y)
        g2.fillRoundRect(3, 3, iconWidth - 6, iconHeight - 6, 4, 4)
        g2.dispose()
      }
    }
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12

  companion object {
    private const val serialVersionUID = 1L
  }
}

private class RadioButtonMenuItemIcon2 : Icon, UIResource, Serializable {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (c is AbstractButton) {
      val model = c.model
      if (model.isSelected) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.translate(x, y)
        g2.fillOval(2, 2, iconWidth - 5, iconHeight - 5)
        g2.dispose()
      }
    }
  }

  override fun getIconWidth() = 12

  override fun getIconHeight() = 12

  companion object {
    private const val serialVersionUID = 1L
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
