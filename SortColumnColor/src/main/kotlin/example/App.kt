package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )
  val model: TableModel = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xFA_E6_E6)
    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(tcr, row, column).also {
        if (isRowSelected(row)) {
          it.foreground = getSelectionForeground()
          it.background = getSelectionBackground()
        } else {
          it.foreground = foreground
          it.background = if (isSortingColumn(column)) evenColor else background
        }
      }

    fun isSortingColumn(column: Int) = rowSorter?.sortKeys?.let {
      it.isNotEmpty() && column == convertColumnIndexToView(it[0].column)
    } ?: false
  }
  table.autoCreateRowSorter = true

  val button = JButton("clear SortKeys")
  button.addActionListener {
    table.rowSorter.setSortKeys(null)
  }

  return JPanel(BorderLayout()).also {
    it.add(button, BorderLayout.SOUTH)
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
