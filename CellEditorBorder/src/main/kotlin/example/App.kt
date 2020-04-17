package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

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
      1 -> Int::class.java
      2 -> Boolean::class.java
      else -> super.getColumnClass(column)
    }
  }
  val table = object : JTable(model) {
    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      if (c is JCheckBox) {
        c.isBorderPainted = true
        c.background = getSelectionBackground()
        // } else if (c instanceof JComponent && convertColumnIndexToModel(column) == 1) {
      } else if (c is JComponent && Number::class.java.isAssignableFrom(getColumnClass(column))) {
        c.border = BorderFactory.createLineBorder(Color.GREEN, 2)
      }
      return c
    }
  }
  val field = JTextField()
  field.border = BorderFactory.createLineBorder(Color.RED, 2)
  table.setDefaultEditor(Any::class.java, DefaultCellEditor(field))

  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem
  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }

  init {
    add("add").addActionListener {
      val table = invoker as JTable
      val model = table.model as DefaultTableModel
      model.addRow(arrayOf<Any>("New row", model.rowCount, false))
      val r = table.getCellRect(model.rowCount - 1, 0, true)
      table.scrollRectToVisible(r)
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as JTable
      val model = table.model as DefaultTableModel
      val selection = table.selectedRows
      for (i in selection.indices.reversed()) {
        model.removeRow(table.convertRowIndexToModel(selection[i]))
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
