package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicTableHeaderUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

fun makeUI(): Component {
  // http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html
  val columnNames = arrayOf("SNo.", "1", "2", "Native", "2", "3")
  val data = arrayOf(
    arrayOf("119", "foo", "bar", "ja", "ko", "zh"),
    arrayOf("911", "bar", "foo", "en", "fr", "pt")
  )
  val model = DefaultTableModel(data, columnNames)
  val table = object : JTable(model) {
    override fun createDefaultTableHeader(): JTableHeader {
      val cm = getColumnModel()
      val gname = ColumnGroup("Name")
      gname.add(cm.getColumn(1))
      gname.add(cm.getColumn(2))

      val glang = ColumnGroup("Language")
      glang.add(cm.getColumn(3))

      val gother = ColumnGroup("Others")
      gother.add(cm.getColumn(4))
      gother.add(cm.getColumn(5))

      glang.add(gother)

      val header = GroupableTableHeader(cm)
      header.addColumnGroup(gname)
      header.addColumnGroup(glang)
      return header
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

/**
 * GroupableTableHeader.
 * @see [GroupableTableHeader](http://www2.gol.com/users/tame/swing/examples/JTableExamples1.html)
 *
 * @version 1.0 10/20/98
 * @author Nobuo Tamemasa
 * @author aterai aterai@outlook.com
 */
private class GroupableTableHeader(model: TableColumnModel) : JTableHeader(model) {
  private val columnGroups = mutableListOf<ColumnGroup>()

  override fun updateUI() {
    super.updateUI()
    setUI(GroupableTableHeaderUI())
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

  fun getColumnGroups(col: TableColumn): List<*> {
    for (cg in columnGroups) {
      val groups = cg.getColumnGroupList(col, mutableListOf())
      if (groups.isNotEmpty()) {
        return groups
      }
    }
    return emptyList<Any>()
  }

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
  override fun paint(g: Graphics, c: JComponent?) {
    val clip = g.clipBounds
    // val left = clip.getLocation()
    // val right = Point(clip.x + clip.width - 1, clip.y)
    val cm = header.columnModel
    val colMin = header.columnAtPoint(clip.location)
    val colMax = header.columnAtPoint(Point(clip.x + clip.width - 1, clip.y))

    val cellRect = header.getHeaderRect(colMin)
    val headerY = cellRect.y
    val headerHeight = cellRect.height

    val map = hashMapOf<ColumnGroup, Rectangle>()
    // int columnMargin = header.getColumnModel().getColumnMargin();
    // int columnWidth;
    for (column in colMin..colMax) {
      val tc = cm.getColumn(column)
      cellRect.y = headerY
      cellRect.setSize(tc.width, headerHeight)

      var groupHeight = 0
      val cglist = (header as? GroupableTableHeader)?.getColumnGroups(tc).orEmpty()
      for (o in cglist) {
        val cg = o as? ColumnGroup ?: continue
        val groupRect = map[cg] ?: Rectangle(cellRect.location, cg.getSize(header)).also {
          map[cg] = it
        }
        paintCellGroup(g, groupRect, cg)
        groupHeight += groupRect.height
        cellRect.height = headerHeight - groupHeight
        cellRect.y = groupHeight
      }
      paintCell(g, cellRect, column)
      cellRect.x += cellRect.width
    }
  }

  // Copied from javax/swing/plaf/basic/BasicTableHeaderUI.java
  private fun getHeaderRenderer(columnIndex: Int): Component {
    val tc = header.columnModel.getColumn(columnIndex)
    val r = tc.headerRenderer ?: header.defaultRenderer
    val hasFocus = !header.isPaintingForPrint && header.hasFocus()
    // && (columnIndex == getSelectedColumnIndex())
    val table = header.table
    return r.getTableCellRendererComponent(table, tc.headerValue, false, hasFocus, -1, columnIndex)
  }

  // Copied from javax/swing/plaf/basic/BasicTableHeaderUI.java
  private fun paintCell(g: Graphics, cellRect: Rectangle, columnIndex: Int) {
    val c = getHeaderRenderer(columnIndex)
    rendererPane.paintComponent(g, c, header, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true)
  }

  private fun paintCellGroup(g: Graphics, cellRect: Rectangle, columnGroup: ColumnGroup) {
    val r = header.defaultRenderer
    val c = r.getTableCellRendererComponent(header.table, columnGroup.headerValue, false, false, -1, -1)
    rendererPane.paintComponent(g, c, header, cellRect.x, cellRect.y, cellRect.width, cellRect.height, true)
  }

  private fun getHeaderHeight(): Int {
    var height = 0
    val columnModel = header.columnModel
    for (column in 0 until columnModel.columnCount) {
      val tc = columnModel.getColumn(column)
      val comp = getHeaderRenderer(column)
      var rendererHeight = comp.preferredSize.height
      val cglist = (header as? GroupableTableHeader)?.getColumnGroups(tc).orEmpty()
      for (o in cglist) {
        val cg = o as? ColumnGroup ?: continue
        rendererHeight += cg.getSize(header).height
      }
      height = maxOf(height, rendererHeight)
    }
    return height
  }

  // Copied from javax/swing/plaf/basic/BasicTableHeaderUI.java
  // private fun createHeaderSize(width: Long): Dimension {
  //   val w = minOf(width, Integer.MAX_VALUE.toLong())
  //   return Dimension(w.toInt(), getHeaderHeight())
  // }

  override fun getPreferredSize(c: JComponent?): Dimension {
    val width = header.columnModel.columns.toList().map { it.preferredWidth.toLong() }.sum()
    return Dimension(minOf(width, Integer.MAX_VALUE.toLong()).toInt(), getHeaderHeight())
    // return createHeaderSize(width)
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
private class ColumnGroup(text: String) {
  private val list = mutableListOf<Any>()
  val headerValue = text

  /**
   * Add TableColumn or ColumnGroup.
   * @param obj TableColumn or ColumnGroup
   */
  fun add(obj: Any?) {
    obj?.also { list.add(it) }
  }

  fun getColumnGroupList(c: TableColumn, g: MutableList<Any>): List<*> {
    g.add(this)
    // if (list.contains(c)) {
    //   return g
    // }
    // for (obj in list) {
    //   val cg = obj as? ColumnGroup ?: continue
    //   val groups = cg.getColumnGroupList(c, MutableList<Any>(g))
    //   if (!groups.isEmpty()) {
    //     return groups
    //   }
    // }
    // return emptyList<Any>()
    return when {
      list.contains(c) -> g
      else -> list.filterIsInstance<ColumnGroup>().map {
        it.getColumnGroupList(c, ArrayList(g))
      }.firstOrNull { it.isNotEmpty() }.orEmpty()
    }
  }

  fun getSize(header: JTableHeader): Dimension {
    val r = header.defaultRenderer
    val c = r.getTableCellRendererComponent(header.table, headerValue, false, false, -1, -1)
    var width = 0
    for (o in list) {
      // width += if (o is TableColumn) o.getWidth() else (o as ColumnGroup).getSize(header).width
      width += (o as? TableColumn)?.width ?: (o as? ColumnGroup)?.getSize(header)?.width ?: 0
    }
    return Dimension(width, c.preferredSize.height)
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
