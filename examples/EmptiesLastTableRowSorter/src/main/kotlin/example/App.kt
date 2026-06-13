package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun createUI(): Component {
  val empty = arrayOf("", "")
  val columnNames = arrayOf("DefaultTableRowSorter", "EmptiesLastTableRowSorter")
  val data = arrayOf(
    arrayOf("aaa", "aaa"),
    arrayOf("ddd", "ddd"),
    arrayOf("bbb", "bbb"),
    arrayOf("eee", "eee"),
    arrayOf("ccc", "ccc"),
    arrayOf("fff", "fff"),
    empty,
    empty,
    empty,
    empty,
    empty,
    empty,
    empty,
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  (table.rowSorter as? TableRowSorter<out TableModel>)?.also {
    it.setComparator(1, RowComparator(table, 1))
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowComparator(
  private val table: JTable,
  private val column: Int,
) : Comparator<String> {
  override fun compare(
    a: String,
    b: String,
  ): Int {
    val dir = table.rowSorter
      ?.sortKeys
      ?.firstOrNull()
      ?.takeIf { it.column == column && it.sortOrder == SortOrder.DESCENDING }
      ?.let { -1 }
      ?: 1
    return if (a.isEmpty() && b.isNotEmpty()) {
      dir
    } else if (a.isNotEmpty() && b.isEmpty()) {
      -1 * dir
    } else {
      a.compareTo(b)
    }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
