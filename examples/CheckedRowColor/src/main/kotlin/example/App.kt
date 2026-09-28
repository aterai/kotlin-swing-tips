package example

import java.awt.*
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableModelEvent
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private const val BOOLEAN_COLUMN = 2
private val CHECKED_COLOR = Color.ORANGE

fun createUI(): Component {
  val table = CheckedRowColorTable(createModel())
  table.model.addTableModelListener { e ->
    // Only a change in the check box column affects the row background color
    if (e.type == TableModelEvent.UPDATE && e.column == BOOLEAN_COLUMN) {
      if (e.firstRow == e.lastRow) {
        repaintRow(table, table.convertRowIndexToView(e.firstRow))
      } else {
        table.repaint()
      }
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

private fun createModel(): TableModel {
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
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = column == BOOLEAN_COLUMN
  }
}

private class CheckedRowColorTable(
  model: TableModel,
) : JTable(model) {
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

  override fun prepareRenderer(
    renderer: TableCellRenderer,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareRenderer(renderer, row, column)
    // Keep the selection colors set by the renderer for the selected rows
    if (!isRowSelected(row)) {
      val value = model.getValueAt(convertRowIndexToModel(row), BOOLEAN_COLUMN)
      c.foreground = foreground
      c.background = if (value == true) CHECKED_COLOR else background
    }
    return c
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    updateEditorBackground(c, row)
    return c
  }

  override fun valueChanged(e: ListSelectionEvent) {
    super.valueChanged(e)
    // A mouse press starts editing before the row is selected,
    // so the editor background also has to follow the selection change
    if (isEditing) {
      updateEditorBackground(editorComponent, editingRow)
    }
  }

  private fun updateEditorBackground(
    c: Component?,
    row: Int,
  ) {
    if (c is JCheckBox) {
      c.background = when {
        isRowSelected(row) -> selectionBackground
        c.isSelected -> CHECKED_COLOR
        else -> background
      }
    }
  }
}

private fun repaintRow(
  table: JTable,
  viewRow: Int,
) {
  // The row may be filtered out by the RowSorter
  if (viewRow >= 0) {
    val r = table.getCellRect(viewRow, 0, true)
    table.repaint(0, r.y, table.width, r.height)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
