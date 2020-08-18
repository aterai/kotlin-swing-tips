package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  val cl = Thread.currentThread().contextClassLoader
  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  val icons = listOf(
    "wi0009-16.png",
    "wi0054-16.png",
    "wi0062-16.png",
    "wi0063-16.png",
    "wi0124-16.png",
    "wi0126-16.png"
  )
  icons.forEach { path ->
    val icon = ImageIcon(cl.getResource("example/$path"))
    val label = ShrinkLabel(path, icon)
    tabbedPane.addTab(path, icon, JLabel(path), path)
    tabbedPane.setTabComponentAt(tabbedPane.tabCount - 1, label)
  }
  updateTabWidth(tabbedPane)
  tabbedPane.addChangeListener { e ->
    (e.source as? JTabbedPane)?.also {
      updateTabWidth(it)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateTabWidth(tabs: JTabbedPane) {
  when (tabs.tabPlacement) {
    JTabbedPane.TOP, JTabbedPane.BOTTOM -> {
      val idx = tabs.selectedIndex
      for (i in 0 until tabs.tabCount) {
        (tabs.getTabComponentAt(i) as? ShrinkLabel)?.isSelected = i == idx
      }
    }
  }
}

private class ShrinkLabel(title: String, icon: Icon) : JLabel(title, icon, SwingConstants.LEFT) {
  var isSelected = false

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    if (!isSelected) {
      it.width = icon?.iconWidth ?: it.width
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
