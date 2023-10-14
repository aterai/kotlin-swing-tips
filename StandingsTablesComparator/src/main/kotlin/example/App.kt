package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val table = makeTable(makeModel())
  table.setDefaultRenderer(RowData::class.java, makeRenderer())
  val rs = table.rowSorter
  if (rs is TableRowSorter<*>) {
    rs.setComparator(0, Comparator.comparing(RowData::position))
    rs.setComparator(1, Comparator.comparing(RowData::team))
    rs.setComparator(2, Comparator.comparing(RowData::matches))
    rs.setComparator(3, Comparator.comparing(RowData::wins))
    rs.setComparator(4, Comparator.comparing(RowData::draws))
    rs.setComparator(5, Comparator.comparing(RowData::losses))
    rs.setComparator(6, Comparator.comparing(RowData::goalsFor))
    rs.setComparator(7, Comparator.comparing(RowData::goalsAgainst))
    rs.setComparator(8, Comparator.comparing(RowData::goalDifference))
    val c9 = Comparator.comparing(RowData::points).thenComparing(RowData::goalDifference)
    rs.setComparator(9, c9)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeRenderer(): DefaultTableCellRenderer {
  return object : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
      table: JTable,
      value: Any,
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
      if (c is JLabel && value is RowData) {
        val l = c
        val col = table.convertColumnIndexToModel(column)
        l.setHorizontalAlignment(if (col == 1) LEADING else CENTER)
        l.setText(getColumnText(value, col))
      }
      return c
    }
  }
}

private fun getColumnText(v: RowData, column: Int) =
  when (column) {
    0 -> v.position.toString()
    1 -> v.team
    2 -> v.matches.toString()
    3 -> v.wins.toString()
    4 -> v.draws.toString()
    5 -> v.losses.toString()
    6 -> v.goalsFor.toString()
    7 -> v.goalsAgainst.toString()
    8 -> v.goalDifference.let { if (it > 0) "+$it" else it.toString() }
    else -> v.points.toString()
  }

private fun makeTable(model: TableModel) =
  object : JTable(model) {
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int): Component {
      val c = super.prepareRenderer(renderer, row, column)
      val isSelected = isRowSelected(row)
      if (!isSelected) {
        val data = model.getValueAt(convertRowIndexToModel(row), 0) as RowData
        val num = data.position
        val promotion = num <= 2
        val promotionPlayOff = num <= 6
        val relegation = num >= 21
        c.background = when {
          promotion -> Color(0xCF_F3_C0)
          promotionPlayOff -> Color(0xCB_F7_F5)
          relegation -> Color(0xFB_DC_DC)
          row % 2 == 0 -> Color.WHITE
          else -> Color(0xF0_F0_F0)
        }
      }
      c.setForeground(Color.BLACK)
      if (c is JLabel && column != 1) {
        c.setHorizontalAlignment(SwingConstants.CENTER)
      }
      return c
    }

    override fun isCellEditable(row: Int, column: Int) = false

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

private fun initTableHeader(table: JTable) {
  val header = table.tableHeader
  (header.defaultRenderer as JLabel).setHorizontalAlignment(SwingConstants.CENTER)
  val columnModel = table.columnModel
  for (i in 0 until columnModel.columnCount) {
    val isNotTeam = i != 1
    if (isNotTeam) {
      columnModel.getColumn(i).setMaxWidth(26)
    }
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("#", "Team", "MP", "W", "D", "L", "F", "A", "GD", "P")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = RowData::class.java
  }
  addRow(model, RowData(1, "Machida", 20, 7, 6, 57, 27))
  addRow(model, RowData(2, "Iwata", 17, 11, 7, 61, 39))
  addRow(model, RowData(3, "Shimizu", 16, 12, 6, 61, 27))
  addRow(model, RowData(4, "Tokyo", 17, 9, 9, 47, 26))
  addRow(model, RowData(5, "Nagasaki", 15, 10, 10, 58, 43))
  addRow(model, RowData(6, "Chiba", 15, 9, 11, 46, 44))
  addRow(model, RowData(7, "Kofu", 15, 7, 13, 49, 43))
  addRow(model, RowData(8, "Okayama", 12, 15, 8, 43, 37))
  addRow(model, RowData(9, "Yamagata", 16, 3, 16, 53, 49))
  addRow(model, RowData(10, "Oita", 14, 9, 12, 46, 49))
  addRow(model, RowData(11, "Gunma", 12, 12, 8, 36, 30))
  addRow(model, RowData(12, "Mito", 11, 12, 12, 45, 53))
  addRow(model, RowData(13, "Tochigi", 10, 12, 13, 35, 35))
  addRow(model, RowData(14, "Tokushima", 8, 17, 10, 39, 46))
  addRow(model, RowData(15, "Akita", 9, 13, 12, 27, 36))
  addRow(model, RowData(16, "Sendai", 10, 10, 15, 40, 50))
  addRow(model, RowData(17, "Fujieda", 11, 7, 15, 46, 57))
  addRow(model, RowData(18, "Kumamoto", 9, 10, 16, 42, 45))
  addRow(model, RowData(19, "Iwaki", 9, 10, 15, 33, 51))
  addRow(model, RowData(20, "Yamaguchi", 8, 12, 15, 28, 55))
  addRow(model, RowData(21, "Kanazawa", 9, 5, 19, 35, 55))
  addRow(model, RowData(22, "Omiya", 7, 6, 22, 30, 60))
  return model
}

private fun addRow(model: DefaultTableModel, data: RowData) {
  model.addRow((0..9).map { data }.toTypedArray())
}

@Suppress("LongParameterList")
private data class RowData(
  val position: Int,
  val team: String,
  val wins: Int,
  val draws: Int,
  val losses: Int,
  val goalsFor: Int,
  val goalsAgainst: Int,
) {
  val goalDifference: Int
    get() = goalsFor - goalsAgainst

  val points: Int
    get() = wins * 3 + draws

  val matches: Int
    get() = wins + draws + losses
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
