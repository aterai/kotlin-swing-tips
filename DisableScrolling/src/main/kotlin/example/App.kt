package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

private val columnNames = arrayOf("String", "Integer", "Boolean")
private val data = arrayOf(
  arrayOf("aaa", 12, true),
  arrayOf("bbb", 5, false),
  arrayOf("CCC", 92, true),
  arrayOf("DDD", 0, false)
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val table = object : JTable(model) {
  override fun getToolTipText(e: MouseEvent): String {
    val row = convertRowIndexToModel(rowAtPoint(e.point))
    val m = model
    return "%s, %s".format(m.getValueAt(row, 0), m.getValueAt(row, 2))
  }
}
private val scroll = JScrollPane(table)

fun makeUI(): Component {
  for (i in 0 until 100) {
    model.addRow(arrayOf("Name $i", i, false))
  }
  table.autoCreateRowSorter = true
  table.componentPopupMenu = TablePopupMenu()

  val check = JCheckBox("Disable Scrolling")
  check.addActionListener { e ->
    table.clearSelection()
    val isSelected = (e.source as? JCheckBox)?.isSelected == true
    scroll.verticalScrollBar.isEnabled = !isSelected
    scroll.isWheelScrollingEnabled = !isSelected
    table.isEnabled = !isSelected
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(check, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val createMenuItem = add("add")
  private val deleteMenuItem = add("delete")

  init {
    createMenuItem.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf("New row", 0, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()

    deleteMenuItem.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      createMenuItem.isEnabled = c.isEnabled
      deleteMenuItem.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
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
