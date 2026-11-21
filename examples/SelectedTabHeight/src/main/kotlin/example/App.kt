package example

import com.sun.java.swing.plaf.windows.WindowsTabbedPaneUI
import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

fun createUI(): Component {
  val tabbedPane = object : JTabbedPane(
    TOP,
    SCROLL_TAB_LAYOUT,
  ) {
    override fun updateUI() {
      super.updateUI()
      val ui2 = if (ui is WindowsTabbedPaneUI) {
        WindowsTabHeightTabbedPaneUI()
      } else {
        BasicTabHeightTabbedPaneUI()
      }
      setUI(ui2)
    }
  }
  tabbedPane.addTab("00000", JLabel("0000000000"))
  tabbedPane.addTab("111112", JLabel("111111111111"))
  tabbedPane.addTab("22222232", JScrollPane(JTree()))
  tabbedPane.addTab("3333333333", JSplitPane())

  val comboBox = JComboBox(TabPlacements.entries.toTypedArray())
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

  return JPanel(BorderLayout()).also {
    it.add(tabbedPane)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class TabPlacements(
  val tabPlacement: Int,
) {
  TOP(JTabbedPane.TOP),
  BOTTOM(JTabbedPane.BOTTOM),
  LEFT(JTabbedPane.LEFT),
  RIGHT(JTabbedPane.RIGHT),
}

private const val TAB_AREA_HEIGHT = 32
private const val LOW_TAB_HEIGHT = TAB_AREA_HEIGHT / 2 + 3

// Returns the tab rectangles used only for painting: an unselected tab of the
// TOP/BOTTOM placement is shortened (and moved down to the bottom of the tab area
// in the TOP placement). A copy is returned so that the layout rectangles
// shared with hit testing and scrolling are not modified while painting.
private fun getPaintRects(
  tabPane: JTabbedPane,
  tabPlacement: Int,
  rects: Array<Rectangle>,
  tabIndex: Int,
): Array<Rectangle> {
  val isHorizontal = tabPlacement == SwingConstants.TOP ||
    tabPlacement == SwingConstants.BOTTOM
  return if (isHorizontal && tabPane.selectedIndex != tabIndex) {
    val r = Rectangle(rects[tabIndex])
    if (tabPlacement == SwingConstants.TOP) {
      r.y += r.height - LOW_TAB_HEIGHT
    }
    r.height = LOW_TAB_HEIGHT
    rects.copyOf().also { it[tabIndex] = r }
  } else {
    rects
  }
}

private class WindowsTabHeightTabbedPaneUI : WindowsTabbedPaneUI() {
  override fun calculateTabHeight(
    tabPlacement: Int,
    tabIndex: Int,
    fontHeight: Int,
  ) = TAB_AREA_HEIGHT

  override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle,
  ) {
    val paintRects = getPaintRects(tabPane, tabPlacement, rects, tabIndex)
    super.paintTab(g, tabPlacement, paintRects, tabIndex, iconRect, textRect)
  }
}

private class BasicTabHeightTabbedPaneUI : BasicTabbedPaneUI() {
  override fun calculateTabHeight(
    tabPlacement: Int,
    tabIndex: Int,
    fontHeight: Int,
  ) = TAB_AREA_HEIGHT

  override fun paintTab(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle,
  ) {
    val paintRects = getPaintRects(tabPane, tabPlacement, rects, tabIndex)
    super.paintTab(g, tabPlacement, paintRects, tabIndex, iconRect, textRect)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
