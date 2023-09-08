package example

import java.awt.*
import java.awt.geom.GeneralPath
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

fun makeUI(): Component {
  val tabs = object : JTabbedPane() {
    override fun updateUI() {
      super.updateUI()
      UIManager.put("TabbedPane.highlight", Color.GRAY)
      tabLayoutPolicy = SCROLL_TAB_LAYOUT
      setUI(IsoscelesTrapezoidTabbedPaneUI())
    }
  }
  tabs.addTab("JTextArea", JScrollPane(JTextArea()))
  tabs.addTab("JTree", JScrollPane(JTree()))
  tabs.addTab("JButton", JButton("button"))
  tabs.addTab("JSplitPane", JSplitPane())
  tabs.preferredSize = Dimension(320, 240)
  return tabs
}

private class IsoscelesTrapezoidTabbedPaneUI : BasicTabbedPaneUI() {
  private val selectedTabColor = UIManager.getColor("TabbedPane.selected")

  override fun paintTabArea(g: Graphics, tabPlacement: Int, selectedIndex: Int) {
    val tabCount = tabPane.tabCount
    val iconRect = Rectangle()
    val textRect = Rectangle()
    val clipRect = g.clipBounds
    // copied from BasicTabbedPaneUI#paintTabArea(...)
    for (i in runCount - 1 downTo 0) {
      val start = tabRuns[i]
      val end = getTabRunsEnd(i, tabCount)
      // https://stackoverflow.com/questions/41566659/tabs-rendering-order-in-custom-jtabbedpane
      for (j in end downTo start) {
        if (j != selectedIndex && rects[j].intersects(clipRect)) {
          paintTab(g, tabPlacement, rects, j, iconRect, textRect)
        }
      }
    }
    if (selectedIndex >= 0 && rects[selectedIndex].intersects(clipRect)) {
      paintTab(g, tabPlacement, rects, selectedIndex, iconRect, textRect)
    }
  }

  private fun getTabRunsEnd(i: Int, tabCount: Int): Int {
    val next = tabRuns[if (i == runCount - 1) 0 else i + 1]
    return if (next == 0) tabCount - 1 else next - 1
  }

  override fun paintTabBorder(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    // Do nothing
  }

  override fun paintFocusIndicator(
    g: Graphics,
    tabPlacement: Int,
    rects: Array<Rectangle>,
    tabIndex: Int,
    iconRect: Rectangle,
    textRect: Rectangle,
    isSelected: Boolean,
  ) {
    // Do nothing
  }

  override fun paintContentBorderTopEdge(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
  ) {
    super.paintContentBorderTopEdge(g, tabPlacement, selectedIndex, x, y, w, h)
    val selRect = getTabBounds(selectedIndex, calcRect)
    val g2 = g.create() as? Graphics2D ?: return
    g2.color = selectedTabColor
    g2.drawLine(selRect.x - ADJ2 + 1, y, selRect.x + selRect.width + ADJ2 - 1, y)
    g2.dispose()
  }

  override fun paintTabBackground(
    g: Graphics,
    tabPlacement: Int,
    tabIndex: Int,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
    isSelected: Boolean,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val clipRect = g2.clipBounds
    clipRect.grow(ADJ2 + 1, 0)
    g2.clip = clipRect
    val textShiftOffset = if (isSelected) 0 else 1
    val trapezoid = GeneralPath()
    trapezoid.moveTo((x - ADJ2).toFloat(), (y + h).toFloat())
    trapezoid.lineTo((x + ADJ2).toFloat(), (y + textShiftOffset).toFloat())
    trapezoid.lineTo((x + w - ADJ2).toFloat(), (y + textShiftOffset).toFloat())
    trapezoid.lineTo((x + w + ADJ2).toFloat(), (y + h).toFloat())
    g2.color = if (isSelected) selectedTabColor else TAB_BACKGROUND
    g2.fill(trapezoid)
    g2.color = TAB_BORDER
    g2.draw(trapezoid)
    g2.dispose()
  }

  companion object {
    private const val ADJ2 = 3
    private val TAB_BACKGROUND = Color.LIGHT_GRAY
    private val TAB_BORDER = Color.GRAY
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
