package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter
import kotlin.collections.HashSet

private val check1 = JCheckBox("!comment.isEmpty()")
private val check2 = JCheckBox("idx % 2 == 0")

private fun canAddRow() = !check1.isSelected && !check2.isSelected

private class TablePopupMenu : JPopupMenu() {
  private val addMenuItem = add("add")
  private val deleteMenuItem: JMenuItem

  init {
    addMenuItem.addActionListener {
      (invoker as? JTable)?.also {
        (it.model as? RowDataModel)?.addRowData(RowData("example", ""))
      }
    }
    addSeparator()
    deleteMenuItem = add("delete")
    deleteMenuItem.addActionListener {
      val tbl = invoker as? JTable
      val m = tbl?.model as? DefaultTableModel
      if (m != null) {
        val selection = tbl.selectedRows
        for (i in selection.indices.reversed()) {
          m.removeRow(tbl.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTable) {
      addMenuItem.isEnabled = canAddRow()
      deleteMenuItem.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }
}

private fun makeModel(): RowDataModel {
  val model = RowDataModel()
  model.addRowData(RowData("Name 1", "comment..."))
  model.addRowData(RowData("Name 2", "Test"))
  model.addRowData(RowData("Name d", ""))
  model.addRowData(RowData("Name c", "Test cc"))
  model.addRowData(RowData("Name b", "Test bb"))
  model.addRowData(RowData("Name a", "ff"))
  model.addRowData(RowData("Name 0", "Test aa"))
  return model
}

fun makeUI(): Component {
  val model = makeModel()
  val table = JTable(model)
  val sorter: TableRowSorter<out RowDataModel> = TableRowSorter(model)
  table.rowSorter = sorter
  table.componentPopupMenu = TablePopupMenu()
  table.fillsViewportHeight = true
  table.putClientProperty("terminateEditOnFocusLost", true)
  val filters: MutableCollection<RowFilter<in RowDataModel, in Int>> = HashSet(2)
  val filter1 = object : RowFilter<RowDataModel, Int>() {
    override fun include(entry: Entry<out RowDataModel, out Int>): Boolean {
      val m = entry.model
      val rd = m.getRowData(entry.identifier)
      return rd.comment.isNotEmpty()
    }
  }
  val filter2 = object : RowFilter<RowDataModel, Int>() {
    override fun include(entry: Entry<out RowDataModel, out Int>) =
      entry.identifier % 2 == 0
  }
  check1.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      filters.add(filter1)
    } else {
      filters.remove(filter1)
    }
    sorter.setRowFilter(RowFilter.andFilter(filters))
  }
  check2.addActionListener { e ->
    if ((e.source as? JCheckBox)?.isSelected == true) {
      filters.add(filter2)
    } else {
      filters.remove(filter2)
    }
    sorter.setRowFilter(RowFilter.andFilter(filters))
  }
  val box = Box.createHorizontalBox()
  box.add(check1)
  box.add(check2)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowDataModel : DefaultTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    val obj = arrayOf<Any>(number, t.name, t.comment)
    super.addRow(obj)
    number++
  }

  fun getRowData(identifier: Int) = RowData(
    getValueAt(identifier, 1)?.toString() ?: "",
    getValueAt(identifier, 2)?.toString() ?: "",
  )

  override fun isCellEditable(
    row: Int,
    col: Int,
  ) = COLUMN_ARRAY[col].isEditable

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
    )
  }
}

private data class RowData(
  val name: String,
  val comment: String,
)

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
