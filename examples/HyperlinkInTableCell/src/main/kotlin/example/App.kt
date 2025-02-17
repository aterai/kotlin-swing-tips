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

fun makeUI(): Component {
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
  m.addRow(makeRow(1, "Example", "https://www.example.com/"))
  m.addRow(makeRow(2, "Example.jp", "https://www.example.jp/"))

  val table = object : JTable(m) {
    private val evenColor = Color(250, 250, 250)

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

private fun makeRow(i: Int, title: String, spec: String) = arrayOf(
  i,
  title,
  runCatching { URI(spec) }.getOrNull(),
)

private class UriRenderer :
  DefaultTableCellRenderer(),
  MouseListener,
  MouseMotionListener {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1 // viewColumnIndex
  private var isRollover = false

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
      val str = value?.toString() ?: ""
      c.text = when {
        isRolloverCell(table, row, column) -> "<html><u><font color='blue'>$str"
        hasFocus -> "<html><font color='blue'>$str"
        else -> str
      }
    }
    return c
  }

  private fun isRolloverCell(
    table: JTable,
    row: Int,
    column: Int,
  ) = !table.isEditing && viewRowIndex == row && viewColumnIndex == column && isRollover

  private fun isUriColumn(
    table: JTable,
    column: Int,
  ) = column >= 0 && table.getColumnClass(column) == URI::class.java

  override fun mouseMoved(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val pt = e.point
    val prevRow = viewRowIndex
    val prevCol = viewColumnIndex
    val prevRollover = isRollover
    viewRowIndex = table.rowAtPoint(pt)
    viewColumnIndex = table.columnAtPoint(pt)
    isRollover = isUriColumn(table, viewColumnIndex)
    val isSameView = viewRowIndex == prevRow && viewColumnIndex == prevCol
    val isSameCell = isSameView && isRollover == prevRollover
    val isNotRollover = !isRollover && !prevRollover
    if (isSameCell || isNotRollover) {
      return
    }
    val rect = if (isRollover) {
      val r = table.getCellRect(viewRowIndex, viewColumnIndex, false)
      if (prevRollover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
    } else {
      table.getCellRect(prevRow, prevCol, false)
    }
    table.repaint(rect)
  }

  override fun mouseExited(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    if (isUriColumn(table, viewColumnIndex)) {
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
    if (isUriColumn(table, col)) {
      val row = table.rowAtPoint(pt)
      val uri = table.getValueAt(row, col) as? URI ?: return
      if (Desktop.isDesktopSupported()) { // JDK 1.6.0
        runCatching {
          Desktop.getDesktop().browse(uri)
        }.onFailure {
          it.printStackTrace()
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
