package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.io.Serializable
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import kotlin.math.pow

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf(null, 15, true),
    arrayOf("", null, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val sorter = TableSorter(model)
  val table = object : JTable(sorter) {
    private val evenColor = Color(0xFA_FA_FA)

    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(tcr, row, column)
      if (isRowSelected(row)) {
        c.foreground = getSelectionForeground()
        c.background = getSelectionBackground()
      } else {
        c.foreground = foreground
        c.background = if (row % 2 == 0) evenColor else background
      }
      return c
    }

    override fun updateUI() {
      sorter.setTableHeader(null)
      super.updateUI()
      EventQueue.invokeLater {
        val h = getTableHeader()
        sorter.setTableHeader(h)
        h.repaint()
      }
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TableSorter() : AbstractTableModel() {
  private var tableModel: TableModel? = null
  private val viewToModel = mutableListOf<TableRow>()
  private val modelToView = mutableListOf<Int>()
  private val sortingColumns = mutableListOf<Directive>()
  private var tableHeader: JTableHeader? = null

  // private val columnComparators: MutableMap<Class<*>, Comparator<*>> = ConcurrentHashMap()
  private val rowComparator = RowComparator()
  private var mouseListener: MouseListener
  private var modelListener: TableModelListener

  val isSorting get() = sortingColumns.isNotEmpty()

  init {
    mouseListener = MouseHandler()
    modelListener = TableModelHandler()
  }

  constructor(tableModel: TableModel?) : this() {
    setTableModel(tableModel)
  }

  // constructor(tableModel: TableModel?, tableHeader: JTableHeader?) : this() {
  //   setTableHeader(tableHeader)
  //   setTableModel(tableModel)
  // }

  // fun readObject() {
  //   mouseListener = MouseHandler()
  //   modelListener = TableModelHandler()
  // }

  fun readResolve(): Any {
    mouseListener = MouseHandler()
    modelListener = TableModelHandler()
    return this
  }

  fun clearSortingState() {
    viewToModel.clear()
    modelToView.clear()
  }

  fun setTableModel(tableModel: TableModel?) {
    this.tableModel?.removeTableModelListener(modelListener)
    this.tableModel = tableModel
    this.tableModel?.addTableModelListener(modelListener)
    EventQueue.invokeLater {
      clearSortingState()
      fireTableStructureChanged()
    }
  }

  fun setTableHeader(tableHeader: JTableHeader?) {
    this.tableHeader?.also { header ->
      header.removeMouseListener(mouseListener)
      (header.defaultRenderer as? SortableHeaderRenderer)?.also {
        header.defaultRenderer = it.cellRenderer
      }
    }
    this.tableHeader = tableHeader
    this.tableHeader?.also {
      it.addMouseListener(mouseListener)
      it.defaultRenderer = SortableHeaderRenderer(it.defaultRenderer)
    }
  }

  private fun getDirective(column: Int) =
    sortingColumns.firstOrNull { it.column == column } ?: EMPTY_DIRECTIVE

  fun getSortingStatus(column: Int) = getDirective(column).direction

  private fun sortingStatusChanged() {
    clearSortingState()
    fireTableDataChanged()
    tableHeader?.repaint()
  }

  fun setSortingStatus(
    column: Int,
    status: Int,
  ) {
    getDirective(column).takeIf { it != EMPTY_DIRECTIVE }.also {
      sortingColumns.remove(it)
    }
    if (status != NOT_SORTED) {
      sortingColumns.add(Directive(column, status))
    }
    sortingStatusChanged()
  }

  fun getHeaderRendererIcon(
    column: Int,
    size: Int,
  ): Icon? {
    val dir = getDirective(column)
    return if (EMPTY_DIRECTIVE == dir) {
      null
    } else {
      Arrow(dir.direction == DESCENDING, size, sortingColumns.indexOf(dir))
    }
  }

  fun cancelSorting() {
    sortingColumns.clear()
    sortingStatusChanged()
  }

  // fun setColumnComparator(type: Class<*>, comparator: Comparator<*>?) {
  //   if (comparator == null) {
  //     columnComparators.remove(type)
  //   } else {
  //     columnComparators[type] = comparator
  //   }
  // }

  // fun getComparator(column: Int): Comparator<*> {
  //   val columnType = tableModel?.getColumnClass(column)
  //   return columnComparators[columnType] ?: LEXICAL_COMP
  // }

  private fun getViewToModel(): List<TableRow> {
    val rc = tableModel?.rowCount ?: 0
    if (viewToModel.isEmpty() && tableModel != null) {
      for (i in 0 until rc) {
        viewToModel.add(TableRow(i))
      }
      if (isSorting) {
        viewToModel.sortWith(rowComparator)
      }
    }
    return viewToModel
  }

  fun modelIndex(viewIndex: Int) = getViewToModel()[viewIndex].modelIndex

  private fun getModelToView(): List<Int> {
    if (modelToView.isEmpty()) {
      for (i in getViewToModel().indices) {
        modelToView.add(modelIndex(i))
      }
    }
    return modelToView
  }

  override fun getRowCount() = tableModel?.rowCount ?: 0

  override fun getColumnCount() = tableModel?.columnCount ?: 0

  override fun getColumnName(column: Int) = tableModel?.getColumnName(column)

  override fun getColumnClass(column: Int) = tableModel?.getColumnClass(column)

  override fun isCellEditable(
    row: Int,
    column: Int,
  ) = tableModel?.isCellEditable(modelIndex(row), column) ?: false

  override fun getValueAt(
    row: Int,
    column: Int,
  ) = tableModel?.getValueAt(modelIndex(row), column)

  override fun setValueAt(
    value: Any?,
    row: Int,
    column: Int,
  ) {
    tableModel?.setValueAt(value, modelIndex(row), column)
  }

  // Helper classes
  private inner class RowComparator : Comparator<TableRow> {
    override fun compare(
      r1: TableRow,
      r2: TableRow,
    ): Int {
      val row1 = r1.modelIndex
      val row2 = r2.modelIndex
      for (directive in sortingColumns) {
        val column = directive.column
        val o1 = tableModel?.getValueAt(row1, column) as? Comparable<*>
        val o2 = tableModel?.getValueAt(row2, column) as? Comparable<*>
        val c = compareValues(o1, o2)
        if (c != 0) {
          return if (directive.direction == DESCENDING) c.inv() + 1 else c
        }
      }
      return row1 - row2
    }
  }

  private inner class TableModelHandler : TableModelListener {
    override fun tableChanged(e: TableModelEvent) {
      // If we're not sorting by anything, just pass the event along.
      when {
        !isSorting -> {
          clearSortingState()
          fireTableChanged(e)
        }
        e.firstRow == TableModelEvent.HEADER_ROW -> {
          cancelSorting()
          fireTableChanged(e)
        }
        else -> {
          val column = e.column
          val fr = e.firstRow
          val lr = e.lastRow
          val b = getSortingStatus(column) == NOT_SORTED
          if (fr == lr && column != TableModelEvent.ALL_COLUMNS && b) {
            val viewIndex = getModelToView()[fr]
            val src = this@TableSorter
            fireTableChanged(TableModelEvent(src, viewIndex, viewIndex, column, e.type))
            return
          }
          clearSortingState()
          fireTableDataChanged()
        }
      }
    }
  }

  private inner class MouseHandler : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val header = e.component as? JTableHeader ?: return
      val table = header.table
      val viewColumn = table.columnAtPoint(e.point)
      val column = table.convertColumnIndexToModel(viewColumn)
      if (column != -1) {
        val keyCol = 0
        val list = mutableListOf<Any?>()
        val intList = table.selectedRows
        for (i in intList.indices.reversed()) {
          list.add(tableModel?.getValueAt(modelIndex(intList[i]), keyCol))
        }
        var status = getSortingStatus(column) + if (e.isShiftDown) -1 else 1
        if (!e.isControlDown) {
          cancelSorting()
        }
        status = (status + 4) % 3 - 1
        setSortingStatus(column, status)
        loadSelectedRow(table, list, keyCol)
      }
    }
  }

  fun loadSelectedRow(
    table: JTable,
    list: List<Any?>,
    keyColIndex: Int,
  ) {
    if (list.isEmpty()) {
      return
    }
    val rc = tableModel?.rowCount ?: return
    for (i in 0 until rc) {
      if (list.contains(tableModel?.getValueAt(modelIndex(i), keyColIndex))) {
        table.selectionModel.addSelectionInterval(i, i)
      }
    }
  }

  companion object {
    const val DESCENDING = -1
    const val NOT_SORTED = 0

    // const val ASCENDING = 1
    private val EMPTY_DIRECTIVE = Directive(-1, NOT_SORTED)
    // val LEXICAL_COMP: Comparator<Any> = LexicalComparator()
  }
}

