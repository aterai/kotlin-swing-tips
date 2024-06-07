package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputListener
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val columnNames = arrayOf("String0", "String111", "String22222")
  val data = arrayOf(
    arrayOf("a", "bb", "cc"),
    arrayOf("dd", "e", "ff"),
    arrayOf("aa", "aa", "a"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.showVerticalLines = false
  table.fillsViewportHeight = true
  table.autoCreateRowSorter = true
  val sorter = table.rowSorter
  if (sorter is DefaultRowSorter<*, *>) {
    for (i in 0..<table.columnCount) {
      sorter.setSortable(i, false)
    }
  }
  val handler = HyperlinkHeaderCellRenderer()
  table.tableHeader.defaultRenderer = handler
  table.tableHeader.addMouseListener(handler)
  table.tableHeader.addMouseMotionListener(handler)
  table.tableHeader.background = table.background

  val scroll = JScrollPane(table)
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.isOpaque = true
    it.background = table.background
    it.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HyperlinkHeaderCellRenderer : DefaultTableCellRenderer(), MouseInputListener {
  private val border = BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.GRAY),
    BorderFactory.createEmptyBorder(4, 1, 3, 2),
  )
  private val alphaZero = Color(0x0, true)
  private var col = -1

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val str = value?.toString() ?: ""
    var sort = ""
    val sorter = table.rowSorter
    if (sorter != null && sorter.sortKeys.isNotEmpty()) {
      val sortKey = sorter.sortKeys[0]
      if (column == sortKey.column) {
        val k = if (sortKey.sortOrder == SortOrder.ASCENDING) "▴" else "▾"
        sort = "<small>$k"
      }
    }
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      c.text = when {
        col == column -> "<html><u><font color='blue'>$str</u>$sort"
        hasFocus -> "<html><font color='blue'>$str$sort"
        else -> "<html>$str$sort"
      }
      c.horizontalAlignment = LEADING
      c.isOpaque = false
      c.background = alphaZero
      c.foreground = Color.BLACK
      c.border = border
    }
    return c
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = 24
    return d
  }

  private fun getTextRect(
    header: JTableHeader,
    idx: Int,
  ): Rectangle {
    val table = header.table
    val hr = table.tableHeader.defaultRenderer
    val headerValue = header.columnModel.getColumn(idx).headerValue
    val c = hr.getTableCellRendererComponent(table, headerValue, false, true, 0, idx)
    val viewRect = Rectangle(header.getHeaderRect(idx))
    val iconRect = Rectangle()
    val textRect = Rectangle()
    if (c is JLabel) {
      val ins = c.insets
      viewRect.x += ins.left
      viewRect.width -= ins.left + ins.right
      SwingUtilities.layoutCompoundLabel(
        c,
        c.getFontMetrics(c.font),
        c.text,
        c.icon,
        c.verticalAlignment,
        c.horizontalAlignment,
        c.verticalTextPosition,
        c.horizontalTextPosition,
        viewRect,
        iconRect,
        textRect,
        c.iconTextGap,
      )
    }
    return textRect
  }

  override fun mouseMoved(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val ci = header.columnAtPoint(e.point)
    col = if (getTextRect(header, ci).contains(e.point)) ci else -1
    header.repaint(header.getHeaderRect(ci))
  }

  override fun mouseExited(e: MouseEvent) {
    col = -1
    e.component.repaint()
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val table = header.table
    val ci = header.columnAtPoint(e.point)
    if (getTextRect(header, ci).contains(e.point)) {
      (table.rowSorter as? DefaultRowSorter<*, *>)?.also {
        val idx = table.convertColumnIndexToModel(ci)
        it.setSortable(idx, true)
        it.toggleSortOrder(idx)
        it.setSortable(idx, false)
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
