package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("#", "String", "Integer")
  val data = arrayOf(
    arrayOf(false, "aaa", 12),
    arrayOf(false, "bbb", 5),
    arrayOf(false, "CCC", 92),
    arrayOf(false, "DDD", 0),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val selector = JTable(model)
  selector.autoCreateRowSorter = true
  selector.columnModel.getColumn(0).maxWidth = 32
  val viewer = object : JTable(model) {
    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
  viewer.autoCreateRowSorter = true
  val cm = viewer.columnModel
  cm.removeColumn(cm.getColumn(0))
  val sorter = TableRowSorter(model)
  viewer.rowSorter = sorter
  sorter.rowFilter = object : RowFilter<TableModel, Int>() {
    override fun include(entry: Entry<out TableModel, out Int>): Boolean {
      val i = entry.identifier
      return i >= 0 && entry.model.getValueAt(i, 0) == true
    }
  }
  model.addTableModelListener { e ->
    if (e.type == TableModelEvent.UPDATE) {
      sorter.allRowsChanged()
    }
  }
  return JPanel(GridLayout(0, 1)).also {
    it.add(JScrollPane(selector))
    it.add(JScrollPane(viewer))
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
