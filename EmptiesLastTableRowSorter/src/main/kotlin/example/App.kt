package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val empty = arrayOf("", "", "")
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("aaa", "fff", "ggg"), arrayOf("jjj", "ppp", "ooo"),
    arrayOf("bbb", "eee", "hhh"), arrayOf("kkk", "qqq", "nnn"),
    arrayOf("ccc", "ddd", "iii"), arrayOf("lll", "rrr", "mmm"),
    empty, empty, empty, empty, empty, empty, empty
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = String::class.java
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  (table.rowSorter as? TableRowSorter<out TableModel>)?.also { sorter ->
    for (i in 0 until 3) { sorter.setComparator(i, RowComparator(table, i)) }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RowComparator(private val table: JTable, private val column: Int) : Comparator<String> {
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
