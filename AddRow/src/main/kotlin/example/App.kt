package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val model = RowDataModel()
    val table = object : JTable(model) {
      private val evenColor = Color(0xFA_FA_FA)
      override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component {
        val c = super.prepareRenderer(tcr, row, column)
        if (isRowSelected(row)) {
          c.setForeground(getSelectionForeground())
          c.setBackground(getSelectionBackground())
        } else {
          c.setForeground(getForeground())
          c.setBackground(if (row % 2 == 0) evenColor else getBackground())
        }
        return c
      }
    }
    val col = table.getColumnModel().getColumn(0)
    col.setMinWidth(60)
    col.setMaxWidth(60)
    col.setResizable(false)
    model.addRowData(RowData("Name 1", "comment..."))
    model.addRowData(RowData("Name 2", "Test"))
    model.addRowData(RowData("Name d", "ee"))
    model.addRowData(RowData("Name c", "Test cc"))
    model.addRowData(RowData("Name b", "Test bb"))
    model.addRowData(RowData("Name a", "ff"))
    model.addRowData(RowData("Name 0", "Test aa"))
    table.setAutoCreateRowSorter(true)
    table.setFillsViewportHeight(true)
    table.setComponentPopupMenu(TablePopupMenu())
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

class RowDataModel : DefaultTableModel() {
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
      ColumnContext("Comment", String::class.java, true)
    )
  }
}

data class RowData(val name: String, val comment: String)

class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem
  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      delete.setEnabled(c.getSelectedRowCount() > 0)
      super.show(c, x, y)
    }
  }

  init {
    add("add").addActionListener {
      (getInvoker() as? JTable)?.also { table ->
        (table.getModel() as? RowDataModel)?.also { model ->
          model.addRowData(RowData("New row", ""))
          val r = table.getCellRect(model.getRowCount() - 1, 0, true)
          table.scrollRectToVisible(r)
        }
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      (getInvoker() as? JTable)?.also { table ->
        (table.getModel() as? RowDataModel)?.also { model ->
          val selection = table.getSelectedRows()
          for (i in selection.indices.reversed()) {
            model.removeRow(table.convertRowIndexToModel(selection[i]))
          }
        }
      }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
