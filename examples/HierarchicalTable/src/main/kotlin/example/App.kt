package example

import java.awt.*
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.swing.*
import javax.swing.border.Border
import javax.swing.plaf.basic.BasicTableHeaderUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel
import javax.swing.table.TableModel

fun createUI(): Component {
  val table = object : JTable(createModel()) {
    override fun createDefaultTableHeader() = createTableHeader(getColumnModel())

    override fun updateUI() {
      super.updateUI()
      // Horizontal lines are painted by the MatteBorder of each cell
      setShowHorizontalLines(false)
      setShowVerticalLines(true)
      intercellSpacing = Dimension(1, 0)
      gridColor = HierarchicalCellRenderer.LINE_COLOR
      rowSelectionAllowed = false
      isFocusable = false
      setDefaultRenderer(Any::class.java, ValueCellRenderer())
      setDefaultRenderer(RowNode::class.java, HierarchicalCellRenderer())
    }
  }
  table.rowHeight = 20
  table.tableHeader.resizingAllowed = false
  table.columnModel.getColumn(0).preferredWidth = 180
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTableHeader(cm: TableColumnModel): JTableHeader {
  val month = ColumnGroup("2026/06")
  month.add(cm.getColumn(1))
  month.add(cm.getColumn(2))
  val total = ColumnGroup("2026/04 - 2026/06")
  total.add(cm.getColumn(3))
  total.add(cm.getColumn(4))
  val header = GroupableTableHeader(cm)
  header.addColumnGroup(month)
  header.addColumnGroup(total)
  return header
}

private fun createModel(): TableModel {
  val columnNames = arrayOf("", "Result", "YoY", "Result", "YoY")
  val data = arrayOf<Array<Any?>>(
    arrayOf(RowNode("Shipments (k units)", 0), 962, 67.7, 2023, 76.9),
    arrayOf(RowNode("Desktop PC", 1), 81, 60.4, 204, 65.5),
    arrayOf(RowNode("All-in-one", 2), 8, 57.9, 36, 87.3),
    arrayOf(RowNode("Standalone", 2), 73, 60.7, 169, 62.3),
    arrayOf(RowNode("Notebook PC", 1), 881, 68.4, 1819, 78.5),
    arrayOf(RowNode("(Share)", 1, true), 91.6, null, 89.9, null),
    arrayOf(RowNode("Mobile notebook", 2), 550, 57.5, 1012, 66.3),
    arrayOf(RowNode("Other notebook", 2), 331, 100.1, 807, 102.1),
    arrayOf(RowNode("Shipment value (100M yen)", 0), 1203, 92.1, 2672, 95.3),
    arrayOf(RowNode("Desktop PC", 1), 138, 83.9, 331, 80.3),
    arrayOf(RowNode("All-in-one", 2), 17, 75.7, 69, 97.0),
    arrayOf(RowNode("Standalone", 2), 121, 85.2, 262, 76.9),
    arrayOf(RowNode("Notebook PC", 1), 1065, 93.2, 2341, 97.8),
    arrayOf(RowNode("Mobile notebook", 2), 613, 81.8, 1196, 85.8),
    arrayOf(RowNode("Other notebook", 2), 452, 115.2, 1145, 114.5),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column == 0) RowNode::class.java else Any::class.java

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
}

/**
 * Row header node.
 *
 * @param title the row title
 * @param depth the hierarchy level, 0 is the top level
 * @param isContinuation true if this row continues the previous row (no top line)
 */
private data class RowNode(
  val title: String,
  val depth: Int,
  val isContinuation: Boolean = false,
) {
  override fun toString() = title
}

private class HierarchicalCellRenderer : DefaultTableCellRenderer() {
  // The x coordinate where the bottom line starts, or -1 if there is no bottom line
  private var bottomLineX = -1

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    super.getTableCellRendererComponent(table, value, false, false, row, column)
    bottomLineX = -1
    if (value is RowNode) {
      text = value.title
      val depth = value.depth
      var b: Border = BorderFactory.createCompoundBorder(
        BorderFactory.createMatteBorder(0, 1, 0, 0, LINE_COLOR),
        BorderFactory.createEmptyBorder(0, 4, 1, 4),
      )
      if (depth > 0) {
        val guide = BorderFactory.createMatteBorder(0, INDENT * depth, 0, 0, GUIDE_ICON)
        b = BorderFactory.createCompoundBorder(guide, b)
      }
      border = b
      if (hasBottomLine(table, row)) {
        // The bottom line starts at the indent of the next row so that
        // it looks like the top line of the next row
        bottomLineX = INDENT * getNextDepth(table, row)
      }
    }
    return this
  }

  override fun paint(g: Graphics) {
    super.paint(g)
    if (bottomLineX >= 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = LINE_COLOR
      val y = height - 1
      g2.drawLine(bottomLineX, y, width - 1, y)
      g2.dispose()
    }
  }

  companion object {
    val LINE_COLOR: Color = Color.GRAY
    const val INDENT = 16

    // Tiled by the MatteBorder to paint the vertical lines of the ancestor levels
    private val GUIDE_ICON = GuideIcon(INDENT, LINE_COLOR)

    private fun getNextDepth(
      table: JTable,
      row: Int,
    ): Int {
      val next = row + 1
      val o = if (next < table.rowCount) table.getValueAt(next, 0) else null
      return (o as? RowNode)?.depth ?: 0
    }

    // The line between the header and the first row is painted by the JTableHeader,
    // and each row paints its bottom line unless the next row continues this row
    fun hasBottomLine(
      table: JTable,
      row: Int,
    ): Boolean {
      val next = row + 1
      if (next >= table.rowCount) {
        return true
      }
      val o = table.getValueAt(next, 0)
      return !(o is RowNode && o.isContinuation)
    }
  }
}

