package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
import java.io.IOException
import java.net.MalformedURLException
import java.net.URISyntaxException
import java.net.URL
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf("No.", "Name", "URL")
  private val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> { Int::class.java }
      1 -> { String::class.java }
      2 -> { URL::class.java }
      else -> { super.getColumnClass(column) }
    }

    override fun isCellEditable(row: Int, col: Int) = false
  }

  init {
    try {
      model.addRow(arrayOf<Any>(0, "FrontPage", URL("https://ateraimemo.com/")))
      model.addRow(arrayOf<Any>(1, "Java Swing Tips", URL("https://ateraimemo.com/Swing.html")))
      model.addRow(arrayOf<Any>(2, "Example", URL("http://www.example.com/")))
      model.addRow(arrayOf<Any>(3, "Example.jp", URL("http://www.example.jp/")))
    } catch (ex: MalformedURLException) {
      ex.printStackTrace()
    }

    val table = object : JTable(model) {
      private val evenColor = Color(250, 250, 250)
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
    table.putClientProperty("terminateEditOnFocusLost", java.lang.Boolean.TRUE)
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
}

internal class UrlRenderer : DefaultTableCellRenderer(), MouseListener, MouseMotionListener {
  private var vrow = -1 // viewRowIndex
  private var vcol = -1 // viewColumnIndex
  private var isRollover: Boolean = false

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    super.getTableCellRendererComponent(table, value, isSelected, false, row, column)
    val str = value?.toString() ?: ""
    if (isRolloverCell(table, row, column)) {
      setText("<html><u><font color='blue'>$str")
    } else if (hasFocus) {
      setText("<html><font color='blue'>$str")
    } else {
      setText(str)
    }
    return this
  }

  private fun isRolloverCell(table: JTable, row: Int, column: Int): Boolean {
    return !table.isEditing() && this.vrow == row && this.vcol == column && this.isRollover
  }

  private fun isUrlColumn(table: JTable, column: Int): Boolean {
    return column >= 0 && table.getColumnClass(column) == URL::class.java
  }

  override fun mouseMoved(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    val pt = e.getPoint()
    val prevRow = vrow
    val prevCol = vcol
    val prevRollover = isRollover
    vrow = table.rowAtPoint(pt)
    vcol = table.columnAtPoint(pt)
    isRollover = isUrlColumn(table, vcol)
    val isSameCell = vrow == prevRow && vcol == prevCol && isRollover == prevRollover
    if (isSameCell || (!isRollover && !prevRollover)) {
      return
    }
    val repaintRect: Rectangle
    if (isRollover) {
      val r = table.getCellRect(vrow, vcol, false)
      repaintRect = if (prevRollover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
    } else { // if (prevRollover) {
      repaintRect = table.getCellRect(prevRow, prevCol, false)
    }
    table.repaint(repaintRect)
  }

  override fun mouseExited(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    if (isUrlColumn(table, vcol)) {
      table.repaint(table.getCellRect(vrow, vcol, false))
      vrow = -1
      vcol = -1
      isRollover = false
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    val pt = e.getPoint()
    val ccol = table.columnAtPoint(pt)
    if (isUrlColumn(table, ccol)) {
      val crow = table.rowAtPoint(pt)
      val url = table.getValueAt(crow, ccol) as URL
      println(url)
      try {
        if (Desktop.isDesktopSupported()) { // JDK 1.6.0
          Desktop.getDesktop().browse(url.toURI())
        }
      } catch (ex: URISyntaxException) {
        ex.printStackTrace()
      } catch (ex: IOException) {
        ex.printStackTrace()
      }
    }
  }

  override fun mouseDragged(e: MouseEvent) { /* not needed */
  }

  override fun mouseEntered(e: MouseEvent) { /* not needed */
  }

  override fun mousePressed(e: MouseEvent) { /* not needed */
  }

  override fun mouseReleased(e: MouseEvent) { /* not needed */
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
