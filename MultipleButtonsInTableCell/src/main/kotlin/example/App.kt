package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val empty = ""
  val columnNames = arrayOf("String", "Button")
  val data = arrayOf(
    arrayOf("AAA", empty),
    arrayOf("CCC", empty),
    arrayOf("BBB", empty),
    arrayOf("ZZZ", empty)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(36)
      autoCreateRowSorter = true
      getColumnModel().getColumn(1)?.also {
        it.cellRenderer = ButtonsRenderer()
        it.cellEditor = ButtonsEditor(this)
      }
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.border = BorderFactory.createTitledBorder("Multiple Buttons in a Table Cell")
    it.preferredSize = Dimension(320, 240)
  }
}

private open class ButtonsPanel : JPanel() {
  val buttons = listOf(JButton("view"), JButton("edit"))

  init {
    for (b in buttons) {
      b.isFocusable = false
      b.isRolloverEnabled = false
      add(b)
    }
  }

  final override fun add(comp: Component?) = super.add(comp)

  override fun updateUI() {
    super.updateUI()
    isOpaque = true
  }
}

private class ButtonsRenderer : TableCellRenderer {
  private val panel = object : ButtonsPanel() {
    override fun updateUI() {
      super.updateUI()
      name = "Table.cellRenderer"
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    panel.background = if (isSelected) table.selectionBackground else table.background
    return panel
  }
}

private class ViewAction(private val table: JTable) : AbstractAction("view") {
  override fun actionPerformed(e: ActionEvent) {
    JOptionPane.showMessageDialog(table, "Viewing")
  }
}

private class EditAction(private val table: JTable) : AbstractAction("edit") {
  override fun actionPerformed(e: ActionEvent) {
    val row = table.convertRowIndexToModel(table.editingRow)
    val o = table.model.getValueAt(row, 0)
    JOptionPane.showMessageDialog(table, "Editing: $o")
  }
}

private class ButtonsEditor(private val table: JTable) : AbstractCellEditor(), TableCellEditor {
  private val panel = ButtonsPanel()

  private inner class EditingStopHandler : MouseAdapter(), ActionListener {
    override fun mousePressed(e: MouseEvent) {
      when (val o = e.source) {
        is TableCellEditor -> actionPerformed(makeActionEvent(o))
        is JButton -> if (o.model.isPressed && e.isControlDown && isInEditor()) {
          panel.background = table.background
        }
      }
    }

    private fun isInEditor() = table.isRowSelected(table.editingRow)

    private fun makeActionEvent(o: Any?) = ActionEvent(o, ActionEvent.ACTION_PERFORMED, "")

    override fun actionPerformed(e: ActionEvent) {
      EventQueue.invokeLater { fireEditingStopped() }
    }
  }

  init {
    panel.buttons[0].action = ViewAction(table)
    panel.buttons[1].action = EditAction(table)

    val handler = EditingStopHandler()
    for (b in panel.buttons) {
      b.addMouseListener(handler)
      b.addActionListener(handler)
    }
    panel.addMouseListener(handler)
  }

  override fun getTableCellEditorComponent(
    tbl: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ) = panel.also { it.background = tbl.selectionBackground }

  override fun getCellEditorValue() = ""
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
