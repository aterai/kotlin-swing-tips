package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

private const val BUTTON_COLUMN = 3

fun makeUI(): Component {
  val model = RowDataModel().also {
    it.addRowData(RowData("Name 1", "Comment..."))
    it.addRowData(RowData("Name 2", "Test"))
    it.addRowData(RowData("Name d", "ee"))
    it.addRowData(RowData("Name c", "Test cc"))
    it.addRowData(RowData("Name b", "Test bb"))
    it.addRowData(RowData("Name a", "ff"))
    it.addRowData(RowData("Name 0", "Test aa"))
    it.addRowData(RowData("Name 0", "gg"))
  }

  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val cm = columnModel
      cm.getColumn(0).also {
        it.minWidth = 60
        it.maxWidth = 60
        it.resizable = false
      }
      cm.getColumn(BUTTON_COLUMN).also {
        it.cellRenderer = DeleteButtonRenderer()
        it.cellEditor = DeleteButtonEditor()
        it.minWidth = 20
        it.maxWidth = 20
        it.resizable = false
      }
    }
  }

  // val sorter: TableRowSorter<out TableModel> = TableRowSorter(model)
  val sorter = TableRowSorter(model)
  table.rowSorter = sorter
  sorter.setSortable(BUTTON_COLUMN, false)

  val button = JButton("add")
  button.addActionListener {
    model.addRowData(RowData("Test", "************"))
  }

  return JPanel(BorderLayout()).also {
    it.add(button, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowDataModel : DefaultTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    val obj = arrayOf(number, t.name, t.comment, "")
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
    val isEditable: Boolean,
  )

  companion object {
    private val COLUMN_ARRAY = arrayOf(
      ColumnContext("No.", Number::class.java, false),
      ColumnContext("Name", String::class.java, true),
      ColumnContext("Comment", String::class.java, true),
      ColumnContext("", String::class.java, true),
    )
  }
}

private data class RowData(val name: String, val comment: String)

private class DeleteButton : JButton() {
  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder()
    isFocusable = false
    isRolloverEnabled = false
    text = "X"
  }
}

private class DeleteButtonRenderer : TableCellRenderer {
  private val renderer = DeleteButton()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ) = renderer
}

private class DeleteButtonEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = DeleteButton()

  init {
    renderer.addActionListener {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, renderer) as? JTable)?.also { table ->
        val row = table.convertRowIndexToModel(table.editingRow)
        fireEditingStopped()
        (table.model as? DefaultTableModel)?.removeRow(row)
      }
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ) = renderer

  override fun getCellEditorValue() = ""
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
