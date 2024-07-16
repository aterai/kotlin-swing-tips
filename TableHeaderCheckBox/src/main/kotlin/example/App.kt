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

private const val CHECKBOX_COLUMN = 0
private val columnNames = arrayOf(Status.INDETERMINATE, "Integer", "String")
private val data = arrayOf(
  arrayOf(true, 1, "BBB"),
  arrayOf(false, 12, "AAA"),
  arrayOf(true, 2, "DDD"),
  arrayOf(false, 5, "CCC"),
  arrayOf(true, 3, "EEE"),
  arrayOf(false, 6, "GGG"),
  arrayOf(true, 4, "FFF"),
  arrayOf(false, 7, "HHH"),
)
private val model = object : DefaultTableModel(data, columnNames) {
  override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
}
private val table = object : JTable(model) {
  private var handler: HeaderCheckBoxHandler? = null

  override fun updateUI() {
    // Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
    // https://bugs.openjdk.org/browse/JDK-6788475
    // Set a temporary ColorUIResource to avoid this issue
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    getTableHeader()?.removeMouseListener(handler)
    model?.removeTableModelListener(handler)
    super.updateUI()

    model?.also {
      for (i in 0..<it.columnCount) {
        val r = getDefaultRenderer(it.getColumnClass(i)) as? Component ?: continue
        SwingUtilities.updateComponentTreeUI(r)
      }
      handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
      it.addTableModelListener(handler)
      getTableHeader().addMouseListener(handler)
    }
    getColumnModel().getColumn(CHECKBOX_COLUMN).also {
      it.headerRenderer = HeaderRenderer()
      it.headerValue = Status.INDETERMINATE
    }
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    if (c is JCheckBox) {
      c.background = getSelectionBackground()
      c.isBorderPainted = true
    }
    return c
  }
}

fun makeUI() = JPanel(BorderLayout()).also {
  table.fillsViewportHeight = true
  it.add(JScrollPane(table))
  it.preferredSize = Dimension(320, 240)
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
    val status = value as? Status ?: Status.INDETERMINATE
    when (status) {
      Status.SELECTED -> updateCheckBox(isSelected = true, isEnabled = true)
      Status.DESELECTED -> updateCheckBox(isSelected = false, isEnabled = true)
      Status.INDETERMINATE -> updateCheckBox(isSelected = true, isEnabled = false)
      // else -> throw AssertionError("Unknown Status")
    }
    check.isOpaque = false
    check.font = table.font
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, status, isSelected, hasFocus, row, column)
    (c as? JLabel)?.also {
      label.icon = ComponentIcon(check)
      it.icon = ComponentIcon(label)
      it.text = null // XXX: Nimbus???
    }
    return c
  }

  private fun updateCheckBox(
    isSelected: Boolean,
    isEnabled: Boolean,
  ) {
    check.isSelected = isSelected
    check.isEnabled = isEnabled
  }
}

private class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumn: Int,
) : MouseAdapter(),
  TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    if (e.type == TableModelEvent.UPDATE && e.column == targetColumn) {
      val vci = table.convertColumnIndexToView(targetColumn)
      val column = table.columnModel.getColumn(vci)
      val status = column.headerValue
      val m = table.model
      if (m is DefaultTableModel && fireUpdateEvent(m, column, status)) {
        val h = table.tableHeader
        h.repaint(h.getHeaderRect(vci))
      }
    }
  }

  private fun fireUpdateEvent(
    m: DefaultTableModel,
    column: TableColumn,
    status: Any,
  ) = if (Status.INDETERMINATE == status) {
    val dv = m.dataVector
    val l = dv.mapNotNull { (it as? List<*>)?.get(targetColumn) as? Boolean }.distinct()
    val isOnlyOneSelected = l.size == 1
    if (isOnlyOneSelected) {
      // column.setHeaderValue(if (l.get(0)) Status.SELECTED else Status.DESELECTED)
      column.headerValue = if (l.first()) Status.SELECTED else Status.DESELECTED
      true
    } else {
      false
    }
  } else {
    column.headerValue = Status.INDETERMINATE
    true
  }

  override fun mouseClicked(e: MouseEvent) {
    val tbl = (e.component as? JTableHeader)?.table ?: return
    val m = tbl.model
    val vci = tbl.columnAtPoint(e.point)
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumn && m.rowCount > 0) {
      val columnModel = tbl.columnModel
      val column = columnModel.getColumn(vci)
      val b = Status.DESELECTED === column.headerValue
      for (i in 0..<m.rowCount) {
        m.setValueAt(b, i, mci)
      }
      column.headerValue = if (b) Status.SELECTED else Status.DESELECTED
    }
  }
}

private class ComponentIcon(
  private val cmp: Component,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    SwingUtilities.paintComponent(g, cmp, c?.parent, x, y, iconWidth, iconHeight)
  }

  override fun getIconWidth() = cmp.preferredSize.width

  override fun getIconHeight() = cmp.preferredSize.height
}

private enum class Status {
  SELECTED,
  DESELECTED,
  INDETERMINATE,
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
