package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf(Status.INDETERMINATE, "Integer", "String")
  val data = arrayOf(
    arrayOf(true, 1, "BBB"),
    arrayOf(false, 12, "AAA"),
    arrayOf(true, 2, "DDD"),
    arrayOf(false, 5, "CCC"),
    arrayOf(true, 3, "EEE"),
    arrayOf(false, 6, "GGG"),
    arrayOf(true, 4, "FFF"),
    arrayOf(false, 7, "HHH"),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val CHECKBOX_COLUMN = 0

    private var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader().removeMouseListener(handler)
      getModel()?.removeTableModelListener(handler)
      super.updateUI()
      val m = getModel()
      for (i in 0 until m.columnCount) {
        val r = getDefaultRenderer(m.getColumnClass(i))
        SwingUtilities.updateComponentTreeUI(r as? Component)
      }
      getColumnModel().getColumn(CHECKBOX_COLUMN).also {
        it.headerRenderer = HeaderRenderer()
        it.headerValue = Status.INDETERMINATE
      }
      handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
      m.addTableModelListener(handler)
      getTableHeader().addMouseListener(handler)
    }

    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ) = super.prepareEditor(editor, row, column).also {
      if (it is JCheckBox) {
        it.background = getSelectionBackground()
        it.isBorderPainted = true
      }
    }
  }
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HeaderRenderer : TableCellRenderer {
  private val check = JCheckBox("")
  private val label = JLabel("Check All")

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    if (value is Status) {
      when (value) {
        Status.SELECTED -> {
          check.isSelected = true
          check.isEnabled = true
        }

        Status.DESELECTED -> {
          check.isSelected = false
          check.isEnabled = true
        }

        Status.INDETERMINATE -> {
          check.isSelected = true
          check.isEnabled = false
        }
      }
    } else {
      check.isSelected = true
      check.isEnabled = false
    }
    check.isOpaque = false
    check.font = table.font
    val r = table.tableHeader.defaultRenderer
    val l = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    label.icon = ComponentIcon(check)
    if (l is JLabel) {
      l.icon = ComponentIcon(label)
      l.text = null // XXX: Nimbus???
    }
    return l
  }
}

private class ComponentIcon(private val c: Component) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    SwingUtilities.paintComponent(g, this.c, c?.parent, x, y, iconWidth, iconHeight)
  }

  override fun getIconWidth() = c.preferredSize.width

  override fun getIconHeight() = c.preferredSize.height
}

private enum class Status {
  SELECTED,
  DESELECTED,
  INDETERMINATE,
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add(true)").addActionListener { addRowActionPerformed(true) }
    add("add(false)").addActionListener { addRowActionPerformed(false) }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }

  private fun addRowActionPerformed(isSelected: Boolean) {
    val table = invoker as? JTable
    val model = table?.model
    if (model is DefaultTableModel) {
      model.addRow(arrayOf(isSelected, 0, ""))
      val rect = table.getCellRect(model.rowCount - 1, 0, true)
      table.scrollRectToVisible(rect)
    }
  }
}

private class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumnIndex: Int,
) : MouseAdapter(), TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    val vci = table.convertColumnIndexToView(targetColumnIndex)
    val col = table.columnModel.getColumn(vci)
    val hv = col.headerValue
    val m = table.model
    val repaint = when (e.type) {
      TableModelEvent.DELETE -> fireDeleteEvent(m, col, hv)
      TableModelEvent.INSERT -> hv !== Status.INDETERMINATE && fireInsertEvent(m, col, hv, e)
      TableModelEvent.UPDATE -> e.column == targetColumnIndex && fireUpdateEvent(m, col, hv)
      else -> false
    }
    if (repaint) {
      val h = table.tableHeader
      h.repaint(h.getHeaderRect(vci))
    }
  }

  private fun fireDeleteEvent(
    m: TableModel,
    column: TableColumn,
    status: Any,
  ): Boolean {
    if (m.rowCount == 0) {
      column.headerValue = Status.DESELECTED
    } else if (status === Status.INDETERMINATE) {
      var selected = true
      var deselected = true
      for (i in 0 until m.rowCount) {
        val b = m.getValueAt(i, targetColumnIndex) as? Boolean ?: false
        selected = selected and b
        deselected = deselected and !b
      }
      when {
        deselected -> column.headerValue = Status.DESELECTED
        selected -> column.headerValue = Status.SELECTED
      }
    }
    return true
  }

  private fun fireInsertEvent(
    m: TableModel,
    column: TableColumn,
    status: Any,
    e: TableModelEvent,
  ): Boolean {
    var selected = status === Status.DESELECTED
    var deselected = status === Status.SELECTED
    for (i in e.firstRow until e.lastRow + 1) {
      val b = m.getValueAt(i, targetColumnIndex) as? Boolean ?: false
      selected = selected and b
      deselected = deselected and !b
    }
    if (selected && m.rowCount == 1) {
      column.headerValue = Status.SELECTED
    } else if (selected || deselected) {
      column.headerValue = Status.INDETERMINATE
    } else {
      return false
    }
    return true
  }

  private fun fireUpdateEvent(
    m: TableModel,
    column: TableColumn,
    status: Any,
  ): Boolean {
    if (status === Status.INDETERMINATE) {
      var selected = true
      var deselected = true
      for (i in 0 until m.rowCount) {
        val b = m.getValueAt(i, targetColumnIndex) as? Boolean ?: false
        selected = selected and b
        deselected = deselected and !b
        if (selected == deselected) {
          return false
        }
      }
      column.headerValue = if (deselected) Status.DESELECTED else Status.SELECTED
    } else {
      column.headerValue = Status.INDETERMINATE
    }
    return true
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val tbl = header.table
    val columnModel = tbl.columnModel
    val m = tbl.model
    val vci = columnModel.getColumnIndexAtX(e.x)
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumnIndex && m.rowCount > 0) {
      val column = columnModel.getColumn(vci)
      val b = column.headerValue === Status.DESELECTED
      for (i in 0 until m.rowCount) {
        m.setValueAt(b, i, mci)
      }
      column.headerValue = if (b) Status.SELECTED else Status.DESELECTED
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
