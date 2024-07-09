package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Boolean")
  val data = arrayOf(
    arrayOf("AAA", true),
    arrayOf("bbb", false),
    arrayOf("CCC", true),
    arrayOf("ddd", false),
    arrayOf("EEE", true),
    arrayOf("fff", false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) =
      if (column == 1) {
        Boolean::class.javaObjectType
      } else {
        String::class.java
      }

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = column == 1
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      setDefaultEditor(Boolean::class.javaObjectType, null)
      super.updateUI()
      setDefaultEditor(Boolean::class.javaObjectType, CheckBoxPanelEditor())
    }
  }
  table.putClientProperty("terminateEditOnFocusLost", true)
  table.rowHeight = 24
  table.rowSelectionAllowed = true
  table.showVerticalLines = false
  table.intercellSpacing = Dimension(0, 1)
  table.isFocusable = false
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class CheckBoxPanelEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val renderer = object : JPanel(GridBagLayout()) {
    private var listener: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(listener)
      super.updateUI()
      border = UIManager.getBorder("Table.noFocusBorder")
      listener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          fireEditingStopped()
        }
      }
      addMouseListener(listener)
    }
  }
  private val checkBox = object : JCheckBox() {
    private var handler: CellEditorHandler? = null

    override fun updateUI() {
      removeActionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      isOpaque = false
      isFocusable = false
      isRolloverEnabled = false
      handler = CellEditorHandler()
      addActionListener(handler)
      addMouseListener(handler)
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    checkBox.isSelected = value == true
    renderer.add(checkBox)
    return renderer
  }

  override fun getCellEditorValue() = checkBox.isSelected

  private inner class CellEditorHandler :
    MouseAdapter(),
    ActionListener {
    override fun actionPerformed(e: ActionEvent) {
      fireEditingStopped()
    }

    override fun mousePressed(e: MouseEvent) {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, e.component) as? JTable)?.also {
        if (checkBox.model.isPressed && it.isRowSelected(it.editingRow) && e.isControlDown) {
          renderer.background = it.background
        } else {
          renderer.background = it.selectionBackground
        }
      }
    }

    override fun mouseExited(e: MouseEvent) {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, e.component) as? JTable)
        ?.takeIf { it.isEditing }
        ?.also { it.removeEditor() }
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
