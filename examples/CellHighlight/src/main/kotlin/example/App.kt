package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val table = object : JTable(DefaultTableModel(10, 10)) {
    private var highlighter: HighlightListener? = null

    override fun updateUI() {
      removeMouseListener(highlighter)
      removeMouseMotionListener(highlighter)
      super.updateUI()
      setRowSelectionAllowed(false)
      highlighter = HighlightListener().also {
        addMouseListener(it)
        addMouseMotionListener(it)
        setDefaultRenderer(Any::class.java, HighlightRenderer(it))
        setDefaultRenderer(Number::class.java, HighlightRenderer(it))
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightListener : MouseAdapter() {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1

  fun getCellHighlightColor(
    row: Int,
    column: Int,
  ): Color? {
    val ri = viewRowIndex == row
    val ci = viewColumnIndex == column
    return if (ri || ci) {
      if (ri && ci) HIGHLIGHT1 else HIGHLIGHT2
    } else {
      null
    }
  }

  private fun setHighlightTableCell(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      viewRowIndex = table.rowAtPoint(e.point)
      viewColumnIndex = table.columnAtPoint(e.point)
      if (viewRowIndex < 0 || viewColumnIndex < 0) {
        viewRowIndex = -1
        viewColumnIndex = -1
      }
      table.repaint()
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    setHighlightTableCell(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    setHighlightTableCell(e)
  }

  override fun mouseExited(e: MouseEvent) {
    viewRowIndex = -1
    viewColumnIndex = -1
    e.component.repaint()
  }

  companion object {
    private val HIGHLIGHT1 = Color(0xC8_C8_FF)
    private val HIGHLIGHT2 = Color(0xF0_F0_FF)
  }
}

private class HighlightRenderer(
  private val highlighter: HighlightListener,
) : DefaultTableCellRenderer() {
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
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      c.background = table.background
      c.horizontalAlignment = if (value is Number) RIGHT else LEFT
      highlighter.getCellHighlightColor(row, column)?.also {
        c.background = it
      }
    }
    return c
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
