package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false)
  )

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int): Class<*> {
      return when (column) {
        0 -> String::class.java
        1 -> Number::class.java
        2 -> Boolean::class.javaObjectType
        else -> super.getColumnClass(column)
      }
    }
  }

  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val button = JButton("remove all rows")
  button.addActionListener { model.rowCount = 0 }

  return JPanel(BorderLayout()).also {
    it.add(button, BorderLayout.SOUTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (table != null && model is DefaultTableModel) {
        model.addRow(arrayOf("", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (table != null && model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTable)?.also {
      delete.isEnabled = it.selectedRowCount > 0
      super.show(it, x, y)
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
