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
    icons.forEach { s ->
      val icon = ImageIcon(javaClass.getResource(s))
      val label = ShrinkLabel(s, icon)
      tabbedPane.addTab(s, icon, JLabel(s), s)
      tabbedPane.setTabComponentAt(tabbedPane.getTabCount() - 1, label)
    }
    updateTabWidth(tabbedPane)
    tabbedPane.addChangeListener { e -> updateTabWidth(e.getSource() as JTabbedPane) }
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }

  fun updateTabWidth(tabs: JTabbedPane) {
    when (tabs.getTabPlacement()) {
      JTabbedPane.TOP, JTabbedPane.BOTTOM -> {
        val sidx = tabs.getSelectedIndex()
        for (i in 0 until tabs.getTabCount()) {
          (tabs.getTabComponentAt(i) as? ShrinkLabel)?.also {
            it.isSelected = i == sidx
          }
        }
      }
    }
  }
}

internal class ShrinkLabel(title: String, icon: Icon) : JLabel(title, icon, SwingConstants.LEFT) {
  var isSelected = false

  override fun getPreferredSize() = super.getPreferredSize().apply {
    if (!isSelected) {
      width = getIcon()?.getIconWidth() ?: width
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
