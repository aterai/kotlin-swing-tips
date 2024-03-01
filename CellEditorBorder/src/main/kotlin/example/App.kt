package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = when (column) {
      0 -> String::class.java

      1 -> Int::class.javaObjectType

      // java.lang.Integer::class.java
      2 -> Boolean::class.javaObjectType

      // java.lang.Boolean::class.java
      else -> super.getColumnClass(column)
    }
  }
  val table = object : JTable(model) {
    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ) = super.prepareEditor(editor, row, column).also {
      if (it is JCheckBox) {
        it.isBorderPainted = true
        it.background = getSelectionBackground()
      } else if (it is JComponent && isNumber(getColumnClass(column))) {
        it.border = BorderFactory.createLineBorder(Color.GREEN, 2)
      }
    }

    private fun isNumber(clz: Class<*>) = Number::class.java.isAssignableFrom(clz)
  }
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val field = JTextField()
  field.border = BorderFactory.createLineBorder(Color.RED, 2)
  table.setDefaultEditor(Any::class.java, DefaultCellEditor(field))

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
      val model = table?.model as? DefaultTableModel
      if (model != null) {
        model.addRow(arrayOf("New row", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model as? DefaultTableModel
      if (model != null) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
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
