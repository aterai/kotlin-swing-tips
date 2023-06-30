package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

private val comboBox = JComboBox(TabPlacements.values())
private val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)

fun makeUI(): Component {
  comboBox.addItemListener { e ->
    val item = e.item
    if (e.stateChange == ItemEvent.SELECTED && item is TabPlacements) {
      tabbedPane.tabPlacement = item.tabPlacement
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(JLabel("TabPlacement: "))
  box.add(Box.createHorizontalStrut(2))
  box.add(comboBox)
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  if (tabbedPane.ui is WindowsTabbedPaneUI) {
    tabbedPane.ui = WindowsTabHeightTabbedPaneUI()
  } else {
    tabbedPane.ui = BasicTabHeightTabbedPaneUI()
  }
  tabbedPane.addTab("00000", JLabel("0000000000"))
  tabbedPane.addTab("111112", JLabel("111111111111"))
  tabbedPane.addTab("22222232", JScrollPane(JTree()))
  tabbedPane.addTab("3333333333", JSplitPane())

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class TabPlacements(val tabPlacement: Int) {
  TOP(JTabbedPane.TOP),
  BOTTOM(JTabbedPane.BOTTOM),
  LEFT(JTabbedPane.LEFT),
  RIGHT(JTabbedPane.RIGHT)
}

private class WindowsTabHeightTabbedPaneUI : WindowsTabbedPaneUI() {
  override fun calculateTabHeight(
    tabPlacement: Int,
    tabIndex: Int,
    fontHeight: Int
  ) = TAB_AREA_HEIGHT

  override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle
  ) {
    val isTopOrBottom = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM
    if (isTopOrBottom && tabPane.selectedIndex != tabIndex) {
      val tabHeight = TAB_AREA_HEIGHT / 2 + 3
      rects[tabIndex].height = tabHeight
      if (tabPlacement == JTabbedPane.TOP) {
        rects[tabIndex].y = TAB_AREA_HEIGHT - tabHeight + 3
      }
    }
    super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect)
  }

  companion object {
    private const val TAB_AREA_HEIGHT = 32
  }
}

private class BasicTabHeightTabbedPaneUI : BasicTabbedPaneUI() {
  override fun calculateTabHeight(
    tabPlacement: Int,
    tabIndex: Int,
    fontHeight: Int
  ) = TAB_AREA_HEIGHT

  override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle
  ) {
    val isTopOrBottom = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM
    if (isTopOrBottom && tabPane.selectedIndex != tabIndex) {
      val tabHeight = TAB_AREA_HEIGHT / 2 + 3
      rects[tabIndex].height = tabHeight
      if (tabPlacement == JTabbedPane.TOP) {
        rects[tabIndex].y = TAB_AREA_HEIGHT - tabHeight + 3
      }
    }
    super.paintTab(g, tabPlacement, rects, tabIndex, iconRect, textRect)
  }

  companion object {
    private const val TAB_AREA_HEIGHT = 32
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