private class SortableHeaderRenderer(val cellRenderer: TableCellRenderer) : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component = cellRenderer.getTableCellRendererComponent(
    table,
    value,
    isSelected,
    hasFocus,
    row,
    column,
  ).also {
    val sorter = table.model
    if (it is JLabel && sorter is TableSorter) {
      val mi = table.convertColumnIndexToModel(column)
      it.icon = sorter.getHeaderRendererIcon(mi, it.font.size)
      it.horizontalTextPosition = SwingConstants.LEFT
    }
  }
}

// private class LexicalComparator : Comparator<Any>, Serializable {
//   override fun compare(o1: Any, o2: Any) = o1.toString().compareTo(o2.toString())
//
//   companion object {
//     private const val serialVersionUID = 1L
//   }
// }

private class Arrow(
  private val descending: Boolean,
  private val size: Int,
  private val priority: Int,
) : Icon, Serializable {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val color1 = c?.background ?: Color.GRAY
    val color2: Color
    // In a compound sort, make each successive triangle 20%
    // smaller than the previous one.
    val dx = (size / 2.0 * .8.pow(priority.toDouble())).toInt()
    val dy: Int
    val d: Int
    val shift: Int
    if (descending) {
      color2 = color1.darker().darker()
      shift = 1
      dy = dx
      d = -dy // Align icon (roughly) with font baseline.
    } else {
      color2 = color1.brighter().brighter()
      shift = -1
      dy = -dx
      d = 0 // Align icon (roughly) with font baseline.
    }
    val ty = y + 5 * size / 6 + d
    g.translate(x, ty)

    // Right diagonal.
    g.color = color1.darker()
    g.drawLine(dx / 2, dy, 0, 0)
    g.drawLine(dx / 2, dy + shift, 0, shift)

    // Left diagonal.
    g.color = color1.brighter()
    g.drawLine(dx / 2, dy, dx, 0)
    g.drawLine(dx / 2, dy + shift, dx, shift)

    // Horizontal line.
    g.color = color1
    g.drawLine(dx, 0, 0, 0)
    g.color = color2
    g.translate(-x, -ty)
  }

  override fun getIconWidth() = size

  override fun getIconHeight() = size

  companion object {
    private const val serialVersionUID = 1L
  }
}

private class Directive(val column: Int, val direction: Int) : Serializable {
  companion object {
    private const val serialVersionUID = 1L
  }
}

private class TableRow(val modelIndex: Int) : Serializable {
  companion object {
    private const val serialVersionUID = 1L
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
