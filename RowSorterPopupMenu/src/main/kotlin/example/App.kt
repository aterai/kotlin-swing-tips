package example

import java.awt.*
import java.awt.event.ActionEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.rowSorter = object : TableRowSorter<TableModel>(model) {
    override fun toggleSortOrder(column: Int) {
      // Disable header click sorting
    }
  }
  table.rowSorter.sortKeys = listOf(RowSorter.SortKey(1, SortOrder.DESCENDING))
  table.tableHeader.componentPopupMenu = TableHeaderPopupMenu()
  table.columnModel.getColumn(0).also {
    it.minWidth = 80
    it.maxWidth = 80
    it.resizable = false
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TableHeaderPopupMenu : JPopupMenu() {
  private val actions = listOf(
    SortAction(SortOrder.ASCENDING),
    SortAction(SortOrder.DESCENDING),
  )

  private inner class SortAction(
    private val dir: SortOrder,
  ) : AbstractAction(dir.toString()) {
    private var index = -1

    fun setIndex(index: Int) {
      this.index = index
    }

    override fun actionPerformed(e: ActionEvent) {
      val t = (invoker as? JTableHeader)?.table
      t?.rowSorter?.sortKeys = listOf(RowSorter.SortKey(index, dir))
    }
  }

  init {
    actions.forEach { this.add(it) }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTableHeader) {
      val table = c.table
      c.draggedColumn = null
      c.repaint()
      table.repaint()
      val i = table.convertColumnIndexToModel(c.columnAtPoint(Point(x, y)))
      if (i >= 0) {
        actions.forEach { it.setIndex(i) }
        super.show(c, x, y)
      }
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
