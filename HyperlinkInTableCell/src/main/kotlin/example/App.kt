package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.MouseMotionListener
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

internal class UrlRenderer : DefaultTableCellRenderer(), MouseListener, MouseMotionListener {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1 // viewColumnIndex
  private var isRollover = false

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
    setText(when {
      isRolloverCell(table, row, column) -> "<html><u><font color='blue'>$str"
      hasFocus -> "<html><font color='blue'>$str"
      else -> str
    })
    return this
  }

  private fun isRolloverCell(table: JTable, row: Int, column: Int) =
    !table.isEditing() && viewRowIndex == row && viewColumnIndex == column && isRollover

  private fun isUrlColumn(table: JTable, column: Int) =
    column >= 0 && table.getColumnClass(column) == URL::class.java

  override fun mouseMoved(e: MouseEvent) {
    val table = e.getComponent() as? JTable ?: return
    val pt = e.getPoint()
    val prevRow = viewRowIndex
    val prevCol = viewColumnIndex
    val prevRollover = isRollover
    viewRowIndex = table.rowAtPoint(pt)
    viewColumnIndex = table.columnAtPoint(pt)
    isRollover = isUrlColumn(table, viewColumnIndex)
    val isSameCell = viewRowIndex == prevRow && viewColumnIndex == prevCol && isRollover == prevRollover
    val isNotRollover = !isRollover && !prevRollover
    if (isSameCell || isNotRollover) {
      return
    }
    table.repaint(when {
      isRollover -> {
        val r = table.getCellRect(viewRowIndex, viewColumnIndex, false)
        if (prevRollover) r.union(table.getCellRect(prevRow, prevCol, false)) else r
      }
      else -> table.getCellRect(prevRow, prevCol, false)
    })
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
    if (isUrlColumn(table, col)) {
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

  override fun mouseDragged(e: MouseEvent) { /* not needed */ }

  override fun mouseEntered(e: MouseEvent) { /* not needed */ }

  override fun mousePressed(e: MouseEvent) { /* not needed */ }

  override fun mouseReleased(e: MouseEvent) { /* not needed */ }
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
