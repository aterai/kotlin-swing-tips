package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf<Array<Any>>(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val table = object : JTable(model) {
  private var highlighter: HighlightListener? = null

  override fun updateUI() {
    addMouseListener(highlighter)
    addMouseMotionListener(highlighter)
    setDefaultRenderer(Any::class.java, null)
    setDefaultRenderer(Number::class.java, null)
    setDefaultRenderer(Boolean::class.javaObjectType, null)
    super.updateUI()
    val hl = HighlightListener()
    highlighter = hl
    addMouseListener(hl)
    addMouseMotionListener(hl)
    setDefaultRenderer(Any::class.java, RolloverDefaultTableCellRenderer(hl))
    setDefaultRenderer(Number::class.java, RolloverNumberRenderer(hl))
    setDefaultRenderer(Boolean::class.javaObjectType, RolloverBooleanRenderer(hl))
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ) = super.prepareEditor(editor, row, column).also {
    (it as? JCheckBox)?.background = getSelectionBackground()
  }
}

fun makeUI(): Component {
  table.autoCreateRowSorter = true

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.topComponent = JScrollPane(JTable(model))
  sp.bottomComponent = JScrollPane(table)
  sp.resizeWeight = .5

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

private class HighlightListener : MouseAdapter() {
  private var viewRowIndex = -1
  private var viewColumnIndex = -1

  fun isHighlightedCell(
    row: Int,
    column: Int,
  ) = viewRowIndex == row && viewColumnIndex == column

  override fun mouseMoved(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      val pt = e.point
      val prevRow = viewRowIndex
      val prevCol = viewColumnIndex
      viewRowIndex = table.rowAtPoint(pt)
      viewColumnIndex = table.columnAtPoint(pt)
      if (viewRowIndex < 0 || viewColumnIndex < 0) {
        viewRowIndex = -1
        viewColumnIndex = -1
      }
      if (viewRowIndex == prevRow && viewColumnIndex == prevCol) {
        return
      }
      table.repaint(getRepaintRect(table, prevRow, prevCol))
    }
  }

  private fun getRepaintRect(
    table: JTable,
    pr: Int,
    pc: Int,
  ) = if (viewRowIndex >= 0 && viewColumnIndex >= 0) {
    val r = table.getCellRect(viewRowIndex, viewColumnIndex, false)
    if (pr >= 0 && pc >= 0) r.union(table.getCellRect(pr, pc, false)) else r
  } else {
    table.getCellRect(pr, pc, false)
  }

  override fun mouseExited(e: MouseEvent) {
    (e.component as? JTable)?.also { table ->
      if (viewRowIndex >= 0 && viewColumnIndex >= 0) {
        table.repaint(table.getCellRect(viewRowIndex, viewColumnIndex, false))
      }
      viewRowIndex = -1
      viewColumnIndex = -1
    }
  }
}

private open class RolloverDefaultTableCellRenderer(
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
    val str = value?.toString() ?: ""
    val isHighlightedCell = highlighter.isHighlightedCell(row, column)
    c.foreground = when {
      isSelected && isHighlightedCell -> HIGHLIGHT
      isSelected && !isHighlightedCell -> table.selectionForeground
      else -> table.foreground
    }
    c.background = when {
      isSelected && isHighlightedCell -> table.selectionBackground.darker()
      isSelected && !isHighlightedCell -> table.selectionBackground
      else -> table.background
    }
    (c as? JLabel)?.text = if (isHighlightedCell) "<html><u>$str" else str
    return c
  }

  companion object {
    private val HIGHLIGHT = Color(255, 150, 50)
  }
}

private class RolloverNumberRenderer(
  highlighter: HighlightListener,
) : RolloverDefaultTableCellRenderer(highlighter) {
  init {
    horizontalAlignment = RIGHT
  }
}

private class RolloverBooleanRenderer(
  private val highlighter: HighlightListener,
) : TableCellRenderer {
  private val check = object : JCheckBox() {
    override fun updateUI() {
      super.updateUI()
      // horizontalAlignment = CENTER
      isBorderPainted = true
      isRolloverEnabled = true
      isOpaque = true
      border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    }

    // Overridden for performance reasons. ---->
    override fun isOpaque(): Boolean {
      val o = SwingUtilities.getAncestorOfClass(JTable::class.java, this)
      return (o as? JTable)?.let { table ->
        val back = background
        val colorMatch = back != null && back == table.background && table.isOpaque
        !colorMatch && super.isOpaque()
      } ?: super.isOpaque()
    }

    override fun firePropertyChange(
      propertyName: String,
      oldValue: Any?,
      newValue: Any?,
    ) {
      // if (propertyName == "border" ||
      //     ((propertyName == "font" || propertyName == "foreground") && oldValue != newValue)) {
      //   super.firePropertyChange(propertyName, oldValue, newValue)
      // }
    }

    override fun firePropertyChange(
      propertyName: String,
      oldValue: Boolean,
      newValue: Boolean,
    ) {
      // Overridden for performance reasons.
    }

    override fun repaint(
      tm: Long,
      x: Int,
      y: Int,
      width: Int,
      height: Int,
    ) {
      // Overridden for performance reasons.
    }

    override fun repaint(r: Rectangle) {
      // Overridden for performance reasons.
    }

    override fun repaint() {
      // Overridden for performance reasons.
    }

    override fun invalidate() {
      // Overridden for performance reasons.
    }

    override fun validate() {
      // Overridden for performance reasons.
    }

    override fun revalidate() {
      // Overridden for performance reasons.
    }
    // <---- Overridden for performance reasons.
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    check.horizontalAlignment = SwingConstants.CENTER
    check.model.isRollover = highlighter.isHighlightedCell(row, column)
    if (isSelected) {
      check.foreground = table.selectionForeground
      check.setBackground(table.selectionBackground)
    } else {
      check.foreground = table.foreground
      check.background = table.background
      // check.background = if (row % 2 == 0) table.background else Color.WHITE // Nimbus
    }
    check.setSelected(value == true)
    return check
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
