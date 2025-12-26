package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private const val BOOLEAN_COLUMN = 2

fun makeTable(model: TableModel): JTable {
  return object : JTable(model) {
    override fun updateUI() {
      // Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
      // https://bugs.openjdk.org/browse/JDK-6788475
      // Set a temporary ColorUIResource to avoid this issue
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      super.updateUI()
      val m = getModel()
      for (i in 0..<m.columnCount) {
        (getDefaultRenderer(m.getColumnClass(i)) as? Component)?.also {
          SwingUtilities.updateComponentTreeUI(it)
        }
      }
    }

    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ): Component {
      val c = super.prepareEditor(editor, row, column)
      if (convertColumnIndexToModel(column) == BOOLEAN_COLUMN && c is JCheckBox) {
        c.background = if (c.isSelected) Color.ORANGE else background
      }
      return c
    }

    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ): Component {
      val r = super.prepareRenderer(renderer, row, column)
      val rowIndex = convertRowIndexToModel(row)
      val b = model.getValueAt(rowIndex, BOOLEAN_COLUMN) as? Boolean
      r.background = if (b == true) Color.ORANGE else background
      r.foreground = foreground
      return r
    }
  }
}

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Number", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 1, false),
    arrayOf("bbb", 20, false),
    arrayOf("ccc", 2, false),
    arrayOf("ddd", 3, false),
    arrayOf("aaa", 1, false),
    arrayOf("bbb", 20, false),
    arrayOf("ccc", 2, false),
    arrayOf("ddd", 3, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(
      row: Int,
      col: Int,
    ) = col == BOOLEAN_COLUMN
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

private fun rowRepaint(
  table: JTable,
  row: Int,
) {
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
