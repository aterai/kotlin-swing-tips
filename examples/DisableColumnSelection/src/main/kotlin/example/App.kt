package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val targetCol = 0
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = column != 0
  }

  val t1 = object : JTable(model) {
    override fun changeSelection(
      rowIdx: Int,
      colIdx: Int,
      toggle: Boolean,
      extend: Boolean,
    ) {
      if (convertColumnIndexToModel(colIdx) == targetCol) {
        super.changeSelection(rowIdx, colIdx, toggle, extend)
      }
    }

    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ) = if (convertColumnIndexToModel(column) != targetCol) {
      val value = getValueAt(row, column)
      renderer.getTableCellRendererComponent(this, value, false, false, row, column)
    } else {
      super.prepareRenderer(renderer, row, column)
    }
  }

  val t2 = object : JTable(model) {
    override fun changeSelection(
      rowIdx: Int,
      colIdx: Int,
      toggle: Boolean,
      extend: Boolean,
    ) {
      if (convertColumnIndexToModel(colIdx) == targetCol) {
        super.changeSelection(rowIdx, colIdx, toggle, extend)
      }
    }
  }
  t2.cellSelectionEnabled = true
  t2.columnModel.selectionModel = object : DefaultListSelectionModel() {
    override fun isSelectedIndex(i: Int) = t2.convertColumnIndexToModel(i) == targetCol
  }

  val p = JPanel(GridLayout(0, 1))
  p.add(JScrollPane(t1))
  p.add(JScrollPane(t2))

  return JPanel(BorderLayout()).also {
    it.add(p)
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