private class ValueCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    super.getTableCellRendererComponent(table, value, false, false, row, column)
    horizontalAlignment = RIGHT
    when (value) {
      is Int -> text = "%,d".format(value)
      is Double -> text = "%.1f%%".format(value)
    }
    val bottom = if (HierarchicalCellRenderer.hasBottomLine(table, row)) 1 else 0
    val color = HierarchicalCellRenderer.LINE_COLOR
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(0, 0, bottom, 0, color),
      BorderFactory.createEmptyBorder(0, 4, 1 - bottom, 4),
    )
    return this
  }
}

private class HeaderCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable?,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    super.getTableCellRendererComponent(table, value, false, false, row, column)
    horizontalAlignment = CENTER
    table?.tableHeader?.also {
      foreground = it.foreground
      background = it.background
      font = it.font
    }
    val left = if (column == 0) 1 else 0
    // The column cells (column >= 0) paint the bottom line that separates the header
    // from the table body, the group cells (column == -1) leave it to the column cells
    val bottom = if (column >= 0) 1 else 0
    val color = HierarchicalCellRenderer.LINE_COLOR
    border = BorderFactory.createCompoundBorder(
      BorderFactory.createMatteBorder(1, left, bottom, 1, color),
      BorderFactory.createEmptyBorder(2, 4, 2, 4),
    )
    return this
  }
}

private class GuideIcon(
  private val width: Int,
  private val color: Color,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = color
    g2.drawLine(x, y, x, y + iconHeight - 1)
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = 1
}

/**
 * GroupableTableHeader.
 * @see [GroupableTableHeader](http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html)
 *
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 * @author aterai aterai@outlook.com
 */
private class GroupableTableHeader(
  model: TableColumnModel,
) : JTableHeader(model) {
  private val columnGroups = mutableListOf<ColumnGroup>()

  override fun updateUI() {
    super.updateUI()
    // Replacing the UI delegate uninstalls the LookAndFeel specific header renderer
    // (Synth: HeaderRenderer, Windows: XPDefaultRenderer), so use a renderer that
    // paints the same cell lines as the table body on every LookAndFeel
    setUI(GroupableTableHeaderUI())
    defaultRenderer = HeaderCellRenderer()
  }

  // [java] BooleanGetMethodName: Don't report bad method names on @Override #97
  // https://github.com/pmd/pmd/pull/97
  override fun getReorderingAllowed() = false

  override fun setReorderingAllowed(b: Boolean) {
    super.setReorderingAllowed(false)
  }

  fun addColumnGroup(g: ColumnGroup) {
    columnGroups.add(g)
  }

  fun getColumnGroups(col: TableColumn) =
    columnGroups
      .map { it.getColumnGroupList(col, mutableListOf()) }
      .firstOrNull { it.isNotEmpty() }
      .orEmpty()

  @Throws(IOException::class)
  private fun writeObject(stream: ObjectOutputStream) {
    stream.defaultWriteObject()
  }

  @Throws(IOException::class, ClassNotFoundException::class)
  private fun readObject(stream: ObjectInputStream) {
    stream.defaultReadObject()
  }
}

/**
 * GroupableTableHeaderUI.
 * @see [GroupableTableHeaderUI](http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html)
 *
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 * @author aterai aterai@outlook.com
 */
private class GroupableTableHeaderUI : BasicTableHeaderUI() {
  override fun paint(
    g: Graphics,
    c: JComponent?,
  ) {
    val clip = g.clipBounds
    val cm = header.columnModel
    val headerHeight = header.height
    // Always lay out the cells from the first column so that a group cell starts at
    // the left edge of its first column even if the clip bounds start in the middle
    // of the group, and paint only the cells that intersect the clip bounds
    val groupRects = hashMapOf<ColumnGroup, Rectangle>()
    val cellRect = Rectangle()
    for (column in 0..<cm.columnCount) {
      val tc = cm.getColumn(column)
      cellRect.setBounds(cellRect.x, 0, tc.width, headerHeight)
      paintGroupCells(g, clip, tc, cellRect, groupRects)
      if (cellRect.intersects(clip)) {
        paintColumnCell(g, cellRect, column)
      }
      cellRect.x += cellRect.width
    }
  }

