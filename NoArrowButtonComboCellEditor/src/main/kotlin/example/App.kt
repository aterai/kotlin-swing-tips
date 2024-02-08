package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.EventObject
import javax.swing.*
import javax.swing.plaf.basic.BasicComboBoxUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor

fun makeUI(): Component {
  val zid = ZoneId.systemDefault()
  val columnNames = arrayOf("LocalDateTime", "String", "Boolean")
  val data = arrayOf(
    arrayOf(LocalDateTime.now(zid), "aaa", true),
    arrayOf(LocalDateTime.now(zid), "bbb", false),
    arrayOf(LocalDateTime.now(zid), "CCC", true),
    arrayOf(LocalDateTime.now(zid), "DDD", false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = JTable(model)
  table.autoCreateRowSorter = true
  table.columnModel.getColumn(0).cellRenderer = LocalDateTimeTableCellRenderer()
  table.columnModel.getColumn(0).cellEditor = LocalDateTimeTableCellEditor()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class LocalDateTimeTableCellRenderer : DefaultTableCellRenderer() {
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
    if (c is JLabel && value is TemporalAccessor) {
      text = DateTimeFormatter.ofPattern(DATE_PATTERN).format(value)
    }
    return c
  }

  companion object {
    private const val DATE_PATTERN = "yyyy/MM/dd"
  }
}

private class ZeroSizeButtonUI : BasicComboBoxUI() {
  override fun createArrowButton() = object : JButton() {
    override fun getPreferredSize() = Dimension()

    override fun isVisible() = false

    override fun updateUI() {
      super.updateUI()
      border = BorderFactory.createEmptyBorder()
    }
  }
}

private class LocalDateTimeTableCellEditor : AbstractCellEditor(), TableCellEditor {
  private val comboBox = object : JComboBox<LocalDateTime>() {
    override fun updateUI() {
      super.updateUI()
      UIManager.put("ComboBox.squareButton", false)
      putClientProperty("JComboBox.isTableCellEditor", true)
      border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
      isOpaque = false
      setRenderer(LocalDateTimeCellRenderer())
      setUI(ZeroSizeButtonUI())
    }
  }
  private var selectedDate: LocalDateTime? = null

  override fun getTableCellEditorComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is LocalDateTime) {
      comboBox.model = object : DefaultComboBoxModel<LocalDateTime>() {
        override fun getElementAt(index: Int) =
          LocalDateTime.now(ZoneId.systemDefault()).plusDays(index.toLong())

        override fun getSize() = 7 // in a week

        override fun getSelectedItem() = selectedDate

        override fun setSelectedItem(anItem: Any) {
          selectedDate = anItem as? LocalDateTime
        }
      }
      comboBox.model.selectedItem = value
    }
    comboBox.border = UIManager.getBorder("Table.focusCellHighlightBorder")
    return comboBox
  }

  override fun getCellEditorValue() = comboBox.selectedItem ?: ""

  override fun shouldSelectCell(e: EventObject): Boolean {
    return e !is MouseEvent || e.id != MouseEvent.MOUSE_DRAGGED
  }

  override fun stopCellEditing(): Boolean {
    if (comboBox.isEditable) {
      // Commit edited value.
      comboBox.actionPerformed(ActionEvent(this, 0, ""))
    }
    return super.stopCellEditing()
  }

  override fun isCellEditable(e: EventObject) = true
}

private class LocalDateTimeCellRenderer : ListCellRenderer<LocalDateTime> {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN)
  private val renderer = DefaultListCellRenderer()

  override fun getListCellRendererComponent(
    list: JList<out LocalDateTime>,
    value: LocalDateTime?,
    index: Int,
    isSelected: Boolean,
    cellHasFocus: Boolean,
  ): Component {
    val c = renderer.getListCellRendererComponent(
      list,
      value,
      index,
      isSelected,
      cellHasFocus,
    )
    if (c is JLabel && value != null) {
      c.isOpaque = true
      c.text = dateTimeFormatter.format(value)
    }
    if (isSelected) {
      c.background = list.selectionBackground
      c.foreground = list.selectionForeground
    } else {
      c.background = list.background
      c.foreground = list.foreground
    }
    return c
  }

  companion object {
    private const val DATE_PATTERN = "yyyy/MM/dd"
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
