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
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java
      1 -> Number::class.java
      2 -> java.lang.Boolean::class.java
      else -> super.getColumnClass(column)
    }
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.rowSelectionAllowed = true
  table.setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)

  val col = table.columnModel.getColumn(0)
  col.minWidth = 60
  col.maxWidth = 60
  col.resizable = false

  val scroll = JScrollPane(table)
  scroll.componentPopupMenu = TablePopupMenu()
  table.inheritsPopupMenu = true
  scroll.viewport.isOpaque = true

  val check = JCheckBox("viewport setOpaque", true)
  check.addActionListener { e ->
    scroll.viewport.isOpaque = (e.source as? JCheckBox)?.isSelected == true
    scroll.repaint()
  }

  val button = JButton("Choose background color")
  button.addActionListener {
    val bgc = scroll.viewport.background
    val color = JColorChooser.showDialog(scroll.rootPane, "background color", bgc)
    scroll.viewport.background = color
    scroll.repaint()
  }

  val pnl = JPanel()
  pnl.add(check)
  pnl.add(button)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(pnl, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf("New row", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
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
      delete.isEnabled = c.selectedRowCount > 0
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
