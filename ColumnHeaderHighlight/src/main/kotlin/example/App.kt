package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val table = JTable(DefaultTableModel(10, 4))
  table.cellSelectionEnabled = true
  table.autoCreateRowSorter = true
  val cm = table.columnModel
  val r = ColumnHeaderRenderer()
  for (i in 0 until cm.columnCount) {
    cm.getColumn(i).headerRenderer = r
  }
  cm.selectionModel.addListSelectionListener { table.tableHeader.repaint() }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColumnHeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val csm = table.columnModel.selectionModel
    val f = csm.leadSelectionIndex == column || hasFocus
    return r.getTableCellRendererComponent(table, value, isSelected, f, row, column)
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
