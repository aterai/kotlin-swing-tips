package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "String")
  val data = arrayOf(
    arrayOf("aaa", 1, "eee"),
    arrayOf("bbb", 2, "FFF"),
    arrayOf("CCC", 0, "GGG"),
    arrayOf("DDD", 3, "hhh"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column == 1) Number::class.java else String::class.java

    override fun isCellEditable(row: Int, column: Int) = false
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val listener = object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      (e.component as? JTable)?.also {
        val isDoubleClick = e.clickCount >= 2
        if (isDoubleClick) {
          val m = it.model
          val pt = e.point
          val i = it.rowAtPoint(pt)
          if (i >= 0) {
            val row = it.convertRowIndexToModel(i)
            val v0 = m.getValueAt(row, 0)
            val v1 = m.getValueAt(row, 1)
            JOptionPane.showMessageDialog(it, "$v0 ($v1)", "title", JOptionPane.INFORMATION_MESSAGE)
          }
        }
      }
    }
  }
  table.addMouseListener(listener)

  return JPanel(BorderLayout()).also {
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

  override fun show(c: Component?, x: Int, y: Int) {
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
