package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Border", "JPanel+JComboBox")
  val data = arrayOf(
    arrayOf("AAA", "a"),
    arrayOf("CCC", "bbb"),
    arrayOf("BBB", "c"),
    arrayOf("ZZZ", "dd")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(36)
      autoCreateRowSorter = true
      var column = getColumnModel().getColumn(0)
      column.cellRenderer = makeComboTableCellRenderer(makeComboBox())
      column.cellEditor = DefaultCellEditor(makeComboBox())
      column = getColumnModel().getColumn(1)
      column.cellRenderer = ComboBoxCellRenderer()
      column.cellEditor = ComboBoxCellEditor()
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.border = BorderFactory.createTitledBorder("JComboBox in a Table Cell")
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeComboBox(): JComboBox<String> {
  val c = JComboBox(arrayOf("11111", "222", "3"))
  c.isEditable = true
  c.border = BorderFactory.createCompoundBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10), c.border)
  return c
}

fun makeComboTableCellRenderer(combo: JComboBox<String>) = TableCellRenderer { table, value, isSelected, _, _, _ ->
  combo.also {
    it.removeAllItems()
    (it.editor.editorComponent as? JComponent)?.also { editor ->
      editor.isOpaque = true
      if (isSelected) {
        editor.foreground = table.selectionForeground
        editor.background = table.selectionBackground
      } else {
        editor.foreground = table.foreground
        editor.background = table.background
      }
      it.addItem(Objects.toString(value, ""))
    }
  }
}

private class ComboBoxPanel : JPanel(GridBagLayout()) {
  val comboBox = JComboBox(arrayOf("11111", "222", "3"))

  init {
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.insets = Insets(0, 10, 0, 10)
    c.fill = GridBagConstraints.HORIZONTAL
    comboBox.isEditable = true
    isOpaque = true
    add(comboBox, c)
    comboBox.selectedIndex = 0
  }
}

private class ComboBoxCellRenderer : TableCellRenderer {
  private val panel = ComboBoxPanel()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    panel.background = if (isSelected) table.selectionBackground else table.background
    value?.also {
      panel.comboBox.setSelectedItem(it)
    }
    return panel
  }
}

private class ComboBoxCellEditor : AbstractCellEditor(), TableCellEditor {
  private val panel = ComboBoxPanel()

  init {
    panel.comboBox.addActionListener { fireEditingStopped() }
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        fireEditingStopped()
      }
    }
    panel.addMouseListener(ml)
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    panel.background = table.selectionBackground
    panel.comboBox.selectedItem = value
    return panel
  }

  override fun getCellEditorValue(): Any? = panel.comboBox.selectedItem

  override fun shouldSelectCell(anEvent: EventObject): Boolean {
    if (anEvent is MouseEvent) {
      return anEvent.id != MouseEvent.MOUSE_DRAGGED
    }
    return true
  }

  override fun stopCellEditing(): Boolean {
    if (panel.comboBox.isEditable) {
      panel.comboBox.actionPerformed(ActionEvent(this, 0, ""))
    }
    fireEditingStopped()
    return true
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
