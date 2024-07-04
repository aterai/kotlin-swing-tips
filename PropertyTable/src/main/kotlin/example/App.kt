package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.util.Date
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("Type", "Value")
  val data = arrayOf(
    arrayOf<Any>("String", "text"),
    arrayOf<Any>("Date", Date()),
    arrayOf<Any>("Integer", 12),
    arrayOf<Any>("Double", 3.45),
    arrayOf<Any>("Boolean", true),
    arrayOf<Any>("Color", Color.RED),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(PropertyTable(model)))
    it.preferredSize = Dimension(320, 240)
  }
}

class PropertyTable(model: TableModel) : JTable(model) {
  private var editingClass: Class<*>? = null

  private fun getClassAt(
    row: Int,
    column: Int,
  ): Class<*> {
    val mc = convertColumnIndexToModel(column)
    val mr = convertRowIndexToModel(row)
    return model.getValueAt(mr, mc).javaClass
  }

  override fun updateUI() {
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    super.updateUI()
    setDefaultRenderer(Color::class.java, ColorRenderer())
    setDefaultEditor(Color::class.java, ColorEditor())
    setDefaultEditor(Date::class.java, DateEditor())
  }

  override fun getCellRenderer(
    row: Int,
    column: Int,
  ): TableCellRenderer = if (convertColumnIndexToModel(column) == TARGET_COLUMN) {
    getDefaultRenderer(getClassAt(row, column))
  } else {
    super.getCellRenderer(row, column)
  }

  override fun getCellEditor(
    row: Int,
    column: Int,
  ): TableCellEditor = if (convertColumnIndexToModel(column) == TARGET_COLUMN) {
    editingClass = getClassAt(row, column)
    getDefaultEditor(editingClass)
  } else {
    editingClass = null
    super.getCellEditor(row, column)
  }

  override fun getColumnClass(
    column: Int,
  ) = if (convertColumnIndexToModel(column) == TARGET_COLUMN) {
    editingClass
  } else {
    super.getColumnClass(column)
  }

  companion object {
    private const val TARGET_COLUMN = 1
  }
}

class DateEditor : AbstractCellEditor(), TableCellEditor {
  private val spinner = JSpinner(SpinnerDateModel())

  init {
    val editor = JSpinner.DateEditor(spinner, "yyyy/MM/dd")
    spinner.editor = editor
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
    spinner.border = BorderFactory.createEmptyBorder()
  }

  private fun setArrowButtonEnabled(flag: Boolean) {
    for (c in spinner.components) {
      (c as? JButton)?.isEnabled = flag
    }
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    spinner.value = value
    return spinner
  }

  override fun getCellEditorValue(): Any = spinner.value

  override fun stopCellEditing(): Boolean {
    val stopEditing = runCatching {
      spinner.commitEdit()
    }.onFailure {
      UIManager.getLookAndFeel().provideErrorFeedback(spinner)
    }.isSuccess
    return stopEditing && super.stopCellEditing()
  }
}

class ColorRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel && value is Color) {
      c.icon = ColorIcon(value)
      c.text = "(${value.red}, ${value.green}, ${value.blue})"
    }
    return c
  }
}

class ColorEditor : AbstractCellEditor(), TableCellEditor, ActionListener {
  private val button = JButton()
  private val colorChooser: JColorChooser
  private val dialog: JDialog
  private var currentColor = Color.WHITE

  init {
    button.actionCommand = EDIT
    button.addActionListener(this)
    button.isContentAreaFilled = false
    button.isFocusPainted = false
    button.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    button.isOpaque = false
    button.horizontalAlignment = SwingConstants.LEFT
    button.horizontalTextPosition = SwingConstants.RIGHT
    colorChooser = JColorChooser()
    dialog = JColorChooser.createDialog(button, "Pick a Color", true, colorChooser, this, null)
  }

  override fun actionPerformed(e: ActionEvent) {
    if (EDIT == e.actionCommand) {
      button.background = currentColor
      button.icon = ColorIcon(currentColor)
      colorChooser.color = currentColor
      dialog.isVisible = true
      fireEditingStopped()
    } else {
      currentColor = colorChooser.color
    }
  }

  override fun getCellEditorValue(): Color = currentColor

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    currentColor = value as? Color
    button.icon = ColorIcon(currentColor)
    val r = currentColor.red
    val g = currentColor.green
    val b = currentColor.blue
    button.text = "($r, $g, $b)"
    return button
  }

  companion object {
    private const val EDIT = "edit"
  }
}

class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.dispose()
  }

  override fun getIconWidth() = 10

  override fun getIconHeight() = 10
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
