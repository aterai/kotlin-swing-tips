package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.Date
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.JSpinner.DateEditor
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val columnNames = arrayOf("Integer", "String", "Date")
  val data = arrayOf(
    arrayOf(-1, "AAA", Date()),
    arrayOf(2, "BBB", Date()),
    arrayOf(-9, "EEE", Date()),
    arrayOf(1, "", Date()),
    arrayOf(10, "CCC", Date()),
    arrayOf(7, "FFF", Date())
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      surrendersFocusOnKeystroke = true
      (getDefaultRenderer(Date::class.java) as? JLabel)?.horizontalAlignment = SwingConstants.LEFT
      setDefaultEditor(Date::class.java, SpinnerCellEditor())
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SpinnerCellEditor : AbstractCellEditor(), TableCellEditor {
  private val spinner = JSpinner(SpinnerDateModel())

  init {
    val editor = DateEditor(spinner, "yyyy/MM/dd")
    spinner.editor = editor
    spinner.border = BorderFactory.createEmptyBorder()
    setArrowButtonEnabled(false)
    editor.textField.horizontalAlignment = SwingConstants.LEFT
    val fl = object : FocusListener {
      override fun focusLost(e: FocusEvent) {
        setArrowButtonEnabled(false)
      }

      override fun focusGained(e: FocusEvent) {
        setArrowButtonEnabled(true)
        EventQueue.invokeLater {
          (e.component as? JTextField)?.also {
            it.caretPosition = 8
            it.selectionStart = 8
            it.selectionEnd = 10
          }
        }
      }
    }
    editor.textField.addFocusListener(fl)
  }

  fun setArrowButtonEnabled(flag: Boolean) {
    for (c in spinner.components) {
      (c as? JButton)?.isEnabled = flag
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ) = spinner.also { it.value = value }

  override fun getCellEditorValue(): Any = spinner.value
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
