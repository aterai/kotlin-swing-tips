package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.net.URI
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val model = makeModel()
  val table1 = makeTable(model).also {
    val renderer = UriRenderer1()
    it.setDefaultRenderer(URI::class.java, renderer)
    it.addMouseListener(renderer)
    it.addMouseMotionListener(renderer)
  }
  val table2 = makeTable(model).also {
    val renderer = UriRenderer()
    it.setDefaultRenderer(URI::class.java, renderer)
    it.addMouseListener(renderer)
    it.addMouseMotionListener(renderer)
  }
  return JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.topComponent = JScrollPane(table1)
    it.bottomComponent = JScrollPane(table2)
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
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

private fun makeTable(model: TableModel): JTable {
  val table = object : JTable(model) {
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

  val col = table.columnModel.getColumn(0)
  col.minWidth = 50
  col.maxWidth = 50
  col.resizable = false
  return table
}

private fun makeRow(i: Int, title: String, path: String) = arrayOf<Any?>(
  i,
  title,
  runCatching { URI(path) }.getOrNull(),
)

private class UriRenderer1 : UriRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      false,
      row,
      column,
    )
    val str = value?.toString() ?: ""
    (c as? JLabel)?.text = when {
      isRolloverCell(table, row, column) -> "<html><u><font color='blue'>$str"
      hasFocus -> "<html><font color='blue'>$str"
      else -> str
    }
    return c
  }
}

open class UriRenderer :
  DefaultTableCellRenderer(),
  MouseListener,
  MouseMotionListener {
  private var viewRowIdx = -1
  private var viewColIdx = -1
  private var hover = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
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
        c.getFontMetrics(font),
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
      c.text = when {
        isRolloverCell(table, row, column) -> "<html><u><font color='blue'>$str"
        hasFocus -> "<html><font color='blue'>$str"
        else -> str
      }
    }
    return c
  }

  fun isRolloverCell(
    table: JTable,
    row: Int,
    column: Int,
  ) = !table.isEditing && viewRowIdx == row && viewColIdx == column && hover

  override fun mouseMoved(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      val pt = e.point
      val prevRow = viewRowIdx
      val prevCol = viewColIdx
      val prevHover = hover
      viewRowIdx = table.rowAtPoint(pt)
      viewColIdx = table.columnAtPoint(pt)
      hover = isUriColumn(table, viewColIdx)
      val b1 = viewRowIdx == prevRow && viewColIdx == prevCol
      val b2 = hover == prevHover
      if (b1 && b2) {
        return
      }
      val repaintRect = if (hover) {
        val r = table.getCellRect(viewRowIdx, viewColIdx, false)
        if (prevHover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
      } else {
        table.getCellRect(prevRow, prevCol, false)
      }
      table.repaint(repaintRect)
    }
  }

  override fun mouseExited(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      if (isUriColumn(table, viewColIdx)) {
        table.repaint(table.getCellRect(viewRowIdx, viewColIdx, false))
        viewRowIdx = -1
        viewColIdx = -1
        hover = false
      }
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val pt = e.point
    (e.component as? JTable)?.also { table ->
      val col = table.columnAtPoint(pt)
      if (isUriColumn(table, col)) { // && pointInsidePrefSize(table, pt)) {
        val crow = table.rowAtPoint(pt)
        runCatching {
          if (Desktop.isDesktopSupported()) {
            val uri = table.getValueAt(crow, col) as? URI
            Desktop.getDesktop().browse(uri)
          }
        }.onFailure {
          UIManager.getLookAndFeel().provideErrorFeedback(e.component)
        }
      }
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    // not needed
  }

  override fun mouseEntered(e: MouseEvent) {
    // not needed
  }

  override fun mousePressed(e: MouseEvent) {
    // not needed
  }

  override fun mouseReleased(e: MouseEvent) {
    // not needed
  }

  companion object {
    private val CELL_RECT = Rectangle()
    private val ICON_RECT = Rectangle()
    private val TEXT_RECT = Rectangle()

    private fun isUriColumn(
      table: JTable,
      column: Int,
    ) = column >= 0 && table.getColumnClass(column) == URI::class.java
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
