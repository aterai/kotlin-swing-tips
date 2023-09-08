package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val model = RowDataModel()
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
  val col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false
  model.addRowData(RowData("Name 1", "comment..."))
  model.addRowData(RowData("Name 2", "Test"))
  model.addRowData(RowData("Name d", "ee"))
  model.addRowData(RowData("Name c", "Test cc"))
  model.addRowData(RowData("Name b", "Test bb"))
  model.addRowData(RowData("Name a", "ff"))
  model.addRowData(RowData("Name 0", "Test aa"))
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowDataModel : DefaultTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    val obj = arrayOf(number, t.name, t.comment)
    super.addRow(obj)
    number++
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

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Comment", String::class.java, true),
    )
  }
}

private data class RowData(val name: String, val comment: String)

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      (invoker as? JTable)?.also { table ->
        (table.model as? RowDataModel)?.also { model ->
          model.addRowData(RowData("New row", ""))
          val r = table.getCellRect(model.rowCount - 1, 0, true)
          table.scrollRectToVisible(r)
        }
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      (invoker as? JTable)?.also { table ->
        (table.model as? RowDataModel)?.also { model ->
          val selection = table.selectedRows
          for (i in selection.indices.reversed()) {
            model.removeRow(table.convertRowIndexToModel(selection[i]))
          }
        }
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
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
