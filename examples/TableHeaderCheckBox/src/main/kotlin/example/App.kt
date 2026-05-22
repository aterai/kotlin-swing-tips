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
private val columnNames = arrayOf<Any>(Status.INDETERMINATE, "Integer", "String")
private val data = arrayOf<Array<Any>>(
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

fun createUI() = JPanel(BorderLayout()).also {
  table.fillsViewportHeight = true
  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
  it.add(JScrollPane(table))
  it.preferredSize = Dimension(320, 240)
}

private class HeaderRenderer : TableCellRenderer {
  private val check = JCheckBox()
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
    status.configureHeaderCheckBox(check)
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      c.setOpaque(false)
      check.setOpaque(false)
      val isSynth = check
        .getUI()
        .javaClass
        .getName()
        .contains("Synth")
      if (isSynth) {
        check.setText(" ")
        check.preferredSize = c.getPreferredSize()
      }
      label.setOpaque(false)
      label.setIcon(ComponentIcon(check))
      c.setIcon(ComponentIcon(label))
      c.setText(null)
    }
    return c
  }
}

private class HeaderCheckBoxHandler(
  private val table: JTable,
  private val targetColumnIndex: Int,
) : MouseAdapter(),
  TableModelListener {
  override fun tableChanged(e: TableModelEvent) {
    if (e.getType() == TableModelEvent.UPDATE &&
      e.getColumn() == targetColumnIndex
    ) {
      val vci = table.convertColumnIndexToView(targetColumnIndex)
      val column = table.getColumnModel().getColumn(vci)
      val status = column.getHeaderValue()
      val m = table.model
      if (updateHeaderState(m, column, status)) {
        val h = table.getTableHeader()
        h.repaint(h.getHeaderRect(vci))
      }
    }
  }

  private fun updateHeaderState(
    model: TableModel,
    column: TableColumn,
    status: Any?,
  ): Boolean {
    val repaint: Boolean
    if (status === Status.INDETERMINATE) {
      repaint = updateIndeterminateHeaderState(model, column)
    } else {
      setIndeterminateHeader(column)
      repaint = true
    }
    return repaint
  }

  private fun setIndeterminateHeader(column: TableColumn) {
    column.setHeaderValue(Status.INDETERMINATE)
  }

  private fun updateIndeterminateHeaderState(
    model: TableModel,
    column: TableColumn,
  ): Boolean {
    var repaint = false
    val status = resolveHeaderState(model)
    if (status != null) {
      column.setHeaderValue(status)
      repaint = true
    }
    return repaint
  }

  private fun resolveHeaderState(model: TableModel): Status? {
    var status: Status? = null
    val rowCount = model.rowCount
    if (rowCount > 0) {
      val values = (0..<rowCount)
        .map { i -> model.getValueAt(i, targetColumnIndex) }
        .filterIsInstance<Boolean>()
        .distinct()
        .take(2)
        .toList()
      val isOnlyOneSelected = values.size == 1
      if (isOnlyOneSelected) {
        val isSelected = values[0]
        status = if (isSelected) Status.SELECTED else Status.DESELECTED
      }
    }
    return status
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader
    if (header?.isEnabled == true) {
      val tbl = header.getTable()
      val model = tbl.model
      val vci = tbl.columnAtPoint(e.getPoint())
      val mci = tbl.convertColumnIndexToModel(vci)
      if (mci == targetColumnIndex && model.rowCount > 0) {
        val column = tbl.getColumnModel().getColumn(vci)
        val select = column.getHeaderValue() === Status.DESELECTED
        toggleAllRows(model, mci, select)
        column.setHeaderValue(if (select) Status.SELECTED else Status.DESELECTED)
      }
    }
  }

  private fun toggleAllRows(model: TableModel, columnIndex: Int, selected: Boolean) {
    for (i in 0..<model.rowCount) {
      model.setValueAt(selected, i, columnIndex)
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
  SELECTED {
    override fun configureHeaderCheckBox(check: JCheckBox) {
      check.setSelected(true)
      check.setEnabled(true)
    }
  },
  DESELECTED {
    override fun configureHeaderCheckBox(check: JCheckBox) {
      check.setSelected(false)
      check.setEnabled(true)
    }
  },
  INDETERMINATE {
    override fun configureHeaderCheckBox(check: JCheckBox) {
      check.setSelected(true)
      check.setEnabled(false)
    }
  }, ;

  abstract fun configureHeaderCheckBox(check: JCheckBox)
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
