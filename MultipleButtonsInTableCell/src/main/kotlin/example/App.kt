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

class MainPanel : JPanel(BorderLayout()) {
  init {
    val empty = ""
    val columnNames = arrayOf("String", "Button")
    val data = arrayOf(
      arrayOf("AAA", empty),
      arrayOf("CCC", empty),
      arrayOf("BBB", empty),
      arrayOf("ZZZ", empty))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = object : JTable(model) {
      override fun updateUI() {
        super.updateUI()
        setRowHeight(36)
        setAutoCreateRowSorter(true)
        getColumnModel().getColumn(1)?.also {
          it.setCellRenderer(ButtonsRenderer())
          it.setCellEditor(ButtonsEditor(this))
        }
      }
    }

    add(JScrollPane(table))
    setBorder(BorderFactory.createTitledBorder("Multiple Buttons in a Table Cell"))
    setPreferredSize(Dimension(320, 240))
  }
}

open class ButtonsPanel : JPanel() {
  val buttons = listOf(JButton("view"), JButton("edit"))

  init {
    EventQueue.invokeLater {
      for (b in buttons) {
        b.setFocusable(false)
        b.setRolloverEnabled(false)
        add(b)
      }
    }
  }

  override fun updateUI() {
    super.updateUI()
    setOpaque(true)
  }
}

class ButtonsRenderer : TableCellRenderer {
  private val panel = object : ButtonsPanel() {
    override fun updateUI() {
      super.updateUI()
      setName("Table.cellRenderer")
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    panel.setBackground(if (isSelected) table.getSelectionBackground() else table.getBackground())
    return panel
  }
}

class ViewAction(private val table: JTable) : AbstractAction("view") {
  override fun actionPerformed(e: ActionEvent) {
    JOptionPane.showMessageDialog(table, "Viewing")
  }
}

class EditAction(private val table: JTable) : AbstractAction("edit") {
  override fun actionPerformed(e: ActionEvent) {
    // Object o = table.getModel().getValueAt(table.getSelectedRow(), 0);
    val row = table.convertRowIndexToModel(table.getEditingRow())
    val o = table.getModel().getValueAt(row, 0)
    JOptionPane.showMessageDialog(table, "Editing: $o")
  }
}

class ButtonsEditor(private val table: JTable) : AbstractCellEditor(), TableCellEditor {
  private val panel = ButtonsPanel()

  private inner class EditingStopHandler : MouseAdapter(), ActionListener {
    override fun mousePressed(e: MouseEvent) {
      when (val o = e.getSource()) {
        is TableCellEditor -> actionPerformed(ActionEvent(o, ActionEvent.ACTION_PERFORMED, ""))
        is JButton -> if (o.getModel().isPressed() && table.isRowSelected(table.getEditingRow()) && e.isControlDown()) {
          panel.setBackground(table.getBackground())
        }
      }
    }

    override fun actionPerformed(e: ActionEvent) {
      EventQueue.invokeLater { fireEditingStopped() }
    }
  }

  init {
    panel.buttons[0].setAction(ViewAction(table))
    panel.buttons[1].setAction(EditAction(table))

    val handler = EditingStopHandler()
    for (b in panel.buttons) {
      b.addMouseListener(handler)
      b.addActionListener(handler)
    }
    panel.addMouseListener(handler)
  }

  override fun getTableCellEditorComponent(tbl: JTable, value: Any, isSelected: Boolean, row: Int, column: Int) =
    panel.also { it.setBackground(tbl.getSelectionBackground()) }

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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
