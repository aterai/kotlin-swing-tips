package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val model = RowDataModel().also {
    it.addRowData(RowData("Name 1", "comment..."))
    it.addRowData(RowData("Name 2", "Test"))
    it.addRowData(RowData("Name d", "ee"))
    it.addRowData(RowData("Name c", "Test cc"))
    it.addRowData(RowData("Name b", "Test bb"))
    it.addRowData(RowData("Name a", "ff"))
    it.addRowData(RowData("Name 0", "Test aa"))
    it.addRowData(RowData("Name 1", "comment..."))
    it.addRowData(RowData("Name 2", "Test"))
    it.addRowData(RowData("Name d", "gg"))
    it.addRowData(RowData("Name c", "Test cc"))
    it.addRowData(RowData("Name b", "Test bb"))
    it.addRowData(RowData("Name a", "hh"))
    it.addRowData(RowData("Name 0", "Test aa"))
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_FA_FA)
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
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
  }
  table.rowSelectionAllowed = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
  table.tableHeader.reorderingAllowed = false
  table.columnModel.getColumn(0).also {
    it.minWidth = 60
    it.maxWidth = 60
    it.resizable = false
  }
  val check = JCheckBox("display an odd number of rows")
  check.addActionListener { model.filterRows(check.isSelected) }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(check, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowDataModel : DefaultTableModel() {
  private val list = mutableListOf<RowData>()
  private var number = 0
  fun addRowData(t: RowData) {
    super.addRow(arrayOf(number, t.name, t.comment))
    number++
    list.add(t)
  }

  override fun isCellEditable(row: Int, col: Int) = COLUMN_ARRAY[col].isEditable

  override fun getColumnClass(column: Int) = COLUMN_ARRAY[column].columnClass

  override fun getColumnCount() = COLUMN_ARRAY.size

  override fun getColumnName(column: Int) = COLUMN_ARRAY[column].columnName

  private data class ColumnContext(
    val columnName: String,
    val columnClass: Class<*>,
    val isEditable: Boolean
  )

  fun filterRows(flg: Boolean) {
    rowCount = 0
    for (i in list.indices) {
      if (flg && i % 2 == 0) {
        continue
      }
      val t = list[i]
      addRow(convertToVector(arrayOf(i, t.name, t.comment)))
    }
    fireTableDataChanged()
  }

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Comment", String::class.java, true)
    )
  }
}

private data class RowData(val name: String, val comment: String)

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
