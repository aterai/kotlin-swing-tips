package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.metal.MetalComboBoxUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val wtf = JTextField(5)
  wtf.isEditable = false

  val htf = JTextField(5)
  htf.isEditable = false

  val columnNames = arrayOf("A series", "width", "height")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) =
      if (column == 1 || column == 2) Number::class.java else String::class.java

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
  for (v in PaperSize.values()) {
    model.addRow(arrayOf(v.series, v.width, v.height))
  }

  val combo = DropdownTableComboBox(PaperSize.values(), model)
  combo.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      val rowData = combo.getItemAt(combo.selectedIndex)
      wtf.text = rowData.width.toString()
      htf.text = rowData.height.toString()
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
        label.text = value?.series ?: ""
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

private enum class PaperSize(
  val series: String,
  val width: Int,
  val height: Int,
) {
  A1("A1", 594, 841),
  A2("A2", 420, 594),
  A3("A3", 297, 420),
  A4("A4", 210, 297),
  A5("A5", 148, 210),
  A6("A6", 105, 148),
  ;

  override fun toString() = "%s(%dx%d)".format(series, width, height)
}

private class DropdownTableComboBox(
  paperSizes: Array<PaperSize>,
  tableModel: TableModel,
) : JComboBox<PaperSize>(paperSizes) {
  private val highlighter = HighlightListener()
  private val table = object : JTable() {
    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ) = super.prepareRenderer(renderer, row, column).also {
      when {
        highlighter.isHighlightedRow(row) -> {
          it.foreground = UIManager.getColor("Table.selectionForeground")
          it.background = UIManager.getColor("Table.selectionBackground").brighter()
        }

        isRowSelected(row) -> {
          it.foreground = UIManager.getColor("Table.selectionForeground")
          it.background = UIManager.getColor("Table.selectionBackground")
        }

        else -> {
          it.foreground = UIManager.getColor("Table.foreground")
          it.background = UIManager.getColor("Table.background")
        }
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

  init {
    table.model = tableModel
  }

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      val tmp = object : MetalComboBoxUI() {
        override fun createPopup() = ComboTablePopup(comboBox, table)
      }
      setUI(tmp)
      setEditable(false)
    }
  }
}

private class ComboTablePopup(
  combo: JComboBox<Any>,
  private val table: JTable,
) : BasicComboPopup(combo) {
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
    // border = BorderFactory.createEmptyBorder()
  }

  override fun togglePopup() {
    if (!isVisible) {
      val ins = scroll.insets
      val tableHeight = table.preferredSize.height
      val headerHeight = table.tableHeader.preferredSize.height
      val scrollHeight = tableHeight + headerHeight + ins.top + ins.bottom
      scroll.preferredSize = Dimension(240, scrollHeight)
      super.removeAll()
      super.add(scroll)
      // border = BorderFactory.createEmptyBorder()
      isBorderPainted = false
    }
    super.togglePopup()
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
