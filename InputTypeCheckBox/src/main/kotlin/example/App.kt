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
    private val checkBoxColumnIndex = 0
    private var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader().removeMouseListener(handler)
      var m = getModel()
      m?.removeTableModelListener(handler)
      super.updateUI()
      m = getModel()
      for (i in 0..<m.columnCount) {
        val r = getDefaultRenderer(m.getColumnClass(i))
        if (r is Component) {
          SwingUtilities.updateComponentTreeUI(r)
        }
      }
      val column = getColumnModel().getColumn(checkBoxColumnIndex)
      column.headerRenderer = HeaderRenderer()
      column.headerValue = Status.INDETERMINATE
      handler = HeaderCheckBoxHandler(this, checkBoxColumnIndex)
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

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class HeaderRenderer : TableCellRenderer {
  private val html = """
    <html>
      <table cellpadding='0' cellspacing='0'>
        <td><input type='checkbox'></td>
        <td>&nbsp;Check All</td>
      </table>
    </html>
  """.trimIndent()

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, html, isSelected, hasFocus, row, column)
    (c as? Container)?.components?.forEach {
      updateCheckBox((it as? Container)?.getComponent(0), value)
    }
    return c
  }

  private fun updateCheckBox(
    c: Component?,
    value: Any?,
  ) {
    if (c is JCheckBox) {
      c.isOpaque = false
      c.border = BorderFactory.createEmptyBorder()
      if (value is Status) {
        when (value) {
          Status.SELECTED -> {
            c.isSelected = true
            c.isEnabled = true
          }

          Status.DESELECTED -> {
            c.isSelected = false
            c.isEnabled = true
          }

          Status.INDETERMINATE -> {
            c.isSelected = true
            c.isEnabled = false
          }
          // else -> throw AssertionError("Unknown Status")
        }
      }
    }
  }
}

private class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumnIndex: Int,
) : MouseAdapter(), TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    if (e.type == TableModelEvent.UPDATE && e.column == targetColumnIndex) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
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
  ): Boolean {
    return if (status === Status.INDETERMINATE) {
      val l = m.dataVector.filterIsInstance<List<*>>()
        .mapNotNull { it[targetColumnIndex] as? Boolean }
        .distinct()
      val notDuplicates = l.size == 1
      if (notDuplicates) {
        val isSelected = l[0]
        column.headerValue = if (isSelected) Status.SELECTED else Status.DESELECTED
        true
      } else {
        false
      }
    } else {
      column.headerValue = Status.INDETERMINATE
      true
    }
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
      for (i in 0..<m.rowCount) {
        m.setValueAt(b, i, mci)
      }
      column.headerValue = if (b) Status.SELECTED else Status.DESELECTED
    }
  }
}

private enum class Status {
  SELECTED,
  DESELECTED,
  INDETERMINATE,
}

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
