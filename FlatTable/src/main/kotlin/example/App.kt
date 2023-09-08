package example

import java.awt.*
import javax.swing.*
import javax.swing.border.EmptyBorder
import javax.swing.plaf.basic.BasicScrollBarUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val table = object : JTable(10, 3) {
    private val border = CellBorder(2, 2, 1, 2)

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      (c as? JTextField)?.border = border
      border.setStartCell(column == 0)
      return c
    }
  }
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.showVerticalLines = false
  table.gridColor = Color.ORANGE
  table.selectionForeground = Color.BLACK
  table.selectionBackground = Color(0x64_AA_EE_FF, true)
  table.intercellSpacing = Dimension(0, 1)
  table.border = BorderFactory.createEmptyBorder()

  val border = CellBorder(2, 2, 1, 2)
  val r = DefaultTableCellRenderer()
  table.setDefaultRenderer(Any::class.java) { tbl, value, isSelected, hasFocus, row, column ->
    r.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column).also {
      border.setStartCell(column == 0)
      (it as? JComponent)?.border = border
    }
  }

  val header = table.tableHeader
  header.border = BorderFactory.createEmptyBorder()
  val hr = DefaultTableCellRenderer()
  header.setDefaultRenderer { tbl, value, isSelected, hasFocus, row, column ->
    hr.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column).also {
      if (it is JLabel) {
        border.setStartCell(column == 0)
        it.horizontalAlignment = SwingConstants.CENTER
        it.border = border
        it.background = table.gridColor
      }
    }
  }

  val scroll = makeTranslucentScrollBar(table)
  scroll.border = BorderFactory.createLineBorder(table.gridColor)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.border = BorderFactory.createLineBorder(Color.LIGHT_GRAY, 20)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTranslucentScrollBar(c: JTable): JScrollPane {
  return object : JScrollPane(c) {
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
}

private class CellBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
) : EmptyBorder(top, left, bottom, right) {
  private var startCell = false

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val c1 = SwingUtilities.getAncestorOfClass(JTable::class.java, c)
    val c2 = SwingUtilities.getAncestorOfClass(JTableHeader::class.java, c)
    g2.paint = when {
      c1 is JTable -> c1.gridColor
      c2 is JTableHeader && c2.table != null -> c2.table.gridColor
      else -> Color.RED
    }
    if (!isStartCell()) {
      g2.drawLine(0, 0, 0, h - 1) // Left line
    }
    g2.dispose()
  }

  fun isStartCell() = startCell

  fun setStartCell(b: Boolean) {
    startCell = b
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
      it.setSize(BAR_SIZE, availR.height)
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
    // g2.setPaint(Color(100, 100, 100, 100))
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
    g2.fill(r)
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
