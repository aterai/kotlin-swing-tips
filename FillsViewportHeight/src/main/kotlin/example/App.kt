package example

import java.awt.*
import java.awt.event.ItemEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

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
      2 -> Boolean::class.javaObjectType
      else -> super.getColumnClass(column)
    }
  }
  val table = object : JTable(model) {
    private val evenColor = Color(0xF5_F5_F5)

    override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(tcr, row, column).also {
        if (isRowSelected(row)) {
          it.foreground = getSelectionForeground()
          it.background = getSelectionBackground()
        } else {
          it.foreground = foreground
          it.background = if (row % 2 == 0) evenColor else background
        }
      }
  }
  val scroll = JScrollPane(table)
  scroll.background = Color.RED
  scroll.viewport.background = Color.GREEN
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.componentPopupMenu = TablePopupMenu()
  table.rowSorter = TableRowSorter(model)

  return JPanel(BorderLayout()).also {
    it.add(makeToolBox(table), BorderLayout.NORTH)
    it.add(makeColorBox(table), BorderLayout.SOUTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeToolBox(table: JTable): Component {
  val check = JCheckBox("FillsViewportHeight")
  check.addActionListener { e ->
    table.fillsViewportHeight = (e.source as? JCheckBox)?.isSelected == true
  }

  val button = JButton("clearSelection")
  button.addActionListener { table.clearSelection() }

  val box = Box.createHorizontalBox()
  box.add(check)
  box.add(button)
  return box
}

private fun makeColorBox(table: JTable): Component {
  val p = JPanel(FlowLayout(FlowLayout.LEFT))
  p.add(JLabel("table.setBackground: "))

  val r1 = JRadioButton("WHITE", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      table.background = Color.WHITE
    }
  }

  val r2 = JRadioButton("BLUE")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      table.background = Color.BLUE
    }
  }

  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    bg.add(it)
    p.add(it)
  }
  return p
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model as? DefaultTableModel
      if (model != null) {
        model.addRow(arrayOf("example", model.rowCount, false))
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
