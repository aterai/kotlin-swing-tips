package example

import java.awt.*
import java.awt.event.ItemEvent
import java.awt.event.ItemListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import javax.swing.*
import javax.swing.plaf.basic.BasicComboPopup
import javax.swing.plaf.metal.MetalComboBoxUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val wtf = JTextField(5)
  wtf.isEditable = false

  val htf = JTextField(5)
  htf.isEditable = false

  val model = createTableModel()
  val combo = DropdownTableComboBox(PaperSize.entries.toTypedArray(), model)
  combo.addItemListener { e ->
    val item = e.getItem()
    if (e.stateChange == ItemEvent.SELECTED && item is PaperSize) {
      wtf.text = item.width.toString()
      htf.text = item.height.toString()
    }
  }
  val renderer = combo.renderer
  combo.setRenderer(PaperSizeListCellRenderer(renderer))
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

private fun createTableModel(): DefaultTableModel {
  val columnNames = arrayOf("A series", "width", "height")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(
      column: Int,
    ) = if (column == 0) String::class.java else Int::class.java

    override fun isCellEditable(row: Int, column: Int) = false
  }
  for (v in PaperSize.entries) {
    val row = arrayOf<Any>(v.series, v.width, v.height)
    model.addRow(row)
  }
  return model
}

private class PaperSizeListCellRenderer(
  private val renderer: ListCellRenderer<in PaperSize>,
) : ListCellRenderer<PaperSize> {
  override fun getListCellRendererComponent(
    list: JList<out PaperSize>,
    value: PaperSize?,
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
    if (isSelected) {
      c.background = list.selectionBackground
      c.foreground = list.selectionForeground
    } else {
      c.background = list.background
      c.foreground = list.foreground
    }
    (c as? JLabel)?.also { label ->
      label.isOpaque = true
      label.text = value?.series ?: ""
    }
    return c
  }
}

private class DropdownTable : JTable() {
  private var highlighter: RowHighlightListener? = null

  override fun prepareRenderer(
    renderer: TableCellRenderer?,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if (highlighter?.isHighlightedRow(row) == true) {
      c.setForeground(getSelectionForeground())
      c.setBackground(getSelectionBackground().brighter())
    } else if (isRowSelected(row)) {
      c.setForeground(getSelectionForeground())
      c.setBackground(getSelectionBackground())
    } else {
      c.setForeground(getForeground())
      c.setBackground(getBackground())
    }
    return c
  }

  override fun updateUI() {
    removeMouseListener(highlighter)
    removeMouseMotionListener(highlighter)
    super.updateUI()
    highlighter = RowHighlightListener()
    addMouseListener(highlighter)
    addMouseMotionListener(highlighter)
    setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
    getTableHeader().setReorderingAllowed(false)
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
  private val table = DropdownTable()

  init {
    table.model = tableModel
  }

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      setUI(object : MetalComboBoxUI() {
        override fun createPopup() = ComboTablePopup(comboBox, table)
      })
    }
  }
}

private class ComboTablePopup(
  combo: JComboBox<Any>,
  private val table: JTable,
) : BasicComboPopup(combo) {
  private val scroll = JScrollPane(table)
  private val itemListener2: ItemListener
  private val mouseListener2: MouseListener

  init {
    itemListener2 = ItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
        setRowSelection(combo.getSelectedIndex())
      }
    }
    combo.addItemListener(itemListener2)

    mouseListener2 = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        val row = table.rowAtPoint(e.getPoint())
        if (row >= 0) {
          combo.setSelectedIndex(row)
          setVisible(false)
        }
      }
    }
    table.addMouseListener(mouseListener2)
  }

  override fun uninstallingUI() {
    comboBox.removeItemListener(itemListener2)
    table.removeMouseListener(mouseListener2)
    super.uninstallingUI()
  }

  // JPopupMenu#setVisible(true) is called from both BasicComboPopup#show()
  // (keyboard: Alt+Down, F4, ...) and #togglePopup() (mouse click)
  override fun setVisible(visible: Boolean) {
    if (visible) {
      val ins = scroll.getInsets()
      val tableHeight = table.getPreferredSize().height
      val headerHeight = table.getTableHeader().getPreferredSize().height
      val scrollHeight = tableHeight + headerHeight + ins.top + ins.bottom
      scroll.preferredSize = Dimension(POPUP_WIDTH, scrollHeight)
      removeAll()
      add(scroll)
      setBorderPainted(false)
      // setBorder(BorderFactory.createEmptyBorder());
    }
    super.setVisible(visible)
  }

  private fun setRowSelection(index: Int) {
    if (index >= 0) {
      table.setRowSelectionInterval(index, index)
      table.scrollRectToVisible(table.getCellRect(index, 0, true))
    }
  }

  companion object {
    private const val POPUP_WIDTH = 240
  }
}

private class RowHighlightListener : MouseAdapter() {
  private var highlightedRow = -1

  fun isHighlightedRow(row: Int) = highlightedRow == row

  private fun updateHighlightedRow(e: MouseEvent) {
    (e.component as? JTable)?.also {
      val row = it.rowAtPoint(e.point)
      if (row != highlightedRow) {
        highlightedRow = row
        it.repaint()
      }
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    updateHighlightedRow(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    updateHighlightedRow(e)
  }

  override fun mouseExited(e: MouseEvent) {
    highlightedRow = -1
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
