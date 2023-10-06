package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "String", "Boolean")
  val data = arrayOf(
    arrayOf(1, "D", true),
    arrayOf(2, "B", false),
    arrayOf(3, "C", false),
    arrayOf(4, "E", false),
    arrayOf(5, "A", false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun setValueAt(
      v: Any,
      row: Int,
      column: Int,
    ) {
      if (v is Boolean) {
        for (i in 0 until rowCount) {
          super.setValueAt(i == row, i, column)
        }
      } else {
        super.setValueAt(v, row, column)
      }
    }
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      autoCreateRowSorter = true
      val c = getColumnModel().getColumn(2)
      c.cellRenderer = RadioButtonsRenderer()
      c.cellEditor = RadioButtonsEditor()
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class RadioButtonsRenderer : TableCellRenderer {
  private val renderer = JRadioButton()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is Boolean) {
      renderer.background = if (isSelected) table.selectionBackground else table.background
      renderer.horizontalAlignment = SwingConstants.CENTER
      renderer.isSelected = value
    }
    return renderer
  }
}

private class RadioButtonsEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = JRadioButton().also {
    it.addActionListener { fireEditingStopped() }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is Boolean) {
      renderer.background = table.selectionBackground
      renderer.horizontalAlignment = SwingConstants.CENTER
      renderer.isSelected = value
    }
    return renderer
  }

  override fun getCellEditorValue() = renderer.isSelected
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
