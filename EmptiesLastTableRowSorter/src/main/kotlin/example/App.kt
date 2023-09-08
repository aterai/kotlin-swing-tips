package example

import java.awt.*
import java.io.Serializable
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val empty = arrayOf("", "")
  val columnNames = arrayOf("DefaultTableRowSorter", "EmptiesLastTableRowSorter")
  val data = arrayOf(
    arrayOf("aaa", "aaa"), arrayOf("ddd", "ddd"),
    arrayOf("bbb", "bbb"), arrayOf("eee", "eee"),
    arrayOf("ccc", "ccc"), arrayOf("fff", "fff"),
    empty, empty, empty, empty, empty, empty, empty,
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
  private val column: Int
) : Comparator<String>, Serializable {
  override fun compare(a: String, b: String): Int {
    var flag = 1
    val keys = table.rowSorter.sortKeys
    if (keys.isNotEmpty()) {
      val sortKey = keys[0]
      if (sortKey.column == column && sortKey.sortOrder == SortOrder.DESCENDING) {
        flag = -1
      }
    }
    return if (a.isEmpty() && b.isNotEmpty()) {
      flag
    } else if (a.isNotEmpty() && b.isEmpty()) {
      -1 * flag
    } else {
      a.compareTo(b)
    }
  }

  companion object {
    private const val serialVersionUID = 1L
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
