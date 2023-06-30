package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("JSpinner", "Buttons")
  val data = arrayOf(
    arrayOf(50, 100),
    arrayOf(100, 50),
    arrayOf(30, 20),
    arrayOf(0, 100)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = Number::class.java
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      EventQueue.invokeLater {
        var column = getColumnModel().getColumn(0)
        column.cellRenderer = SpinnerRenderer()
        column.cellEditor = SpinnerEditor()
        column = getColumnModel().getColumn(1)
        column.cellRenderer = ButtonsRenderer()
        column.cellEditor = ButtonsEditor()
        repaint()
      }
    }
  }
  table.rowHeight = 36
  table.autoCreateRowSorter = true

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SpinnerPanel : JPanel(GridBagLayout()) {
  val spinner = JSpinner(SpinnerNumberModel(100, 0, 200, 1))

  init {
    val c = GridBagConstraints()
    c.weightx = 1.0
    c.insets = Insets(0, 10, 0, 10)
    c.fill = GridBagConstraints.HORIZONTAL
    isOpaque = true
    add(spinner, c)
  }
}

private class SpinnerRenderer : TableCellRenderer {
  private val renderer = SpinnerPanel()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ) = renderer.also {
    it.background = if (isSelected) table.selectionBackground else table.background
    it.spinner.value = value
  }
}

private class SpinnerEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = SpinnerPanel()
  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ) = renderer.also {
    it.background = table.selectionBackground
    it.spinner.value = value
  }

  override fun getCellEditorValue(): Any = renderer.spinner.value

  override fun stopCellEditing() = kotlin.runCatching {
    renderer.spinner.commitEdit()
    true
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(renderer.spinner)
  }.getOrNull() ?: super.stopCellEditing()
}

private class ButtonsPanel : JPanel() {
  val buttons = arrayOf(JButton("+"), JButton("-"))
  val label = object : JLabel(" ", RIGHT) {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = 50
      return d
    }
  }
  var counter = -1

  init {
    isOpaque = true
    add(label)
    for (b in buttons) {
      b.isFocusable = false
      b.isRolloverEnabled = false
      add(b)
    }
  }
}

private class ButtonsRenderer : TableCellRenderer {
  private val renderer = ButtonsPanel()
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ) = renderer.also {
    it.background = if (isSelected) table.selectionBackground else table.background
    it.label.foreground = if (isSelected) table.selectionForeground else table.foreground
    it.label.text = value?.toString() ?: ""
  }
}

private class ButtonsEditor : AbstractCellEditor(), TableCellEditor {
  private val renderer = ButtonsPanel()

  init {
    renderer.buttons[0].addActionListener {
      renderer.counter++
      renderer.label.text = renderer.counter.toString()
      fireEditingStopped()
    }
    renderer.buttons[1].addActionListener {
      renderer.counter--
      renderer.label.text = renderer.counter.toString()
      fireEditingStopped()
    }
    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        fireEditingStopped()
      }
    }
    renderer.addMouseListener(ml)
  }

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int
  ) = renderer.also {
    it.background = table.selectionBackground
    it.label.foreground = table.selectionForeground
    it.counter = value as? Int ?: 0
    it.label.text = renderer.counter.toString()
  }

  override fun getCellEditorValue() = renderer.counter
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
