package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTabbedPaneUI

private val SELECTED_BG = Color(255, 150, 0)
private val UNSELECTED_BG = Color(255, 50, 0)

fun makeUI(): Component {
  UIManager.put("TabbedPane.tabInsets", Insets(5, 10, 5, 10))
  // UIManager.put("TabbedPane.selectedTabPadInsets", new Insets(2, 3, 2, 2))
  UIManager.put("TabbedPane.contentBorderInsets", Insets(5, 5, 5, 5))
  UIManager.put("TabbedPane.tabAreaInsets", Insets(0, 0, 0, 0))

  UIManager.put("TabbedPane.selectedLabelShift", 0)
  UIManager.put("TabbedPane.labelShift", 0)

  // UIManager.put("TabbedPane.foreground", Color.WHITE)
  // UIManager.put("TabbedPane.selectedForeground", Color.WHITE)
  // UIManager.put("TabbedPane.unselectedBackground", UNSELECTED_BG)
  // UIManager.put("TabbedPane.tabAreaBackground", UNSELECTED_BG)

  val tabs = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      setUI(FlatTabbedPaneUI())
      isOpaque = true
      foreground = Color.WHITE
      background = UNSELECTED_BG
      setTabPlacement(SwingConstants.LEFT)
      tabLayoutPolicy = SCROLL_TAB_LAYOUT
    }
  }

  // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
  tabs.addTab("A", makeIcon("example/wi0009-32.png"), JScrollPane(JTree()))
  tabs.addTab("B", makeIcon("example/wi0054-32.png"), JScrollPane(JTextArea()))
  tabs.addTab("C", makeIcon("example/wi0062-32.png"), JScrollPane(JTree()))
  tabs.addTab("D", makeIcon("example/wi0063-32.png"), JScrollPane(JTextArea()))

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeIcon(path: String): Icon {
  val url = Thread.currentThread().contextClassLoader.getResource(path)
  return url?.openStream()?.use(ImageIO::read)?.let(::ImageIcon)
    ?: UIManager.getIcon("OptionPane.errorIcon")
}

private class FlatTabbedPaneUI : BasicTabbedPaneUI() {
  override fun paintFocusIndicator(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle?,
    textRect: Rectangle?,
    isSelected: Boolean
  ) { /* Do not paint anything */ }

  override fun paintTabBorder(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean
  ) { /* Do not paint anything */ }

  override fun paintTabBackground(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean
  ) {
    g.color = if (isSelected) SELECTED_BG else UNSELECTED_BG
    g.fillRect(x, y, w, h)
  }

  override fun paintContentBorderTopEdge(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int
  ) {
    paintContentBorder(g, x, y, w, h)
  }

  override fun paintContentBorderRightEdge(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int
  ) {
    paintContentBorder(g, x, y, w, h)
  }

  override fun paintContentBorderBottomEdge(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int
  ) {
    paintContentBorder(g, x, y, w, h)
  }

  override fun paintContentBorderLeftEdge(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int
  ) {
    paintContentBorder(g, x, y, w, h)
  }

  private fun paintContentBorder(g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    g.color = SELECTED_BG
    g.fillRect(x, y, w, h)
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
