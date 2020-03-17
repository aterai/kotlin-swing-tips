package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    add(JScrollPane(makeList()))
    add(makeTranslucentScrollBar(makeList()))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeList() = JTable(DefaultTableModel(30, 5)).also {
    it.setAutoCreateRowSorter(true)
    it.setAutoResizeMode(JTable.AUTO_RESIZE_OFF)
  }

  private fun makeTranslucentScrollBar(c: JTable) = object : JScrollPane(c) {
    override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        getVerticalScrollBar().setUI(OverlappedScrollBarUI())
        getHorizontalScrollBar().setUI(OverlappedScrollBarUI())
        setComponentZOrder(getVerticalScrollBar(), 0)
        setComponentZOrder(getHorizontalScrollBar(), 1)
        setComponentZOrder(getViewport(), 2)
        getVerticalScrollBar().setOpaque(false)
        getHorizontalScrollBar().setOpaque(false)
      }
      setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
      setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_ALWAYS)
      setLayout(OverlapScrollPaneLayout())
    }
  }
}

class OverlapScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    val scrollPane = parent as? JScrollPane ?: return

    val availR = scrollPane.getBounds()
    availR.setLocation(0, 0) // availR.x = availR.y = 0;

    val insets = parent.getInsets()
    availR.x = insets.left
    availR.y = insets.top
    availR.width -= insets.left + insets.right
    availR.height -= insets.top + insets.bottom

    val colHeadR = Rectangle(0, availR.y, 0, 0)
    if (colHead != null && colHead.isVisible()) {
      val colHeadHeight = minOf(availR.height, colHead.getPreferredSize().height)
      colHeadR.height = colHeadHeight
      availR.y += colHeadHeight
      availR.height -= colHeadHeight
    }

    colHeadR.width = availR.width
    colHeadR.x = availR.x
    colHead?.setBounds(colHeadR)

    val hsbR = Rectangle()
    hsbR.height = BAR_SIZE
    hsbR.width = availR.width - hsbR.height
    hsbR.x = availR.x
    hsbR.y = availR.y + availR.height - hsbR.height

    val vsbR = Rectangle()
    vsbR.width = BAR_SIZE
    vsbR.height = availR.height - vsbR.width
    vsbR.x = availR.x + availR.width - vsbR.width
    vsbR.y = availR.y

    viewport?.setBounds(availR)
    vsb?.also {
      it.setVisible(true)
      it.setBounds(vsbR)
    }
    hsb?.also {
      it.setVisible(true)
      it.setBounds(hsbR)
    }
  }

  companion object {
    private const val BAR_SIZE = 12
  }
}

class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

class OverlappedScrollBarUI : BasicScrollBarUI() {
  protected override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  protected override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  protected override fun paintTrack(g: Graphics, c: JComponent?, r: Rectangle) {
    // val g2 = g.create() as Graphics2D
    // g2.setPaint(new Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  protected override fun paintThumb(g: Graphics, c: JComponent?, r: Rectangle) {
    (c as? JScrollBar)?.takeIf { it.isEnabled() } ?: return
    val color = when {
      isDragging -> DRAGGING_COLOR
      isThumbRollover() -> ROLLOVER_COLOR
      else -> DEFAULT_COLOR
    }
    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.setPaint(color)
    g2.fillRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8)
    g2.setPaint(Color.WHITE)
    g2.drawRoundRect(r.x, r.y, r.width - 1, r.height - 1, 8, 8)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
