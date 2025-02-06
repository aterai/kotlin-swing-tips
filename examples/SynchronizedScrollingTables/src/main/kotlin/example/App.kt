package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableModel

const val FIXED_RANGE = 2
private const val ES = ""

fun makeUI(): Component {
  val data = arrayOf(
    arrayOf(1, 11, "A", ES, ES),
    arrayOf(2, 22, ES, "B", ES),
    arrayOf(3, 33, ES, ES, "C"),
    arrayOf(4, 1, ES, ES, ES),
    arrayOf(5, 55, ES, ES, ES),
    arrayOf(6, 66, ES, ES, ES),
  )
  val columnNames = arrayOf("1", "2", "a", "b", "c")
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column < FIXED_RANGE) Number::class.java else Any::class.java
  }
  val leftTable = makeTable(model)
  val table = makeTable(model)
  table.autoCreateRowSorter = true
  leftTable.rowSorter = table.rowSorter
  leftTable.selectionModel = table.selectionModel
  for (i in model.columnCount - 1 downTo 0) {
    if (i < FIXED_RANGE) {
      table.removeColumn(table.columnModel.getColumn(i))
      leftTable.columnModel.getColumn(i).resizable = false
    } else {
      leftTable.removeColumn(leftTable.columnModel.getColumn(i))
    }
  }

  val scroll1 = JScrollPane(leftTable)
  scroll1.verticalScrollBar = object : JScrollBar(Adjustable.VERTICAL) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 0
      return d
    }
  }

  val scroll2 = JScrollPane(table)
  scroll2.verticalScrollBar.model = scroll1.verticalScrollBar.model

  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, scroll1, scroll2)
  split.resizeWeight = .3

  val button = JButton("add")
  button.addActionListener {
    table.rowSorter.sortKeys = null
    for (i in 0..<100) {
      model.addRow(arrayOf(i, i + 1, "A$i", "B$i"))
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(split)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(model: TableModel): JTable {
  val table = JTable(model)
  table.showVerticalLines = false
  table.showHorizontalLines = false
  table.intercellSpacing = Dimension()
  return table
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
