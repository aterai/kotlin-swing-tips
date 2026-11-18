package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Path2D
import javax.swing.*
import javax.swing.plaf.basic.BasicTabbedPaneUI

fun createUI(): Component {
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

  override fun paintTabArea(
    g: Graphics,
    tabPlacement: Int,
    selectedIndex: Int,
  ) {
    val tabCount = tabPane.tabCount
    val iconRect = Rectangle()
    val textRect = Rectangle()
    val clipRect = g.clipBounds
    // copied from BasicTabbedPaneUI#paintTabArea(...)
    for (i in runCount - 1 downTo 0) {
      val start = tabRuns[i]
      val end = getTabRunsEnd(i, tabCount)
      // Paint the tabs from right to left so that the left tab overlaps the right one
      // https://stackoverflow.com/questions/41566659/tabs-rendering-order-in-custom-jtabbedpane
      for (j in end downTo start) {
        if (j != selectedIndex && intersectsTrapezoid(rects[j], clipRect)) {
          paintTab(g, tabPlacement, rects, j, iconRect, textRect)
        }
      }
    }
    if (selectedIndex >= 0 && intersectsTrapezoid(rects[selectedIndex], clipRect)) {
      paintTab(g, tabPlacement, rects, selectedIndex, iconRect, textRect)
    }
  }

  private fun getTabRunsEnd(
    i: Int,
    tabCount: Int,
  ): Int {
    val next = tabRuns[(i + 1) % runCount]
    return if (next == 0) tabCount - 1 else next - 1
  }

  // The trapezoid sticks out of the tab bounds by SLANT_WIDTH on both sides,
  // so a tab must also be repainted when only its protruding part is in the clip
  // (e.g. when only the adjacent tab is repainted by a focus change).
  private fun intersectsTrapezoid(
    tabRect: Rectangle,
    clipRect: Rectangle,
  ): Boolean {
    val r = Rectangle(tabRect)
    r.grow(SLANT_WIDTH, 0)
    return r.intersects(clipRect)
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
    if (tabPlacement != TOP || selectedIndex < 0) {
      return
    }
    val selRect = getTabBounds(selectedIndex, calcRect)
    // Same condition as BasicTabbedPaneUI: skip if the selected tab is
    // scrolled out of view (SCROLL_TAB_LAYOUT)
    if (selRect.x in x..x + w) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.color = selectedTabColor
      val x1 = selRect.x - SLANT_WIDTH + 1
      val x2 = selRect.x + selRect.width + SLANT_WIDTH - 1
      g2.drawLine(x1, y, x2, y)
      g2.dispose()
    }
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
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    // The trapezoid sticks out of the tab bounds, so expand the clipping area
    val clipRect = g2.clipBounds
    clipRect.grow(SLANT_WIDTH + 1, 0)
    g2.clip = clipRect
    // Unselected tabs are 1px lower than the selected tab
    val top = if (isSelected) 0.0 else 1.0
    val trapezoid = Path2D.Float()
    trapezoid.moveTo(-SLANT_WIDTH.toDouble(), h.toDouble())
    trapezoid.lineTo(SLANT_WIDTH.toDouble(), top)
    trapezoid.lineTo((w - SLANT_WIDTH).toDouble(), top)
    trapezoid.lineTo((w + SLANT_WIDTH).toDouble(), h.toDouble())
    trapezoid.closePath()
    val tx = x.toDouble()
    val ty = y.toDouble()
    trapezoid.transform(AffineTransform.getTranslateInstance(tx, ty))
    g2.color = if (isSelected) selectedTabColor else TAB_BACKGROUND
    g2.fill(trapezoid)
    g2.color = TAB_BORDER
    g2.draw(trapezoid)
    g2.dispose()
  }

  companion object {
    // horizontal width of the slanted sides of the trapezoid
    private const val SLANT_WIDTH = 3
    private val TAB_BACKGROUND = Color.LIGHT_GRAY
    private val TAB_BORDER = Color.GRAY
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
