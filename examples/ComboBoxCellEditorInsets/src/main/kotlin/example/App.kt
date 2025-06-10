package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Border", "JPanel+JComboBox")
  val data = arrayOf(
    arrayOf("AAA", "a"),
    arrayOf("CCC", "bbb"),
    arrayOf("BBB", "c"),
    arrayOf("ZZZ", "dd"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(36)
      autoCreateRowSorter = true
      val combo = makeComboBox()
      getColumnModel().getColumn(0).also {
        it.setCellRenderer { table, value, isSelected, _, _, _ ->
          combo.removeAllItems()
          (combo.editor.editorComponent as? JComponent)?.also { editor ->
            editor.isOpaque = true
            if (isSelected) {
              editor.foreground = table.selectionForeground
              editor.background = table.selectionBackground
            } else {
              editor.foreground = table.foreground
              editor.background = table.background
            }
            combo.addItem(value?.toString() ?: "")
          }
          combo
        }
        it.cellEditor = DefaultCellEditor(makeComboBox())
      }

      getColumnModel().getColumn(1).also {
        it.cellRenderer = ComboBoxCellRenderer()
        it.cellEditor = ComboBoxCellEditor()
      }
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.border = BorderFactory.createTitledBorder("JComboBox in a Table Cell")
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeComboModel() = arrayOf("11111", "222", "3")

fun makeComboBox(): JComboBox<String> {
  val c = JComboBox(makeComboModel())
  c.isEditable = true
  val outsideBorder = BorderFactory.createEmptyBorder(8, 10, 8, 10)
  c.border = BorderFactory.createCompoundBorder(outsideBorder, c.border)
  return c
}

private class ComboBoxCellRenderer : TableCellRenderer {
  private val comboBox = JComboBox(makeComboModel())
  private val panel = JPanel(GridBagLayout())

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.insets = Insets(0, 10, 0, 10)
    c.fill = GridBagConstraints.HORIZONTAL
    comboBox.isEditable = true
    panel.removeAll()
    panel.add(comboBox, c)
    comboBox.selectedIndex = 0
    panel.isOpaque = true
    panel.background = if (isSelected) table.selectionBackground else table.background
    value?.also {
      comboBox.setSelectedItem(it)
    }
    return panel
  }
}

private class ComboBoxCellEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val comboBox = JComboBox(makeComboModel())
  private val panel = JPanel(GridBagLayout())

  init {
    comboBox.addActionListener { fireEditingStopped() }
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
    column: Int,
  ): Component {
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.insets = Insets(0, 10, 0, 10)
    c.fill = GridBagConstraints.HORIZONTAL
    comboBox.isEditable = true
    panel.removeAll()
    panel.add(comboBox, c)
    comboBox.selectedIndex = 0
    panel.isOpaque = true
    panel.background = table.selectionBackground
    comboBox.selectedItem = value
    return panel
  }

  override fun getCellEditorValue(): Any? = comboBox.selectedItem

  override fun shouldSelectCell(anEvent: EventObject): Boolean {
    if (anEvent is MouseEvent) {
      return anEvent.id != MouseEvent.MOUSE_DRAGGED
    }
    return true
  }

  override fun stopCellEditing(): Boolean {
    if (comboBox.isEditable) {
      comboBox.actionPerformed(ActionEvent(this, 0, ""))
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
