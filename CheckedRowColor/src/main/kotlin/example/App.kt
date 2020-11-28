package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TableModelEvent
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private const val BOOLEAN_COLUMN = 2

fun makeTable(model: TableModel) = object : JTable(model) {
  override fun updateUI() {
    // XXX: set dummy ColorUIResource
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    val m = getModel()
    for (i in 0 until m.columnCount) {
      (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
        SwingUtilities.updateComponentTreeUI(it)
      }
    }
  }

  override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
    val c = super.prepareEditor(editor, row, column)
    if (convertColumnIndexToModel(column) == BOOLEAN_COLUMN && c is JCheckBox) {
      c.background = if (c.isSelected) Color.ORANGE else background
    }
    return c
  }

  override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int) =
    super.prepareRenderer(renderer, row, column).also {
      val b = model.getValueAt(convertRowIndexToModel(row), BOOLEAN_COLUMN) as? Boolean
      it.background = if (b == true) Color.ORANGE else background
      it.foreground = foreground
    }
}

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Number", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 1, false),
    arrayOf("bbb", 20, false),
    arrayOf("ccc", 2, false),
    arrayOf("ddd", 3, false),
    arrayOf("aaa", 1, false),
    arrayOf("bbb", 20, false),
    arrayOf("ccc", 2, false),
    arrayOf("ddd", 3, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, col: Int) = col == BOOLEAN_COLUMN
  }
  val table = makeTable(model)
  model.addTableModelListener { e ->
    if (e.type == TableModelEvent.UPDATE) {
      rowRepaint(table, table.convertRowIndexToView(e.firstRow))
    }
  }
  table.autoCreateRowSorter = true
  table.fillsViewportHeight = true
  table.setShowGrid(false)
  table.intercellSpacing = Dimension()
  table.rowSelectionAllowed = true
  // table.surrendersFocusOnKeystroke = true
  // table.putClientProperty("JTable.autoStartsEdit", false)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun rowRepaint(table: JTable, row: Int) {
  val r = table.getCellRect(row, 0, true)
  r.width = table.width
  table.repaint(r)
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
