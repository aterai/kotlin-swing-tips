package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf(50, 50, false),
    arrayOf(13, 13, true),
    arrayOf(0, 0, false),
    arrayOf(20, 20, true),
    arrayOf(99, 99, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = column != 0
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      putClientProperty("terminateEditOnFocusLost", true)
      autoCreateRowSorter = true
      setRowHeight(26)
      getColumnModel().getColumn(1).also {
        it.cellRenderer = SliderRenderer()
        it.cellEditor = SliderEditor()
      }
    }

    override fun getSelectionBackground() = super.getSelectionBackground()?.brighter()
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SliderRenderer : TableCellRenderer {
  private val renderer = JSlider()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.background = if (isSelected) table.selectionBackground else table.background
    if (value is Int) {
      renderer.value = value
    }
    return renderer
  }
}

private class SliderEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = JSlider()
  private var prev = 0
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (value is Int) {
      renderer.value = value
    }
    renderer.background = table.selectionBackground
    return renderer
  }

  override fun getCellEditorValue() = renderer.value

  init {
    renderer.isOpaque = true
    renderer.addChangeListener {
      (SwingUtilities.getAncestorOfClass(JTable::class.java, renderer) as? JTable)?.also {
        val value = renderer.value
        if (it.isEditing && value != prev) {
          val row = it.convertRowIndexToModel(it.editingRow)
          it.model.setValueAt(value, row, 0)
          it.model.setValueAt(value, row, 1)
          prev = value
        }
      }
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
