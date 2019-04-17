package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val check = JCheckBox("JCheckBox#setIcon(...)")
    check.setIcon(CheckIcon())
    add(check, BorderLayout.SOUTH)
    add(JScrollPane(JTextArea()))
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    fun createMenuBar(): JMenuBar {
      val menuBar = JMenuBar()
      val menu = JMenu("JMenu")
      menuBar.add(menu)
      menu.add(JCheckBoxMenuItem("default"))

      val defIcon = UIManager.getIcon("CheckBoxMenuItem.checkIcon")
      UIManager.put("CheckBoxMenuItem.checkIcon", CheckIcon())
      menu.add(JCheckBoxMenuItem("checkIcon test"))
      UIManager.put("CheckBoxMenuItem.checkIcon", defIcon)

      val menu2 = JMenu("JMenu2")
      val jcbmi = JCheckBoxMenuItem("setIcon")
      jcbmi.setIcon(CheckIcon())
      // jcbmi.setSelectedIcon(new CheckIcon());
      menu2.add(jcbmi)
      menuBar.add(menu)
      menuBar.add(menu2)
      return menuBar
    }
  }
}

internal class CheckIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val m = (c as? AbstractButton)?.getModel() ?: return
    val g2 = g.create() as Graphics2D
    g2.translate(x, y)
    g2.setPaint(if (m.isSelected()) Color.ORANGE else Color.GRAY)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.fillOval(1, 1, getIconWidth() - 2, getIconHeight() - 2)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setJMenuBar(MainPanel.createMenuBar())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
