package example

import java.awt.*
import javax.imageio.ImageIO
import javax.swing.*

fun makeUI(): Component {
  val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)
  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  listOf(
    "example/wi0009-16.png",
    "example/wi0054-16.png",
    "example/wi0062-16.png",
    "example/wi0063-16.png",
    "example/wi0124-16.png",
    "example/wi0126-16.png",
  ).forEach { path ->
    val icon = makeIcon(path)
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

private fun makeIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read)?.let(::ImageIcon)
    ?: UIManager.getIcon("html.missingImage")
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
