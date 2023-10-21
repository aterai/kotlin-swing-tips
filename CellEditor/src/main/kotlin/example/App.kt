package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val modelCheck = JCheckBox("isCellEditable return false")
  val model = object : RowDataModel() {
    override fun isCellEditable(
      row: Int,
      col: Int,
    ) = !modelCheck.isSelected
  }
  model.addRowData(RowData("Name 1", "Comment"))
  model.addRowData(RowData("Name 2", "Test"))
  model.addRowData(RowData("Name d", "ee"))
  model.addRowData(RowData("Name c", "Test cc"))
  model.addRowData(RowData("Name b", "Test bb"))
  model.addRowData(RowData("Name a", "ff"))
  model.addRowData(RowData("Name 0", "Test aa"))
  model.addRowData(RowData("Name 0", "gg"))

  val table = JTable(model)
  table.columnModel.getColumn(0).also {
    it.minWidth = 50
    it.maxWidth = 50
    it.resizable = false
  }
  val dce = DefaultCellEditor(JTextField())
  val objectCheck = JCheckBox("setDefaultEditor(Object.class, null)")
  val editableCheck = JCheckBox("setEnabled(false)")
  val al = ActionListener {
    table.clearSelection()
    table.takeIf { it.isEditing }?.cellEditor?.stopCellEditing()
    table.setDefaultEditor(Any::class.java, if (objectCheck.isSelected) null else dce)
    table.isEnabled = !editableCheck.isSelected
  }
  val p = JPanel(GridLayout(3, 1))
  listOf(modelCheck, objectCheck, editableCheck).forEach {
    it.addActionListener(al)
    p.add(it)
  }
  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class RowDataModel : DefaultTableModel() {
  private var number = 0

  fun addRowData(t: RowData) {
    super.addRow(arrayOf(number, t.name, t.comment))
    number++
  }

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
