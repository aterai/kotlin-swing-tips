package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.table.DefaultTableModel

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  it.add(JScrollPane(makeList()))
  it.add(makeTranslucentScrollBar(makeList()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeList() = JTable(DefaultTableModel(30, 5)).also {
  it.autoCreateRowSorter = true
  it.autoResizeMode = JTable.AUTO_RESIZE_OFF
}

private fun makeTranslucentScrollBar(c: JTable) = object : JScrollPane(c) {
  override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      getVerticalScrollBar().ui = OverlappedScrollBarUI()
      getHorizontalScrollBar().ui = OverlappedScrollBarUI()
      setComponentZOrder(getVerticalScrollBar(), 0)
      setComponentZOrder(getHorizontalScrollBar(), 1)
      setComponentZOrder(getViewport(), 2)
      getVerticalScrollBar().isOpaque = false
      getHorizontalScrollBar().isOpaque = false
    }
    setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
    layout = OverlapScrollPaneLayout()
  }
}

private class OverlapScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return
    val availR = SwingUtilities.calculateInnerArea(scrollPane, null)

    val colHeadR = Rectangle(0, availR.y, 0, 0)
    if (colHead != null && colHead.isVisible) {
      val colHeadHeight = minOf(availR.height, colHead.preferredSize.height)
      colHeadR.height = colHeadHeight
      availR.y += colHeadHeight
      availR.height -= colHeadHeight
      colHeadR.width = availR.width
      colHeadR.x = availR.x
      colHead.bounds = colHeadR
    }
    viewport?.bounds = availR
    vsb?.also {
      it.setLocation(availR.x + availR.width - BAR_SIZE, availR.y)
      it.setSize(BAR_SIZE, availR.height - BAR_SIZE)
    }
    hsb?.also {
      it.setLocation(availR.x, availR.y + availR.height - BAR_SIZE)
      it.setSize(availR.width - BAR_SIZE, BAR_SIZE)
    }
  }

  companion object {
    private const val BAR_SIZE = 12
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class OverlappedScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    // val g2 = g.create() as? Graphics2D ?: return
    // g2.setPaint(new Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    (c as? JScrollBar)?.takeIf { it.isEnabled } ?: return
    val color = when {
      isDragging -> DRAGGING_COLOR
      isThumbRollover -> ROLLOVER_COLOR
      else -> DEFAULT_COLOR
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = color
    g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.paint = Color.WHITE
    // g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8)
    g2.dispose()
  }

  companion object {
    private val DEFAULT_COLOR = Color(100, 180, 255, 100)
    private val DRAGGING_COLOR = Color(100, 180, 200, 100)
    private val ROLLOVER_COLOR = Color(100, 180, 220, 100)
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
