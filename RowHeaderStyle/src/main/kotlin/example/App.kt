package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("ccc", 92, true),
    arrayOf("ddd", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = column != 0
  }
  val table = object : JTable(model) {
    @Transient private var handler: RowHeaderRenderer? = null
    override fun updateUI() {
      getColumnModel().getColumn(0).cellRenderer = null
      removeMouseListener(handler)
      removeMouseMotionListener(handler)
      super.updateUI()
      handler = RowHeaderRenderer()
      getColumnModel().getColumn(0).cellRenderer = handler
      addMouseListener(handler)
      addMouseMotionListener(handler)
    }
  }
  table.autoCreateRowSorter = true
  table.rowHeight = 24

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowHeaderRenderer : MouseAdapter(), TableCellRenderer {
  private val renderer = JLabel()
  private var rollOverRowIndex = -1
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val tcr = table.tableHeader.defaultRenderer
    val f = row == rollOverRowIndex
    val c = tcr.getTableCellRendererComponent(table, value, isSelected, f || hasFocus, -1, -1)
    return if (tcr.javaClass.name.contains("XPDefaultRenderer")) {
      (c as? JComponent)?.isOpaque = !f
      renderer.icon = ComponentIcon(c)
      renderer
    } else {
      c
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    val table = e.component as? JTable
    val pt = e.point
    val col = table?.columnAtPoint(pt) ?: -1
    val column = table?.convertColumnIndexToModel(col)
    if (column != 0) {
      return
    }
    val prevRow = rollOverRowIndex
    rollOverRowIndex = table.rowAtPoint(pt)
    if (rollOverRowIndex == prevRow) {
      return
    }
    val repaintRect: Rectangle
    repaintRect = if (rollOverRowIndex >= 0) {
      val r = table.getCellRect(rollOverRowIndex, col, false)
      if (prevRow >= 0) r.union(table.getCellRect(prevRow, col, false)) else r
    } else {
      table.getCellRect(prevRow, col, false)
    }
    table.repaint(repaintRect)
  }

  override fun mouseExited(e: MouseEvent) {
    val table = e.component as? JTable ?: return
    val col = table.columnAtPoint(e.point)
    if (table.convertColumnIndexToModel(col) == 0) {
      if (rollOverRowIndex >= 0) {
        table.repaint(table.getCellRect(rollOverRowIndex, col, false))
      }
      rollOverRowIndex = -1
    }
  }
}

private class ComponentIcon(private val cmp: Component) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    SwingUtilities.paintComponent(g, cmp, c.parent, x, y, iconWidth, iconHeight)
  }

  override fun getIconWidth() = 4000 // Short.MAX_VALUE

  override fun getIconHeight() = cmp.preferredSize.height + 4 // XXX: +4 for Windows 7
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
