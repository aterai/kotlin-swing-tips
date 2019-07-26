package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTabbedPaneUI

class MainPanel : JPanel(BorderLayout()) {
  private val comboBox = JComboBox<TabPlacements>(TabPlacements.values())
  private val tabbedPane = JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT)

  init {
    comboBox.addItemListener { e ->
      val item = e.getItem()
      if (e.getStateChange() == ItemEvent.SELECTED && item is TabPlacements) {
        tabbedPane.setTabPlacement(item.tabPlacement)
      }
    }
    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(JLabel("TabPlacement: "))
    box.add(Box.createHorizontalStrut(2))
    box.add(comboBox)
    box.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))

    if (tabbedPane.getUI() is WindowsTabbedPaneUI) {
      tabbedPane.setUI(WindowsTabHeightTabbedPaneUI())
    } else {
      tabbedPane.setUI(BasicTabHeightTabbedPaneUI())
    }
    tabbedPane.addTab("00000", JLabel("aaaaaaaaaaa"))
    tabbedPane.addTab("111112", JLabel("bbbbbbbbbbbbbbbb"))
    tabbedPane.addTab("22222232", JScrollPane(JTree()))
    tabbedPane.addTab("3333333333", JSplitPane())
    add(tabbedPane)
    add(box, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

internal enum class TabPlacements private constructor(val tabPlacement: Int) {
  TOP(JTabbedPane.TOP),
  BOTTOM(JTabbedPane.BOTTOM),
  LEFT(JTabbedPane.LEFT),
  RIGHT(JTabbedPane.RIGHT)
}

internal class WindowsTabHeightTabbedPaneUI : WindowsTabbedPaneUI() {
  protected override fun calculateTabHeight(tabPlacement: Int, tabIndex: Int, fontHeight: Int) = TAB_AREA_HEIGHT

  protected override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle
  ) {
    val isTopOrBottom = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM
    if (isTopOrBottom && tabPane.getSelectedIndex() != tabIndex) {
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

internal class BasicTabHeightTabbedPaneUI : BasicTabbedPaneUI() {
  protected override fun calculateTabHeight(tabPlacement: Int, tabIndex: Int, fontHeight: Int) = TAB_AREA_HEIGHT

  protected override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle
  ) {
    val isTopOrBottom = tabPlacement == SwingConstants.TOP || tabPlacement == SwingConstants.BOTTOM
    if (isTopOrBottom && tabPane.getSelectedIndex() != tabIndex) {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
