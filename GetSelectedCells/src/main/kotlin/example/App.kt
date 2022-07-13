package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

private const val CELL_SIZE = 24

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B", "C", "D", "E", "F", "G", "H", "I")
  val data = arrayOf(
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true),
    arrayOf(true, false, true, false, true, true, false, true, true)
  )

  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = Boolean::class.javaObjectType
  }

  val table = object : JTable(model) {
    override fun getPreferredScrollableViewportSize() = super.getPreferredSize()

    override fun updateUI() {
      setDefaultEditor(Boolean::class.javaObjectType, null)
      super.updateUI()
      setDefaultEditor(Boolean::class.javaObjectType, BooleanEditor())
    }
  }

  table.cellSelectionEnabled = true
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF
  table.autoCreateRowSorter = true
  table.componentPopupMenu = TablePopupMenu()

  val m = table.columnModel
  (0 until m.columnCount).forEach {
    val col = m.getColumn(it)
    col.preferredWidth = CELL_SIZE
    col.resizable = false
  }

  val scroll = JScrollPane(table)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  return JPanel(GridBagLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val select = add("select")
  private val clear = add("clear")
  private val toggle = add("toggle")

  init {
    select.addActionListener {
      (invoker as? JTable)?.also {
        initAllTableValue(it, true)
      }
    }

    clear.addActionListener {
      (invoker as? JTable)?.also {
        initAllTableValue(it, false)
      }
    }

    toggle.addActionListener {
      (invoker as? JTable)?.also {
        toggleTableValue(it)
      }
    }
  }

  private fun initAllTableValue(table: JTable, b: Boolean) {
    for (row in table.selectedRows) {
      for (col in table.selectedColumns) {
        table.setValueAt(b, row, col)
      }
    }
  }

  private fun toggleTableValue(table: JTable) {
    for (row in table.selectedRows) {
      for (col in table.selectedColumns) {
        val b = table.getValueAt(row, col) as? Boolean ?: continue
        table.setValueAt(!b, row, col)
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTable) {
      val isSelected = c.selectedRowCount > 0
      select.isEnabled = isSelected
      clear.isEnabled = isSelected
      toggle.isEnabled = isSelected
      super.show(c, x, y)
    }
  }
}

private class BooleanEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = object : JPanel(GridBagLayout()) {
    private var listener: MouseListener? = null

    override fun updateUI() {
      removeMouseListener(listener)
      super.updateUI()
      listener = object : MouseAdapter() {
        override fun mousePressed(e: MouseEvent) {
          fireEditingStopped()
        }
      }
      addMouseListener(listener)
    }
  }
  private val checkBox = object : JCheckBox() {
    var handler: CheckBoxHandler? = null

    override fun updateUI() {
      removeActionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      border = UIManager.getBorder("Table.noFocusBorder")
      isOpaque = false
      isFocusable = false
      isRolloverEnabled = false
      handler = CheckBoxHandler()
      addActionListener(handler)
      addMouseListener(handler)
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    checkBox.isSelected = value == true
    renderer.add(checkBox)
    return renderer
  }

  override fun getCellEditorValue() = checkBox.isSelected

  override fun isCellEditable(e: EventObject) = (e as? MouseEvent)
    ?.takeUnless { e.isShiftDown || e.isControlDown }?.let { true } ?: super.isCellEditable(e)

  private inner class CheckBoxHandler : MouseAdapter(), ActionListener {
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
      val c = SwingUtilities.getAncestorOfClass(JTable::class.java, e.component)
      (c as? JTable)?.takeIf { it.isEditing }?.removeEditor()
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
