package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Column1", "Column2")
  val data = arrayOf(
    arrayOf("colors", makeModel("blue", "violet", "red", "yellow")),
    arrayOf("sports", makeModel("basketball", "soccer", "football", "hockey")),
    arrayOf("food", makeModel("hot dogs", "pizza", "ravioli", "bananas"))
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column == 1) DefaultComboBoxModel::class.java else String::class.java
  }
  val table = JTable(model)
  table.rowHeight = 24
  table.autoCreateRowSorter = true

  val col = table.columnModel.getColumn(1)
  col.cellRenderer = ComboCellRenderer()
  col.cellEditor = ComboCellEditor()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(vararg items: String) =
  object : DefaultComboBoxModel<String>(items) {
    override fun toString() = selectedItem?.toString() ?: ""
  }

private class ComboCellRenderer : TableCellRenderer {
  private val combo = JComboBox<String>()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    combo.removeAllItems()
    if (value is DefaultComboBoxModel<*>) {
      combo.addItem(value.selectedItem.toString())
    }
    return combo
  }
}

private class ComboCellEditor : AbstractCellEditor(), TableCellEditor {
  private val combo = JComboBox<String>()

  init {
    combo.putClientProperty("JComboBox.isTableCellEditor", true)
    combo.isEditable = true
    combo.addActionListener { fireEditingStopped() }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (value is ComboBoxModel<*>) {
      @Suppress("UNCHECKED_CAST")
      combo.model = value as? ComboBoxModel<String>
    }
    return combo
  }

  override fun getCellEditorValue(): Any {
    val m = combo.model
    if (m is DefaultComboBoxModel<String> && combo.isEditable) {
      val str = combo.editor?.item?.toString() ?: ""
      if (str.isNotEmpty() && m.getIndexOf(str) < 0) {
        m.insertElementAt(str, 0)
        combo.selectedIndex = 0
      }
    }
    return m
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
