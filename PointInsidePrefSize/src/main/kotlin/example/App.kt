package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URL
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private val columnNames = arrayOf("No.", "Name", "URL")
private val model = object : DefaultTableModel(columnNames, 0) {
  override fun getColumnClass(column: Int) = when (column) {
    0 -> Number::class.java
    1 -> String::class.java
    2 -> URL::class.java
    else -> super.getColumnClass(column)
  }

  override fun isCellEditable(row: Int, col: Int) = false
}

fun makeUI(): Component {
  model.addRow(arrayOf(0, "FrontPage", makeUrl("https://ateraimemo.com/")))
  model.addRow(arrayOf(1, "Java Swing Tips", makeUrl("https://ateraimemo.com/Swing.html")))
  model.addRow(arrayOf(2, "Example", makeUrl("http://www.example.com/")))
  model.addRow(arrayOf(3, "Example.jp", makeUrl("http://www.example.jp/")))

  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(tcr, row, column)
      c.foreground = foreground
      c.background = if (row % 2 == 0) evenColor else background
      return c
    }
  }
  table.rowSelectionAllowed = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.intercellSpacing = Dimension()
  table.setShowGrid(false)
  table.putClientProperty("terminateEditOnFocusLost", true)
  table.autoCreateRowSorter = true

  var col = table.columnModel.getColumn(0)
  col.minWidth = 50
  col.maxWidth = 50
  col.resizable = false

  val renderer = UrlRenderer()
  table.setDefaultRenderer(URL::class.java, renderer)
  table.addMouseListener(renderer)
  table.addMouseMotionListener(renderer)

  col = table.columnModel.getColumn(1)
  col.preferredWidth = 1000

  col = table.columnModel.getColumn(2)
  // col.setCellRenderer(renderer)
  col.preferredWidth = 2000

  val scrollPane = JScrollPane(table)
  scrollPane.viewport.background = Color.WHITE
  return JPanel(BorderLayout()).also {
    it.add(scrollPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeUrl(spec: String) = runCatching { URL(spec) }.getOrNull()

private class UrlRenderer : MouseAdapter(), TableCellRenderer {
  private val renderer = DefaultTableCellRenderer()
  private var viewRowIndex = -1
  private var viewColumnIndex = -1
  private var isRollover = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column)
    if (c is JLabel) {
      val cm = table.columnModel
      val i = c.insets
      CELL_RECT.x = i.left
      CELL_RECT.y = i.top
      CELL_RECT.width = cm.getColumn(column).width - cm.columnMargin - i.right - CELL_RECT.x
      CELL_RECT.height = table.getRowHeight(row) - table.rowMargin - i.bottom - CELL_RECT.y
      ICON_RECT.setBounds(0, 0, 0, 0)
      TEXT_RECT.setBounds(0, 0, 0, 0)

      val str = SwingUtilities.layoutCompoundLabel(
        c,
        c.getFontMetrics(c.font),
        value?.toString() ?: "",
        c.icon,
        c.verticalAlignment,
        c.horizontalAlignment,
        c.verticalTextPosition,
        c.horizontalTextPosition,
        CELL_RECT,
        ICON_RECT,
        TEXT_RECT,
        c.iconTextGap,
      )
      c.text = if (isRolloverCell(table, row, column)) "<html><u><font color='blue'>$str" else str
    }
    return c
  }

  private fun isRolloverCell(table: JTable, row: Int, column: Int) =
    !table.isEditing && viewRowIndex == row && viewColumnIndex == column && isRollover

  override fun mouseMoved(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val pt = e.point
    val prevRow = viewRowIndex
    val prevCol = viewColumnIndex
    val prevRollover = isRollover
    viewRowIndex = table.rowAtPoint(pt)
    viewColumnIndex = table.columnAtPoint(pt)
    isRollover = isUrlColumn(table, viewColumnIndex) && pointInsidePrefSize(table, pt)
    val rollover = isRollover == prevRollover
    val isSameCell = viewRowIndex == prevRow && viewColumnIndex == prevCol && rollover
    val isNotRollover = !isRollover && !prevRollover
    if (isSameCell || isNotRollover) {
      return
    }
    val repaintRect = if (isRollover) {
      val r = table.getCellRect(viewRowIndex, viewColumnIndex, false)
      if (prevRollover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
    } else { // if (prevRollover) {
      table.getCellRect(prevRow, prevCol, false)
    }
    table.repaint(repaintRect)
  }

  override fun mouseExited(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    if (isUrlColumn(table, viewColumnIndex)) {
      table.repaint(table.getCellRect(viewRowIndex, viewColumnIndex, false))
      viewRowIndex = -1
      viewColumnIndex = -1
      isRollover = false
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val pt = e.point
    val col = table.columnAtPoint(pt)
    if (isUrlColumn(table, col) && pointInsidePrefSize(table, pt)) {
      val row = table.rowAtPoint(pt)
      val url = table.getValueAt(row, col) as? URL ?: return
      if (Desktop.isDesktopSupported()) {
        runCatching {
          Desktop.getDesktop().browse(url.toURI())
        }.onFailure {
          it.printStackTrace()
        }
      }
    }
  }

  private fun isUrlColumn(tbl: JTable, col: Int) =
    col >= 0 && tbl.getColumnClass(col) == URL::class.java

  // @see SwingUtilities2.pointOutsidePrefSize(...)
  private fun pointInsidePrefSize(table: JTable, p: Point): Boolean {
    val row = table.rowAtPoint(p)
    val col = table.columnAtPoint(p)
    val tcr = table.getCellRenderer(row, col)
    val value = table.getValueAt(row, col)
    val cell = tcr.getTableCellRendererComponent(table, value, false, false, row, col)
    val itemSize = cell.preferredSize
    val cellBounds = table.getCellRect(row, col, false).also {
      val i = (cell as? JComponent)?.insets ?: Insets(0, 0, 0, 0)
      it.width = itemSize.width - i.right - i.left
      it.translate(i.left, i.top)
    }
    return cellBounds.contains(p)
  }

  companion object {
    private val CELL_RECT = Rectangle()
    private val ICON_RECT = Rectangle()
    private val TEXT_RECT = Rectangle()
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
