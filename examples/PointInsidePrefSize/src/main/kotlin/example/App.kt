package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = object : JTable(makeModel()) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
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
  val renderer = UriRenderer()
  table.setDefaultRenderer(URI::class.java, renderer)
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

fun makeModel(): TableModel {
  val columnNames = arrayOf("No.", "Name", "URI")
  val m = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> Number::class.java
      1 -> String::class.java
      2 -> URI::class.java
      else -> super.getColumnClass(column)
    }

    override fun isCellEditable(
      row: Int,
      col: Int,
    ) = false
  }
  m.addRow(makeRow(0, "FrontPage", "https://ateraimemo.com/"))
  m.addRow(makeRow(1, "Java Swing Tips", "https://ateraimemo.com/Swing.html"))
  m.addRow(makeRow(2, "Example", "http://www.example.com/"))
  m.addRow(makeRow(3, "Example.jp", "http://www.example.jp/"))
  return m
}

private fun makeRow(i: Int, title: String, path: String) = arrayOf<Any?>(
  i,
  title,
  runCatching { URI(path) }.getOrNull(),
)

private class UriRenderer :
  MouseAdapter(),
  TableCellRenderer {
  private val renderer = DefaultTableCellRenderer()
  private var viewRowIdx = -1
  private var viewColIdx = -1
  private var isHover = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = renderer.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      false,
      row,
      column,
    )
    if (c is JLabel) {
      val cm = table.columnModel
      val i = c.insets
      CELL_RECT.x = i.left
      CELL_RECT.y = i.top
      CELL_RECT.width =
        cm.getColumn(column).width - cm.columnMargin - i.right - CELL_RECT.x
      CELL_RECT.height =
        table.getRowHeight(row) - table.rowMargin - i.bottom - CELL_RECT.y
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
      val hover = isHoverCell(table, row, column)
      c.text = if (hover) "<html><u><font color='blue'>$str" else str
    }
    return c
  }

  private fun isHoverCell(
    table: JTable,
    row: Int,
    col: Int,
  ) = !table.isEditing && viewRowIdx == row && viewColIdx == col && isHover

  override fun mouseMoved(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val pt = e.point
    val prevRow = viewRowIdx
    val prevCol = viewColIdx
    val prevHover = isHover
    viewRowIdx = table.rowAtPoint(pt)
    viewColIdx = table.columnAtPoint(pt)
    isHover = isUriColumn(table, viewColIdx) && pointInsidePrefSize(table, pt)
    val hover = isHover == prevHover
    val isSameCell = viewRowIdx == prevRow && viewColIdx == prevCol && hover
    val isNotHover = !isHover && !prevHover
    if (isSameCell || isNotHover) {
      return
    }
    val repaintRect = if (isHover) {
      val r = table.getCellRect(viewRowIdx, viewColIdx, false)
      if (prevHover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
    } else { // if (prevHover) {
      table.getCellRect(prevRow, prevCol, false)
    }
    table.repaint(repaintRect)
  }

  override fun mouseExited(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    if (isUriColumn(table, viewColIdx)) {
      table.repaint(table.getCellRect(viewRowIdx, viewColIdx, false))
      viewRowIdx = -1
      viewColIdx = -1
      isHover = false
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val pt = e.point
    val col = table.columnAtPoint(pt)
    if (isUriColumn(table, col) && pointInsidePrefSize(table, pt)) {
      val row = table.rowAtPoint(pt)
      val uri = table.getValueAt(row, col) as? URI ?: return
      if (Desktop.isDesktopSupported()) {
        runCatching {
          Desktop.getDesktop().browse(uri)
        }.onFailure {
          it.printStackTrace()
        }
      }
    }
  }

  private fun isUriColumn(
    tbl: JTable,
    col: Int,
  ) = col >= 0 && tbl.getColumnClass(col) == URI::class.java

  // @see SwingUtilities2.pointOutsidePrefSize(...)
  private fun pointInsidePrefSize(
    table: JTable,
    p: Point,
  ): Boolean {
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
