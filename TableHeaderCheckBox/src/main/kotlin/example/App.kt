package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

class MainPanel : JPanel(BorderLayout()) {
  private val columnNames = arrayOf(Status.INDETERMINATE, "Integer", "String")
  private val data = arrayOf(
    arrayOf(true, 1, "BBB"),
    arrayOf(false, 12, "AAA"),
    arrayOf(true, 2, "DDD"),
    arrayOf(false, 5, "CCC"),
    arrayOf(true, 3, "EEE"),
    arrayOf(false, 6, "GGG"),
    arrayOf(true, 4, "FFF"),
    arrayOf(false, 7, "HHH")
  )
  private val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  private val table = object : JTable(model) {
    @Transient
    private var handler: HeaderCheckBoxHandler? = null

    override fun updateUI() {
      // [JDK-6788475] Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
      // https://bugs.openjdk.java.net/browse/JDK-6788475
      // XXX: set dummy ColorUIResource
      setSelectionForeground(ColorUIResource(Color.RED))
      setSelectionBackground(ColorUIResource(Color.RED))
      getTableHeader()?.removeMouseListener(handler)
      getModel()?.removeTableModelListener(handler)
      super.updateUI()

      getModel()?.also {
        for (i in 0 until it.getColumnCount()) {
          val r = getDefaultRenderer(it.getColumnClass(i)) as? Component ?: continue
          SwingUtilities.updateComponentTreeUI(r)
        }
        handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN)
        it.addTableModelListener(handler)
        getTableHeader().addMouseListener(handler)
      }
      getColumnModel().getColumn(CHECKBOX_COLUMN).also {
        it.setHeaderRenderer(HeaderRenderer())
        it.setHeaderValue(Status.INDETERMINATE)
      }
    }

    override fun prepareEditor(editor: TableCellEditor, row: Int, column: Int): Component {
      val c = super.prepareEditor(editor, row, column)
      (c as? JCheckBox)?.also {
        it.setBackground(getSelectionBackground())
        it.setBorderPainted(true)
      }
      return c
    }
  }

  init {
    table.setFillsViewportHeight(true)
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }

  companion object {
    private const val CHECKBOX_COLUMN = 0
  }
}

class HeaderRenderer : TableCellRenderer {
  private val check = JCheckBox("")
  private val label = JLabel("Check All")

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val status = value as? Status ?: Status.INDETERMINATE
    when (status) {
      Status.SELECTED -> updateCheckBox(isSelected = true, isEnabled = true)
      Status.DESELECTED -> updateCheckBox(isSelected = false, isEnabled = true)
      Status.INDETERMINATE -> updateCheckBox(isSelected = true, isEnabled = false)
      // else -> throw AssertionError("Unknown Status")
    }
    check.setOpaque(false)
    check.setFont(table.getFont())
    val r = table.getTableHeader().getDefaultRenderer()
    val c = r.getTableCellRendererComponent(table, status, isSelected, hasFocus, row, column)
    (c as? JLabel)?.also {
      label.setIcon(ComponentIcon(check))
      it.setIcon(ComponentIcon(label))
      it.setText(null) // XXX: Nimbus???
    }
    return c
  }

  private fun updateCheckBox(isSelected: Boolean, isEnabled: Boolean) {
    check.setSelected(isSelected)
    check.setEnabled(isEnabled)
  }
}

class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumnIndex: Int
) : MouseAdapter(), TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    if (e.getType() == TableModelEvent.UPDATE && e.getColumn() == targetColumnIndex) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
      val column = table.getColumnModel().getColumn(vci)
      val status = column.getHeaderValue()
      val m = table.getModel()
      if (m is DefaultTableModel && fireUpdateEvent(m, column, status)) {
        val h = table.getTableHeader()
        h.repaint(h.getHeaderRect(vci))
      }
    }
  }

  private fun fireUpdateEvent(m: DefaultTableModel, column: TableColumn, status: Any): Boolean {
    return if (Status.INDETERMINATE == status) {
      val l = m.getDataVector().mapNotNull { (it as? List<*>)?.get(targetColumnIndex) as? Boolean }.distinct()
      val isOnlyOneSelected = l.size == 1
      if (isOnlyOneSelected) {
        // column.setHeaderValue(if (l.get(0)) Status.SELECTED else Status.DESELECTED)
        column.setHeaderValue(if (l.first()) Status.SELECTED else Status.DESELECTED)
        true
      } else {
        false
      }
    } else {
      column.setHeaderValue(Status.INDETERMINATE)
      true
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val tbl = (e.getComponent() as? JTableHeader)?.getTable() ?: return
    val columnModel = tbl.getColumnModel()
    val m = tbl.getModel()
    val vci = columnModel.getColumnIndexAtX(e.getX())
    val mci = tbl.convertColumnIndexToModel(vci)
    if (mci == targetColumnIndex && m.getRowCount() > 0) {
      val column = columnModel.getColumn(vci)
      val b = Status.DESELECTED === column.getHeaderValue()
      for (i in 0 until m.getRowCount()) {
        m.setValueAt(b, i, mci)
      }
      column.setHeaderValue(if (b) Status.SELECTED else Status.DESELECTED)
    }
  }
}

class ComponentIcon(private val cmp: Component) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    SwingUtilities.paintComponent(g, cmp, c.getParent(), x, y, getIconWidth(), getIconHeight())
  }

  override fun getIconWidth() = cmp.getPreferredSize().width

  override fun getIconHeight() = cmp.getPreferredSize().height
}

enum class Status {
  SELECTED, DESELECTED, INDETERMINATE
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
