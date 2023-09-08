package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("AAA", "BBB", "CCC", "DDD")
  val data = arrayOf(
    arrayOf("aaa", "1", "true", "cc"),
    arrayOf("aaa", "1", "false", "dd"),
    arrayOf("aaa", "2", "true", "ee"),
    arrayOf("ddd", "3", "false", "ff"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val r = MultiSortHeaderRenderer()
      val cm = getColumnModel()
      for (i in 0 until cm.columnCount) {
        cm.getColumn(i).headerRenderer = r
      }
    }
  }
  table.autoCreateRowSorter = true
  return JScrollPane(table).also {
    it.preferredSize = Dimension(320, 240)
  }
}

private class MultiSortHeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    var str = value?.toString() ?: ""
    table.rowSorter?.also { sorter ->
      for ((idx, sortKey) in sorter.sortKeys.withIndex()) {
        if (column == sortKey.column) {
          // // BLACK TRIANGLE
          // val k = if (sortKey.sortOrder == SortOrder.ASCENDING) "▲ " else "▼ "
          // BLACK SMALL TRIANGLE
          val k = if (sortKey.sortOrder == SortOrder.ASCENDING) "▴ " else "▾ "
          str = "<html>$str<small color='gray'>$k${idx + 1}"
        }
      }
    }
    val r = table.tableHeader.defaultRenderer
    return r.getTableCellRendererComponent(table, str, isSelected, hasFocus, row, column)
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
