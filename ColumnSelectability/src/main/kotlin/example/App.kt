package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    fun isColumnSelectable(column: Int) = convertColumnIndexToModel(column) == 0

    override fun changeSelection(
      rowIndex: Int,
      columnIndex: Int,
      toggle: Boolean,
      extend: Boolean,
    ) {
      if (isColumnSelectable(columnIndex)) {
        super.changeSelection(rowIndex, columnIndex, toggle, extend)
      }
    }

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = isColumnSelectable(column)

    override fun prepareRenderer(
      r: TableCellRenderer,
      row: Int,
      column: Int,
    ) = if (isColumnSelectable(column)) {
      super.prepareRenderer(r, row, column)
    } else {
      r.getTableCellRendererComponent(
        this,
        getValueAt(row, column),
        false,
        false,
        row,
        column,
      )
    }
  }
  table.cellSelectionEnabled = true

  return JPanel(GridLayout(2, 1)).also {
    it.add(JScrollPane(JTable(model)))
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
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
