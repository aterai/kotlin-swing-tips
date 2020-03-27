package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "Answer")
  val data = arrayOf(
    arrayOf(1, Answer.A),
    arrayOf(2, Answer.B),
    arrayOf(3, Answer.C),
    arrayOf(4, Answer.C),
    arrayOf(5, Answer.A)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      getColumnModel().getColumn(1).cellRenderer = RadioButtonsRenderer()
      getColumnModel().getColumn(1).cellEditor = RadioButtonsEditor()
    }
  }
  table.putClientProperty("terminateEditOnFocusLost", true)

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private enum class Answer {
  A, B, C
}

private class RadioButtonsPanel : JPanel() {
  private val answer = arrayOf(Answer.A.toString(), Answer.B.toString(), Answer.C.toString())
  val buttons = ArrayList<JRadioButton>(answer.size)
  var bg = ButtonGroup()

  private fun initButtons() {
    buttons.clear()
    removeAll()
    bg = ButtonGroup()
    for (title in answer) {
      val b = makeButton(title)
      buttons.add(b)
      add(b)
      bg.add(b)
    }
  }

  fun updateSelectedButton(v: Any?) {
    if (v is Answer) {
      initButtons()
      when (v) {
        Answer.A -> buttons[0].isSelected = true
        Answer.B -> buttons[1].isSelected = true
        Answer.C -> buttons[2].isSelected = true
      }
    }
  }

  companion object {
    private fun makeButton(title: String): JRadioButton {
      val b = JRadioButton(title)
      b.actionCommand = title
      b.isFocusable = false
      b.isRolloverEnabled = false
      return b
    }
  }

  init {
    layout = BoxLayout(this, BoxLayout.X_AXIS)
    initButtons()
  }
}

private class RadioButtonsRenderer : TableCellRenderer {
  private val renderer = RadioButtonsPanel()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.updateSelectedButton(value)
    return renderer
  }
}

private class RadioButtonsEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = RadioButtonsPanel()
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ): Component {
    renderer.updateSelectedButton(value)
    return renderer
  }

  override fun getCellEditorValue() = Answer.valueOf(renderer.bg.selection.actionCommand)

  init {
    val al = ActionListener { fireEditingStopped() }
    for (b in renderer.buttons) {
      b.addActionListener(al)
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
