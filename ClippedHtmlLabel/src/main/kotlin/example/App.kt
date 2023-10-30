package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.net.URL
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private fun makeUrl(path: String): URL? = runCatching { URL(path) }.getOrNull()

private fun makeTable(model: DefaultTableModel): JTable {
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

fun makeUI(): Component {
  val columnNames = arrayOf("No.", "Name", "URL")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int): Class<*> {
      return when (column) {
        0 -> Number::class.java
        1 -> String::class.java
        2 -> URL::class.java
        else -> super.getColumnClass(column)
      }
    }

    override fun isCellEditable(
      row: Int,
      col: Int,
    ) = false
  }
  model.addRow(arrayOf(0, "FrontPage", makeUrl("https://ateraimemo.com/")))
  model.addRow(arrayOf(1, "Java Swing Tips", makeUrl("https://ateraimemo.com/Swing.html")))
  model.addRow(arrayOf(2, "Example", makeUrl("http://www.example.com/")))
  model.addRow(arrayOf(3, "Example.jp", makeUrl("http://www.example.jp/")))

  val table1 = makeTable(model)
  val renderer1 = UrlRenderer1()
  table1.setDefaultRenderer(URL::class.java, renderer1)
  table1.addMouseListener(renderer1)
  table1.addMouseMotionListener(renderer1)

  val table = makeTable(model)
  val renderer = UrlRenderer()
  table.setDefaultRenderer(URL::class.java, renderer)
  table.addMouseListener(renderer)
  table.addMouseMotionListener(renderer)

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(table1)
  sp.bottomComponent = JScrollPane(table)
  sp.resizeWeight = .5
  sp.preferredSize = Dimension(320, 240)
  return sp
}

private class UrlRenderer1 : UrlRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column)
    val str = value?.toString() ?: ""
    (c as? JLabel)?.text = when {
      isRolloverCell(table, row, column) -> "<html><u><font color='blue'>$str"
      hasFocus -> "<html><font color='blue'>$str"
      else -> str
    }
    return c
  }
}

private open class UrlRenderer : DefaultTableCellRenderer(), MouseListener, MouseMotionListener {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1
  private var isRollover = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(table, value, isSelected, false, row, column)
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
  ) = !table.isEditing && viewRowIndex == row && viewColumnIndex == column && isRollover

  override fun mouseMoved(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      val pt = e.point
      val prevRow = viewRowIndex
      val prevCol = viewColumnIndex
      val prevRollover = isRollover
      viewRowIndex = table.rowAtPoint(pt)
      viewColumnIndex = table.columnAtPoint(pt)
      isRollover = isUrlColumn(table, viewColumnIndex) // && pointInsidePrefSize(table, pt)
      if (viewRowIndex == prevRow && viewColumnIndex == prevCol && isRollover == prevRollover) {
        return
      }
      if (!isRollover && !prevRollover) {
        return
      }
      val repaintRect = if (isRollover) {
        val r = table.getCellRect(viewRowIndex, viewColumnIndex, false)
        if (prevRollover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
      } else {
        table.getCellRect(prevRow, prevCol, false)
      }
      table.repaint(repaintRect)
    }
  }

  override fun mouseExited(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      if (isUrlColumn(table, viewColumnIndex)) {
        table.repaint(table.getCellRect(viewRowIndex, viewColumnIndex, false))
        viewRowIndex = -1
        viewColumnIndex = -1
        isRollover = false
      }
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val pt = e.point
    (e.component as? JTable)?.also { table ->
      val col = table.columnAtPoint(pt)
      if (isUrlColumn(table, col)) { // && pointInsidePrefSize(table, pt)) {
        val crow = table.rowAtPoint(pt)
        runCatching {
          if (Desktop.isDesktopSupported()) {
            val url = table.getValueAt(crow, col) as? URL
            Desktop.getDesktop().browse(url?.toURI())
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

    private fun isUrlColumn(
      table: JTable,
      column: Int,
    ) = column >= 0 && table.getColumnClass(column) == URL::class.java
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
