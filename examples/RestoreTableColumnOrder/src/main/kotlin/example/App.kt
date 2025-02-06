package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TableColumnModelEvent
import javax.swing.table.DefaultTableColumnModel
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val table = object : JTable(DefaultTableModel(8, 6)) {
    override fun createDefaultColumnModel() = SortableTableColumnModel()
  }
  table.autoCreateRowSorter = true

  val b = JButton("restore TableColumn order")
  b.addActionListener {
    (table.columnModel as? SortableTableColumnModel)?.restoreColumnOrder()
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.add(b, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SortableTableColumnModel : DefaultTableColumnModel() {
  fun restoreColumnOrder() {
    tableColumns.sortWith(compareBy { it.modelIndex })
    fireColumnMoved(TableColumnModelEvent(this, 0, tableColumns.size))
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
