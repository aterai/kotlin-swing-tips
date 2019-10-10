package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
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

  init {
    model.addRow(arrayOf<Any?>(0, "FrontPage", makeUrl("https://ateraimemo.com/")))
    model.addRow(arrayOf<Any?>(1, "Java Swing Tips", makeUrl("https://ateraimemo.com/Swing.html")))
    model.addRow(arrayOf<Any?>(2, "Example", makeUrl("http://www.example.com/")))
    model.addRow(arrayOf<Any?>(3, "Example.jp", makeUrl("http://www.example.jp/")))

    val table = object : JTable(model) {
      private val evenColor = Color(0xFA_FA_FA)
      override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
        val c = super.prepareRenderer(tcr, row, column)
        c.setForeground(getForeground())
        c.setBackground(if (row % 2 == 0) evenColor else getBackground())
        return c
      }
    }
    table.setRowSelectionAllowed(true)
    table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
    table.setIntercellSpacing(Dimension())
    table.setShowGrid(false)
    table.putClientProperty("terminateEditOnFocusLost", true)
    table.setAutoCreateRowSorter(true)

    var col = table.getColumnModel().getColumn(0)
    col.setMinWidth(50)
    col.setMaxWidth(50)
    col.setResizable(false)

    val renderer = UrlRenderer()
    table.setDefaultRenderer(URL::class.java, renderer)
    table.addMouseListener(renderer)
    table.addMouseMotionListener(renderer)

    col = table.getColumnModel().getColumn(1)
    col.setPreferredWidth(1000)

    col = table.getColumnModel().getColumn(2)
    // col.setCellRenderer(renderer)
    col.setPreferredWidth(2000)

    val scrollPane = JScrollPane(table)
    scrollPane.getViewport().setBackground(Color.WHITE)
    add(scrollPane)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeUrl(spec: String) = runCatching { URL(spec) }.getOrNull()
}

class UrlRenderer : MouseAdapter(), TableCellRenderer {
  private val renderer = DefaultTableCellRenderer()
  private var viewRowIndex = -1
  private var viewColumnIndex = -1
  private var isRollover: Boolean = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val c = renderer.getTableCellRendererComponent(table, value, isSelected, false, row, column)
    val label = c as? JLabel ?: return c
    val cm = table.getColumnModel()
    val i = renderer.getInsets()
    CELL_RECT.x = i.left
    CELL_RECT.y = i.top
    CELL_RECT.width = cm.getColumn(column).getWidth() - cm.getColumnMargin() - i.right - CELL_RECT.x
    CELL_RECT.height = table.getRowHeight(row) - table.getRowMargin() - i.bottom - CELL_RECT.y
    ICON_RECT.setBounds(0, 0, 0, 0)
    TEXT_RECT.setBounds(0, 0, 0, 0)

    val str = SwingUtilities.layoutCompoundLabel(
      label, label.getFontMetrics(label.getFont()), value?.toString() ?: "", label.getIcon(),
      label.getVerticalAlignment(), label.getHorizontalAlignment(),
      label.getVerticalTextPosition(), label.getHorizontalTextPosition(),
      CELL_RECT, ICON_RECT, TEXT_RECT, label.getIconTextGap())
    label.setText(if (isRolloverCell(table, row, column)) "<html><u><font color='blue'>$str" else str)
    return label
  }

  private fun isRolloverCell(table: JTable, row: Int, column: Int) =
    !table.isEditing() && viewRowIndex == row && viewColumnIndex == column && isRollover

  override fun mouseMoved(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    val pt = e.getPoint()
    val prevRow = viewRowIndex
    val prevCol = viewColumnIndex
    val prevRollover = isRollover
    viewRowIndex = table.rowAtPoint(pt)
    viewColumnIndex = table.columnAtPoint(pt)
    isRollover = isUrlColumn(table, viewColumnIndex) && pointInsidePrefSize(table, pt)
    val isSameCell = viewRowIndex == prevRow && viewColumnIndex == prevCol && isRollover == prevRollover
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
    val table = e.getComponent() as? JTable ?: return
    if (isUrlColumn(table, viewColumnIndex)) {
      table.repaint(table.getCellRect(viewRowIndex, viewColumnIndex, false))
      viewRowIndex = -1
      viewColumnIndex = -1
      isRollover = false
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    val pt = e.getPoint()
    val col = table.columnAtPoint(pt)
    if (isUrlColumn(table, col) && pointInsidePrefSize(table, pt)) {
      val row = table.rowAtPoint(pt)
      val url = table.getValueAt(row, col) as? URL ?: return
      println(url)
      if (Desktop.isDesktopSupported()) { // JDK 1.6.0
        runCatching {
          Desktop.getDesktop().browse(url.toURI())
        }.onFailure {
          it.printStackTrace()
        }
      }
    }
  }

  private fun isUrlColumn(tbl: JTable, col: Int) = col >= 0 && tbl.getColumnClass(col) == URL::class.java

  // @see SwingUtilities2.pointOutsidePrefSize(...)
  private fun pointInsidePrefSize(table: JTable, p: Point): Boolean {
    val row = table.rowAtPoint(p)
    val col = table.columnAtPoint(p)
    val tcr = table.getCellRenderer(row, col)
    val value = table.getValueAt(row, col)
    val cell = tcr.getTableCellRendererComponent(table, value, false, false, row, col)
    val itemSize = cell.getPreferredSize()
    val cellBounds = table.getCellRect(row, col, false).also {
      val i = (cell as? JComponent)?.getInsets() ?: Insets(0, 0, 0, 0)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
