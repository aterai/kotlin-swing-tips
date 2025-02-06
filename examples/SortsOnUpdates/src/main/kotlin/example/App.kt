package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "String", "Boolean")
  val data = arrayOf(
    arrayOf(0, "", true),
    arrayOf(1, "", false),
    arrayOf(2, "", true),
    arrayOf(3, "", false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      when (column) {
        0 -> Number::class.java
        1 -> String::class.java
        2 -> Boolean::class.javaObjectType
        else -> super.getColumnClass(column)
      }
  }
  val table = object : JTable(model) {
    override fun prepareRenderer(
      tcr: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareRenderer(tcr, row, column)
      when {
        isRowSelected(row) -> {
          c.foreground = getSelectionForeground()
          c.background = getSelectionBackground()
        }

        convertRowIndexToModel(row) == rowCount - 1 -> {
          c.foreground = Color.WHITE
          c.background = Color.RED
        }

        else -> {
          c.foreground = foreground
          c.background = background
        }
      }
      return c
    }
  }
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  val check = JCheckBox("DefaultRowSorter#setSortsOnUpdates")
  check.addActionListener { e ->
    (table.rowSorter as? DefaultRowSorter<*, *>)?.also {
      it.sortsOnUpdates = (e.source as? JCheckBox)?.isSelected == true
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
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
        val i = model.rowCount
        model.addRow(arrayOf(i, "", i % 2 == 0))
        val r = table.getCellRect(table.convertRowIndexToView(i - 1), 0, true)
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
