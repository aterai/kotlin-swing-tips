package example

import java.awt.*
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "Answer")
  val data = arrayOf<Array<Any>>(
    arrayOf(1, Answer.A),
    arrayOf(2, Answer.B),
    arrayOf(3, Answer.C),
    arrayOf(4, Answer.C),
    arrayOf(5, Answer.A),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val column = getColumnModel().getColumn(1)
      column.cellRenderer = RadioButtonsRenderer()
      column.cellEditor = RadioButtonsEditor()
    }
  }
  table.putClientProperty("terminateEditOnFocusLost", true)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class Answer {
  A,
  B,
  C,
}

private class RadioButtonsPanel : JPanel() {
  private val group = ButtonGroup()

  init {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    initButtons()
  }

  val selectedActionCommand: String
    get() = group.selection.actionCommand

  private fun initButtons() {
    group.clearSelection()
    group.elements.toList().forEach { group.remove(it) }
    removeAll()
    for (a in Answer.entries) {
      val b = makeButton(a.name)
      add(b)
      group.add(b)
    }
  }

  fun updateSelectedButton(v: Any?) {
    if (v is Answer) {
      initButtons()
      (getComponent(v.ordinal) as? JRadioButton)?.isSelected = true
    }
  }
}

private fun makeButton(title: String): JRadioButton {
  val b = JRadioButton(title)
  b.actionCommand = title
  b.isFocusable = false
  b.isRolloverEnabled = false
  return b
}

private class RadioButtonsRenderer : TableCellRenderer {
  private val renderer = RadioButtonsPanel()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ) = renderer.also { it.updateSelectedButton(value) }
}

private class RadioButtonsEditor :
  AbstractCellEditor(),
  TableCellEditor {
  private val renderer = RadioButtonsPanel()

  init {
    val al = ActionListener { fireEditingStopped() }
    for (c in renderer.components) {
      (c as? JRadioButton)?.addActionListener(al)
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ) = renderer.also { it.updateSelectedButton(value) }

  override fun getCellEditorValue() = Answer.valueOf(renderer.selectedActionCommand)
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
