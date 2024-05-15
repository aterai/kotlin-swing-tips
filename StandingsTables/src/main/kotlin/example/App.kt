package example

import java.awt.*
import java.awt.geom.Line2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = makeTable(makeModel())
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
      val isSelected = isRowSelected(row)
      if (!isSelected) {
        val num = model.getValueAt(convertRowIndexToModel(row), 0) as? Int ?: -1
        val bgc = when {
          num <= 2 -> 0xCF_F3_C0
          num <= 6 -> 0xCB_F7_F5
          num >= 21 -> 0xFB_DC_DC
          row % 2 == 0 -> 0xFF_FF_FF
          else -> 0xF0_F0_F0
        }
        c.setBackground(Color(bgc))
      }
      c.setForeground(Color.BLACK)
      if (c is JLabel && column != 1) {
        c.setHorizontalAlignment(SwingConstants.CENTER)
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
      setAutoCreateRowSorter(true)
      setFocusable(false)
      initTableHeader(this)
    }
  }
}

private fun initTableHeader(table: JTable) {
  val header = table.tableHeader
  (header.defaultRenderer as JLabel).setHorizontalAlignment(SwingConstants.CENTER)
  val columnModel = table.columnModel
  for (i in 0 until columnModel.columnCount) {
    if (i != 1) {
      columnModel.getColumn(i).setMaxWidth(26)
    }
  }
  columnModel.getColumn(8).setCellRenderer(object : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable,
      value: Any?,
      isSelected: Boolean,
      hasFocus: Boolean,
      row: Int,
      column: Int,
    ): Component {
      val sv = value?.toString() ?: ""
      var txt = if (sv.startsWith("-")) sv else "+$sv"
      if ("+0" == txt) {
        txt = "0"
      }
      setHorizontalAlignment(RIGHT)
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
  val data = arrayOf(
    arrayOf<Any>(1, "Machida", 33, 20, 7, 6, 57, 27, +30, 67),
    arrayOf<Any>(2, "Iwata", 35, 17, 11, 7, 61, 39, +22, 62),
    arrayOf<Any>(3, "Shimizu", 34, 16, 12, 6, 61, 27, +34, 60),
    arrayOf<Any>(4, "Tokyo", 35, 17, 9, 9, 47, 26, +21, 60),
    arrayOf<Any>(5, "Nagasaki", 35, 15, 10, 10, 58, 43, +15, 55),
    arrayOf<Any>(6, "Chiba", 35, 15, 9, 11, 46, 44, +2, 54),
    arrayOf<Any>(7, "Kofu", 35, 15, 7, 13, 49, 43, +6, 52),
    arrayOf<Any>(8, "Okayama", 35, 12, 15, 8, 43, 37, +6, 51),
    arrayOf<Any>(9, "Yamagata", 35, 16, 3, 16, 53, 49, +4, 51),
    arrayOf<Any>(10, "Oita", 35, 14, 9, 12, 46, 49, -3, 51),
    arrayOf<Any>(11, "Gunma", 32, 12, 12, 8, 36, 30, +6, 48),
    arrayOf<Any>(12, "Mito", 35, 11, 12, 12, 45, 53, -8, 45),
    arrayOf<Any>(13, "Tochigi", 35, 10, 12, 13, 35, 35, +0, 42),
    arrayOf<Any>(14, "Tokushima", 35, 8, 17, 10, 39, 46, -7, 41),
    arrayOf<Any>(15, "Akita", 34, 9, 13, 12, 27, 36, -9, 40),
    arrayOf<Any>(16, "Sendai", 35, 10, 10, 15, 40, 50, -10, 40),
    arrayOf<Any>(17, "Fujieda", 33, 11, 7, 15, 46, 57, -11, 40),
    arrayOf<Any>(18, "Kumamoto", 35, 9, 10, 16, 42, 45, -3, 37),
    arrayOf<Any>(19, "Iwaki", 34, 9, 10, 15, 33, 51, -18, 37),
    arrayOf<Any>(20, "Yamaguchi", 35, 8, 12, 15, 28, 55, -27, 36),
    arrayOf<Any>(21, "Kanazawa", 33, 9, 5, 19, 35, 55, -20, 32),
    arrayOf<Any>(22, "Omiya", 35, 7, 6, 22, 30, 60, -30, 27),
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
    val table = getTable(c)
    val sorter = table?.rowSorter
    if (sorter != null) {
      val keys = sorter.sortKeys
      val column = if (keys.isEmpty()) -1 else keys[0].column
      if (column <= 0 || column == 9) {
        val g2 = g.create() as Graphics2D
        val b1 = column == 0 && keys[0].sortOrder == SortOrder.ASCENDING
        val b2 = column == 9 && keys[0].sortOrder == SortOrder.DESCENDING
        if (column < 0 || b1 || b2) {
          g2.paint = Color.GREEN.darker()
          g2.draw(makeUnderline(c, table, 2))
          g2.paint = Color.BLUE.darker()
          g2.draw(makeUnderline(c, table, 6))
          g2.paint = Color.RED.darker()
          g2.draw(makeUnderline(c, table, 20))
        } else {
          g2.paint = Color.GREEN.darker()
          g2.draw(makeUnderline(c, table, 22 - 2))
          g2.paint = Color.BLUE.darker()
          g2.draw(makeUnderline(c, table, 22 - 6))
          g2.paint = Color.RED.darker()
          g2.draw(makeUnderline(c, table, 22 - 20))
        }
        g2.dispose()
      }
    }
  }

  private fun getTable(c: Component): JTable? {
    var table: JTable? = null
    if (c is JLayer<*>) {
      val c1 = c.view
      if (c1 is JScrollPane) {
        table = c1.viewport.view as? JTable
      }
    }
    return table
  }

  private fun makeUnderline(
    c: JComponent,
    table: JTable,
    idx: Int,
  ): Line2D {
    val r0 = table.getCellRect(idx - 1, 0, false)
    val r1 = table.getCellRect(idx - 1, table.columnCount - 1, false)
    val r = SwingUtilities.convertRectangle(table, r0.union(r1), c)
    return Line2D.Double(r.minX, r.maxY, r.maxX, r.maxY)
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
