package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableRowSorter

private const val FIXED_RANGE = 2
private const val ES = ""

fun makeUI(): Component {
  // FixedColumnExample.java
  // @author Nobuo Tamemasa
  val data = arrayOf<Array<Any>>(
    arrayOf(1, 11, "A", ES, ES, ES, ES, ES),
    arrayOf(2, 22, ES, "B", ES, ES, ES, ES),
    arrayOf(3, 33, ES, ES, "C", ES, ES, ES),
    arrayOf(4, 1, ES, ES, ES, "D", ES, ES),
    arrayOf(5, 55, ES, ES, ES, ES, "E", ES),
    arrayOf(6, 66, ES, ES, ES, ES, ES, "F"),
  )
  val columnNames = arrayOf("fixed 1", "fixed 2", "A", "B", "C", "D", "E", "F")
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column < FIXED_RANGE) Number::class.java else Any::class.java
  }
  val sorter = TableRowSorter(model)
  val fixedTable = JTable(model)
  val table = JTable(model)
  fixedTable.selectionModel = table.selectionModel
  for (i in model.columnCount - 1 downTo 0) {
    if (i < FIXED_RANGE) {
      table.removeColumn(table.columnModel.getColumn(i))
      fixedTable.columnModel.getColumn(i).resizable = false
    } else {
      fixedTable.removeColumn(fixedTable.columnModel.getColumn(i))
    }
  }
  fixedTable.rowSorter = sorter
  fixedTable.autoResizeMode = JTable.AUTO_RESIZE_OFF
  fixedTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  fixedTable.putClientProperty("terminateEditOnFocusLost", true)
  table.rowSorter = sorter
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
  table.putClientProperty("terminateEditOnFocusLost", true)

  val scroll = JScrollPane(table)
  fixedTable.preferredScrollableViewportSize = fixedTable.preferredSize
  scroll.setRowHeaderView(fixedTable)
  scroll.setCorner(ScrollPaneConstants.UPPER_LEFT_CORNER, fixedTable.tableHeader)
  scroll.viewport.background = Color.WHITE
  scroll.rowHeader.background = Color.WHITE

  // https://tips4java.wordpress.com/2008/11/05/fixed-column-table/
  // @author Rob Camick
  scroll.rowHeader.addChangeListener { e ->
    (e.source as? JViewport)?.also {
      scroll.verticalScrollBar.value = it.viewPosition.y
    }
  }
  val addButton = JButton("add")
  addButton.addActionListener {
    sorter.sortKeys = null
    for (i in 0..<100) {
      model.addRow(arrayOf<Any>(i, i + 1, "A$i", "B$i"))
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(addButton, BorderLayout.SOUTH)
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