  // Paints the group cells that start at the given column, and shrinks the cellRect
  // so that it covers only the column cell below the groups
  private fun paintGroupCells(
    g: Graphics,
    clip: Rectangle,
    tc: TableColumn,
    cellRect: Rectangle,
    groupRects: MutableMap<ColumnGroup, Rectangle>,
  ) {
    for (cg in getColumnGroups(tc)) {
      var groupRect = groupRects[cg]
      if (groupRect == null) {
        // The first column of the group: paint the group cell only once
        groupRect = Rectangle(cellRect.location, cg.getSize(header))
        groupRects[cg] = groupRect
        if (groupRect.intersects(clip)) {
          paintGroupCell(g, groupRect, cg)
        }
      }
      cellRect.y += groupRect.height
      cellRect.height -= groupRect.height
    }
  }

  private fun getColumnGroups(tc: TableColumn) =
    (header as? GroupableTableHeader)?.getColumnGroups(tc).orEmpty()

  // Copied from javax/swing/plaf/basic/BasicTableHeaderUI.java
  private fun getHeaderRendererComponent(columnIndex: Int): Component {
    val tc = header.columnModel.getColumn(columnIndex)
    val r = tc.headerRenderer ?: header.defaultRenderer
    val hasFocus = !header.isPaintingForPrint && header.hasFocus()
    // && (columnIndex == getSelectedColumnIndex())
    val table = header.table
    return r.getTableCellRendererComponent(
      table,
      tc.headerValue,
      false,
      hasFocus,
      -1,
      columnIndex,
    )
  }

  // Copied from javax/swing/plaf/basic/BasicTableHeaderUI.java
  private fun paintColumnCell(
    g: Graphics,
    rect: Rectangle,
    columnIndex: Int,
  ) {
    val c = getHeaderRendererComponent(columnIndex)
    rendererPane.paintComponent(
      g,
      c,
      header,
      rect.x,
      rect.y,
      rect.width,
      rect.height,
      true,
    )
  }

  private fun paintGroupCell(
    g: Graphics,
    rect: Rectangle,
    columnGroup: ColumnGroup,
  ) {
    val r = header.defaultRenderer
    val v = columnGroup.headerValue
    val c = r.getTableCellRendererComponent(header.table, v, false, false, -1, -1)
    rendererPane.paintComponent(
      g,
      c,
      header,
      rect.x,
      rect.y,
      rect.width,
      rect.height,
      true,
    )
  }

  private fun getPreferredHeaderHeight(): Int {
    var height = 0
    val columnModel = header.columnModel
    for (column in 0..<columnModel.columnCount) {
      val tc = columnModel.getColumn(column)
      val comp = getHeaderRendererComponent(column)
      var rendererHeight = comp.preferredSize.height
      for (cg in getColumnGroups(tc)) {
        rendererHeight += cg.getSize(header).height
      }
      height = maxOf(height, rendererHeight)
    }
    return height
  }

  override fun getPreferredSize(c: JComponent?): Dimension {
    val width = header.columnModel.columns
      .toList()
      .sumOf { it.preferredWidth.toLong() }
    val w = minOf(width, Int.MAX_VALUE.toLong()).toInt()
    return Dimension(w, getPreferredHeaderHeight())
  }
}

/**
 * ColumnGroup.
 * @see [ColumnGroup](http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html)
 *
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 * @author aterai aterai@outlook.com
 */
private class ColumnGroup(
  text: String,
) {
  private val children = mutableListOf<Any>()
  val headerValue = text

  /**
   * Add TableColumn or ColumnGroup.
   * @param obj TableColumn or ColumnGroup
   */
  fun add(obj: Any?) {
    obj?.also { children.add(it) }
  }

  fun getColumnGroupList(
    column: TableColumn,
    groups: MutableList<ColumnGroup>,
  ): List<ColumnGroup> {
    groups.add(this)
    return if (children.contains(column)) {
      groups
    } else {
      children
        .filterIsInstance<ColumnGroup>()
        .map { it.getColumnGroupList(column, groups.toMutableList()) }
        .firstOrNull { it.isNotEmpty() }
        .orEmpty()
    }
  }

  fun getSize(header: JTableHeader): Dimension {
    val headerRenderer = header.defaultRenderer
    val c = headerRenderer.getTableCellRendererComponent(
      header.table,
      headerValue,
      false,
      false,
      -1,
      -1,
    )
    var width = 0
    for (o in children) {
      width += when (o) {
        is TableColumn -> o.width
        is ColumnGroup -> o.getSize(header).width
        else -> 0
      }
    }
    return Dimension(width, c.preferredSize.height)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
