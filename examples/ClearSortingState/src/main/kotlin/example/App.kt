package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  val table = JTable(model)
  table.autoCreateRowSorter = true
  val ml = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val tbl = (e.component as? JTableHeader)?.table
      val sorter = tbl?.rowSorter
      if (sorter?.sortKeys?.isNotEmpty() == true) {
        val viewColumn = tbl.columnAtPoint(e.point)
        val column = tbl.convertColumnIndexToModel(viewColumn)
        if (column >= 0 && e.isShiftDown) {
          EventQueue.invokeLater { sorter.sortKeys = null }
        }
      }
    }
  }
  table.tableHeader.addMouseListener(ml)

  val col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false

  return JPanel(BorderLayout()).also {
    it.add(JLabel("Shift + Click -> Clear Sorting State"), BorderLayout.NORTH)
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
