package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class MainPanel : JPanel(BorderLayout()) {
  init {
    val check1 = JRadioButton("Default: ASCENDING<->DESCENDING", false)
    val check2 = JRadioButton("ASCENDING->DESCENDING->UNSORTED", true)
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
        arrayOf<Any>("aaa", 12, true),
        arrayOf<Any>("bbb", 5, false),
        arrayOf<Any>("CCC", 92, true),
        arrayOf<Any>("DDD", 0, false))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = JTable(model)
    val sorter = object : TableRowSorter<TableModel>(model) {
      override fun toggleSortOrder(column: Int) {
        if (!check2.isSelected() || !isSortable(column)) {
          super.toggleSortOrder(column)
          return
        }
        // val keys = ArrayList<RowSorter.SortKey>(getSortKeys())
        // if (!keys.isEmpty()) {
        //   val sortKey = keys.get(0)
        //   if (sortKey.getColumn() == column && sortKey.getSortOrder() == SortOrder.DESCENDING) {
        //     setSortKeys(null)
        //     return
        //   }
        // }
        getSortKeys().firstOrNull()
            ?.takeIf { it.getColumn() == column && it.getSortOrder() == SortOrder.DESCENDING }
            ?.also {
              setSortKeys(null)
              return
            }
        super.toggleSortOrder(column)
      }
    }
    table.setRowSorter(sorter)

    table.getColumnModel().getColumn(0).also {
      it.setMinWidth(60)
      it.setMaxWidth(60)
      it.setResizable(false)
    }

    ButtonGroup().also {
      it.add(check1)
      it.add(check2)
    }
    add(JPanel(GridLayout(2, 1)).also {
      it.add(check1)
      it.add(check2)
    }, BorderLayout.NORTH)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
