package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  private val icons = listOf(
    "wi0009-16.png", "wi0054-16.png", "wi0062-16.png",
    "wi0063-16.png", "wi0124-16.png", "wi0126-16.png")

  init {
    val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
    icons.forEach { path ->
      val icon = ImageIcon(javaClass.getResource(path))
      val label = ShrinkLabel(path, icon)
      tabbedPane.addTab(path, icon, JLabel(path), path)
      tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, label)
    }
    updateTabWidth(tabbedPane)
    tabbedPane.addChangeListener { e ->
      (e.getSource() as? JTabbedPane)?.also {
        updateTabWidth(it)
      }
    }
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }

  private fun updateTabWidth(tabs: JTabbedPane) {
    when (tabs.getTabPlacement()) {
      JTabbedPane.TOP, JTabbedPane.BOTTOM -> {
        val idx = tabs.getSelectedIndex()
        for (i in 0 until tabs.getTabCount()) {
          (tabs.getTabComponentAt(i) as? ShrinkLabel)?.also {
            it.isSelected = i == idx
          }
        }
      }
    }
  }
}

internal class ShrinkLabel(title: String, icon: Icon) : JLabel(title, icon, SwingConstants.LEFT) {
  var isSelected = false

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    if (!isSelected) {
      it.width = getIcon()?.getIconWidth() ?: it.width
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
