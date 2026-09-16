package example

import java.awt.*
import java.awt.geom.Line2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val table = makeTable(makeModel())
  table.autoCreateRowSorter = true
  return JPanel(BorderLayout()).also {
    it.add(JLayer(JScrollPane(table), BorderPaintLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(model: TableModel): JTable {
  return object : JTable(model) {
    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(renderer, row, column)
      if (!isRowSelected(row)) {
        val position = model.getValueAt(convertRowIndexToModel(row), 0) as? Int ?: -1
        c.background = getRowBackground(position, row)
      }
      c.foreground = Color.BLACK
      // use the model index so that the alignment survives column reordering
      val isTeamColumn = convertColumnIndexToModel(column) == 1
      if (c is JLabel) {
        c.horizontalAlignment = if (isTeamColumn) {
          SwingConstants.LEADING
        } else {
          SwingConstants.CENTER
        }
      }
      return c
    }

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false

    override fun updateUI() {
      super.updateUI()
      setFillsViewportHeight(true)
      setShowVerticalLines(false)
      setShowHorizontalLines(false)
      setIntercellSpacing(Dimension())
      setSelectionForeground(getForeground())
      setSelectionBackground(Color(0, 0, 100, 50))
      setFocusable(false)
      initTableColumns(this)
    }
  }
}

private val PROMOTION = Color(0xCF_F3_C0)
private val PROMOTION_PLAYOFF = Color(0xCB_F7_F5)
private val RELEGATION = Color(0xFB_DC_DC)
private val ODD_ROW = Color(0xF0_F0_F0)

private fun getRowBackground(
  position: Int,
  row: Int,
) = when {
  position <= 2 -> PROMOTION
  position <= 6 -> PROMOTION_PLAYOFF
  position >= 21 -> RELEGATION
  row % 2 == 0 -> Color.WHITE
  else -> ODD_ROW
}

private fun initTableColumns(table: JTable) {
  val header = table.tableHeader
  (header.defaultRenderer as? JLabel)?.setHorizontalAlignment(SwingConstants.CENTER)
  val columnModel = table.columnModel
  for (i in 0..<columnModel.columnCount) {
    if (i != 1) {
      columnModel.getColumn(i).setMaxWidth(26)
    }
  }
  // goal difference: prefix positive values with "+"
  columnModel.getColumn(8).setCellRenderer(object : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int,
    ): Component {
      val txt = if (value is Int && value > 0) "+$value" else value
      return super.getTableCellRendererComponent(
        table,
        txt,
        isSelected,
        hasFocus,
        row,
        column,
      )
    }
  })
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("#", "Team", "MP", "W", "D", "L", "F", "A", "GD", "P")
  val data = arrayOf<Array<Any>>(
    arrayOf(1, "Machida", 33, 20, 7, 6, 57, 27, +30, 67),
    arrayOf(2, "Iwata", 35, 17, 11, 7, 61, 39, +22, 62),
    arrayOf(3, "Shimizu", 34, 16, 12, 6, 61, 27, +34, 60),
    arrayOf(4, "Tokyo", 35, 17, 9, 9, 47, 26, +21, 60),
    arrayOf(5, "Nagasaki", 35, 15, 10, 10, 58, 43, +15, 55),
    arrayOf(6, "Chiba", 35, 15, 9, 11, 46, 44, +2, 54),
    arrayOf(7, "Kofu", 35, 15, 7, 13, 49, 43, +6, 52),
    arrayOf(8, "Okayama", 35, 12, 15, 8, 43, 37, +6, 51),
    arrayOf(9, "Yamagata", 35, 16, 3, 16, 53, 49, +4, 51),
    arrayOf(10, "Oita", 35, 14, 9, 12, 46, 49, -3, 51),
    arrayOf(11, "Gunma", 32, 12, 12, 8, 36, 30, +6, 48),
    arrayOf(12, "Mito", 35, 11, 12, 12, 45, 53, -8, 45),
    arrayOf(13, "Tochigi", 35, 10, 12, 13, 35, 35, +0, 42),
    arrayOf(14, "Tokushima", 35, 8, 17, 10, 39, 46, -7, 41),
    arrayOf(15, "Akita", 34, 9, 13, 12, 27, 36, -9, 40),
    arrayOf(16, "Sendai", 35, 10, 10, 15, 40, 50, -10, 40),
    arrayOf(17, "Fujieda", 33, 11, 7, 15, 46, 57, -11, 40),
    arrayOf(18, "Kumamoto", 35, 9, 10, 16, 42, 45, -3, 37),
    arrayOf(19, "Iwaki", 34, 9, 10, 15, 33, 51, -18, 37),
    arrayOf(20, "Yamaguchi", 35, 8, 12, 15, 28, 55, -27, 36),
    arrayOf(21, "Kanazawa", 33, 9, 5, 19, 35, 55, -20, 32),
    arrayOf(22, "Omiya", 35, 7, 6, 22, 30, 60, -30, 27),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private class BorderPaintLayerUI : LayerUI<JScrollPane>() {
  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val table = getTable(c) ?: return
    val key = table.rowSorter?.sortKeys?.firstOrNull()
    if (key == null || isStandingsOrder(key)) {
      paintLines(g, c, table, true)
    } else if (isReversedStandingsOrder(key)) {
      paintLines(g, c, table, false)
    }
  }

  // rows are ordered from first to last place
  private fun isStandingsOrder(key: RowSorter.SortKey): Boolean {
    val column = key.column
    val order = key.sortOrder
    return (column == POSITION_COLUMN && order == SortOrder.ASCENDING) ||
      (column == POINTS_COLUMN && order == SortOrder.DESCENDING)
  }

  // rows are ordered from last to first place
  private fun isReversedStandingsOrder(key: RowSorter.SortKey): Boolean {
    val column = key.column
    val order = key.sortOrder
    return (column == POSITION_COLUMN && order == SortOrder.DESCENDING) ||
      (column == POINTS_COLUMN && order == SortOrder.ASCENDING)
  }

  private fun paintLines(
    g: Graphics,
    layer: JComponent,
    table: JTable,
    ascending: Boolean,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    for (b in Boundary.entries) {
      g2.paint = b.color
      g2.draw(makeUnderline(layer, table, b.getViewRow(table.rowCount, ascending)))
    }
    g2.dispose()
  }

  private fun getTable(c: Component) =
    ((c as? JLayer<*>)?.view as? JScrollPane)?.viewport?.view as? JTable

  private fun makeUnderline(
    c: JComponent,
    table: JTable,
    row: Int,
  ): Line2D {
    val r0 = table.getCellRect(row, 0, false)
    val r1 = table.getCellRect(row, table.columnCount - 1, false)
    val r = SwingUtilities.convertRectangle(table, r0.union(r1), c)
    return Line2D.Double(r.minX, r.maxY, r.maxX, r.maxY)
  }

  companion object {
    private const val POSITION_COLUMN = 0
    private const val POINTS_COLUMN = 9
  }
}

// a line is drawn below the row of the last team in each zone
private enum class Boundary(
  private val lastPosition: Int,
  val color: Color,
) {
  PROMOTION(2, Color.GREEN.darker()),
  PROMOTION_PLAYOFF(6, Color.BLUE.darker()),
  SAFETY(20, Color.RED.darker()),
  ;

  fun getViewRow(
    rowCount: Int,
    ascending: Boolean,
  ) = if (ascending) lastPosition - 1 else rowCount - lastPosition - 1
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
