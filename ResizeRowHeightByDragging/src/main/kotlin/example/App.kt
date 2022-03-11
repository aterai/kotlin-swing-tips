package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val table = JTable(DefaultTableModel(10, 3))
  table.rowHeight = 24
  table.autoCreateRowSorter = true
  return JPanel(BorderLayout()).also {
    it.add(JLayer(JScrollPane(table), RowHeightResizeLayer()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowHeightResizeLayer : LayerUI<JScrollPane>() {
  private var mouseYOffset = 0
  private var resizingRow = -1
  private var otherCursor = RESIZE_CURSOR

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val table = e.component
    if (table is JTable && e.id == MouseEvent.MOUSE_PRESSED) {
      resizingRow = getResizeTargetRow(table, e.point)
      if (resizingRow >= 0) {
        mouseYOffset = e.y - table.getRowHeight(resizingRow)
        e.consume()
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val table = e.component as? JTable ?: return
    when (e.id) {
      MouseEvent.MOUSE_MOVED -> {
        val isResizing = table.cursor === RESIZE_CURSOR
        val row = getResizeTargetRow(table, e.point)
        if (row >= 0 != isResizing) {
          val tmp = table.cursor
          table.cursor = otherCursor
          otherCursor = tmp
        }
      }
      MouseEvent.MOUSE_DRAGGED -> {
        val newHeight = e.y - mouseYOffset
        if (newHeight > MIN_ROW_HEIGHT && resizingRow >= 0) {
          table.setRowHeight(resizingRow, newHeight)
        }
        e.consume()
      }
    }
  }

  private fun getResizeTargetRow(table: JTable, p: Point): Int {
    val row = table.rowAtPoint(p)
    val col = table.columnAtPoint(p)
    val r = table.getCellRect(row, col, false)
    r.grow(0, -2)
    return when {
      // col != 0 -> -1
      r.contains(p) -> -1
      p.y < r.centerY -> row - 1
      else -> row
    }
  }

  companion object {
    private const val MIN_ROW_HEIGHT = 16
    private val RESIZE_CURSOR = Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
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
