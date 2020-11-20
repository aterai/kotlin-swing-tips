package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabs = CloseableTabbedPane()
  tabs.componentPopupMenu = TabbedPanePopupMenu()
  tabs.addTab("JLabel", JLabel("JDK 6"))
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.preferredSize = Dimension(320, 240)
  return tabs
}

private class CloseableTabbedPane : JTabbedPane() {
  override fun addTab(title: String, content: Component) {
    val tab = JPanel(BorderLayout())
    tab.isOpaque = false
    val label = JLabel(title)
    label.border = BorderFactory.createEmptyBorder(0, 0, 0, 4)
    val button = JButton(CLOSE_ICON)
    button.border = BorderFactory.createEmptyBorder()
    button.addActionListener { removeTabAt(indexOfComponent(content)) }
    tab.add(label, BorderLayout.WEST)
    tab.add(button, BorderLayout.EAST)
    tab.border = BorderFactory.createEmptyBorder(2, 1, 1, 1)
    super.addTab(title, content)
    setTabComponentAt(tabCount - 1, tab)
  }

  companion object {
    private val CLOSE_ICON = CloseTabIcon()
  }
}

private class CloseTabIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = Color.BLACK
    g2.drawLine(4, 4, 11, 11)
    g2.drawLine(4, 5, 10, 11)
    g2.drawLine(5, 4, 11, 10)
    g2.drawLine(11, 4, 4, 11)
    g2.drawLine(11, 5, 5, 11)
    g2.drawLine(10, 4, 4, 10)
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 16
}

private class TabbedPanePopupMenu : JPopupMenu() {
  private var counter = 0
  private val closeAll: JMenuItem

  init {
    add("Add").addActionListener {
      (invoker as? JTabbedPane)?.also {
        it.addTab("Title$counter", JLabel("Tab$counter"))
        it.selectedIndex = it.tabCount - 1
      }
      counter++
    }
    addSeparator()
    closeAll = add("Close All")
    closeAll.addActionListener { (invoker as? JTabbedPane)?.removeAll() }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTabbedPane) {
      closeAll.isEnabled = c.tabCount > 0
      super.show(c, x, y)
    }
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
