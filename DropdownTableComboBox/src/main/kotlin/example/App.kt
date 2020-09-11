package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.metal.MetalComboBoxUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val aSeries = listOf(
    listOf("A1", 594, 841),
    listOf("A2", 420, 594),
    listOf("A3", 297, 420),
    listOf("A4", 210, 297),
    listOf("A5", 148, 210),
    listOf("A6", 105, 148)
  )

  val columns = arrayOf("A series", "width", "height")

  val wtf = JTextField(5)
  wtf.isEditable = false

  val htf = JTextField(5)
  htf.isEditable = false

  val model = object : DefaultTableModel(null, columns) {
    override fun getColumnClass(column: Int) =
      if (column == 1 || column == 2) Integer::class.java else String::class.java

    override fun isCellEditable(row: Int, column: Int) = false
  }

  val combo = DropdownTableComboBox(aSeries, model)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val rowData = combo.getSelectedRow()
      wtf.text = rowData[1].toString()
      htf.text = rowData[2].toString()
    }
  }
  val renderer = combo.renderer
  combo.setRenderer { list, value, index, isSelected, cellHasFocus ->
    renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
      if (isSelected) {
        it.background = list.selectionBackground
        it.foreground = list.selectionForeground
      } else {
        it.background = list.background
        it.foreground = list.foreground
      }
      (it as? JLabel)?.also { label ->
        label.isOpaque = true
        label.text = value?.get(0)?.toString() ?: ""
      }
    }
  }

  EventQueue.invokeLater { combo.setSelectedIndex(3) }

  val box = Box.createHorizontalBox().also {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    it.add(combo)
    it.add(Box.createHorizontalStrut(15))
    it.add(JLabel("width: "))
    it.add(wtf)
    it.add(Box.createHorizontalStrut(5))
    it.add(JLabel("height: "))
    it.add(htf)
    it.add(Box.createHorizontalGlue())
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DropdownTableComboBox<E : List<Any>>(
  private val list: List<E>,
  model: DefaultTableModel
) : JComboBox<E>() {
  @Transient private val highlighter = HighlightListener()
  private val table = object : JTable() {
    override fun prepareRenderer(renderer: TableCellRenderer, row: Int, column: Int) =
      super.prepareRenderer(renderer, row, column).also {
        it.foreground = Color.BLACK
        it.background = when {
          highlighter.isHighlightedRow(row) -> Color(0xFF_C8_C8)
          isRowSelected(row) -> Color.CYAN
          else -> Color.WHITE
        }
      }

    override fun updateUI() {
      removeMouseListener(highlighter)
      removeMouseMotionListener(highlighter)
      super.updateUI()
      addMouseListener(highlighter)
      addMouseMotionListener(highlighter)
      getTableHeader().reorderingAllowed = false
    }
  }

  fun getSelectedRow() = list[selectedIndex]

  init {
    table.model = model
    list.forEach { this.addItem(it) }
    list.forEach { model.addRow(it.toTypedArray()) }
  }

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      ui = object : MetalComboBoxUI() {
        override fun createPopup() = ComboTablePopup(comboBox, table)
      }
      setEditable(false)
    }
  }
}

private class ComboTablePopup(combo: JComboBox<*>, private val table: JTable) : BasicComboPopup(combo) {
  private val scroll: JScrollPane

  init {
    val sm = table.selectionModel
    sm.selectionMode = ListSelectionModel.SINGLE_SELECTION
    sm.addListSelectionListener { combo.setSelectedIndex(table.selectedRow) }

    combo.addItemListener { e ->
      if (e.stateChange == ItemEvent.SELECTED) {
        setRowSelection(combo.selectedIndex)
      }
    }

    val ml = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        combo.selectedIndex = table.rowAtPoint(e.point)
        isVisible = false
      }
    }
    table.addMouseListener(ml)

    scroll = JScrollPane(table)
    border = BorderFactory.createEmptyBorder()
  }

  override fun show() {
    if (isEnabled) {
      val ins = scroll.insets
      val tableHt = table.preferredSize.height
      val headerHt = table.tableHeader.preferredSize.height
      scroll.preferredSize = Dimension(240, tableHt + headerHt + ins.top + ins.bottom)
      super.removeAll()
      super.add(scroll)
      setRowSelection(comboBox.selectedIndex)
      super.show(comboBox, 0, comboBox.bounds.height)
    }
  }

  private fun setRowSelection(index: Int) {
    if (index != -1) {
      table.setRowSelectionInterval(index, index)
      table.scrollRectToVisible(table.getCellRect(index, 0, true))
    }
  }
}

private class HighlightListener : MouseAdapter() {
  private var viewRowIndex = -1

  fun isHighlightedRow(row: Int) = this.viewRowIndex == row

  private fun setHighlightedTableCell(e: MouseEvent) {
    (e.component as? JTable)?.also {
      viewRowIndex = it.rowAtPoint(e.point)
      it.repaint()
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    setHighlightedTableCell(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    setHighlightedTableCell(e)
  }

  override fun mouseExited(e: MouseEvent) {
    viewRowIndex = -1
    e.component.repaint()
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
