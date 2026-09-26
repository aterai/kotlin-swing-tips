package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.TableModelEvent
import javax.swing.event.TableModelListener
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.synth.SynthUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

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
  private val columnClasses = arrayOf(
    Boolean::class.javaObjectType,
    Int::class.javaObjectType,
    String::class.java,
  )

  // getValueAt(0, column) throws an exception if the model has no rows
  override fun getColumnClass(column: Int) = columnClasses[column]
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
      val vci = convertColumnIndexToView(CHECKBOX_COLUMN)
      getColumnModel().getColumn(vci).headerRenderer = HeaderRenderer()
      handler = HeaderCheckBoxHandler(this, CHECKBOX_COLUMN).also { h ->
        h.updateHeaderState()
        it.addTableModelListener(h)
        getTableHeader().addMouseListener(h)
      }
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
  private val icon = ComponentIcon(label)

  init {
    check.isOpaque = false
    label.isOpaque = false
    label.icon = ComponentIcon(check)
    if (isSynth()) {
      check.text = " "
    }
  }

  private fun isSynth() = check.ui is SynthUI

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
      c.isOpaque = false
      if (isSynth()) {
        check.preferredSize = c.preferredSize
      }
      c.icon = icon
      c.text = null
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
    val col = e.column
    val targetChanged = col == targetColumnIndex || col == TableModelEvent.ALL_COLUMNS
    if (targetChanged && e.firstRow != TableModelEvent.HEADER_ROW) {
      updateHeaderState()
    }
  }

  fun updateHeaderState() {
    val vci = table.convertColumnIndexToView(targetColumnIndex)
    if (vci >= 0) {
      setHeaderStatus(vci, resolveHeaderState(table.model))
    }
  }

  private fun setHeaderStatus(vci: Int, status: Status) {
    val column = table.columnModel.getColumn(vci)
    if (column.headerValue !== status) {
      column.headerValue = status
      val h = table.tableHeader
      h.repaint(h.getHeaderRect(vci))
    }
  }

  private fun resolveHeaderState(model: TableModel): Status {
    val values = (0..<model.rowCount)
      .asSequence()
      .map { model.getValueAt(it, targetColumnIndex) == true }
      .distinct()
      .take(2)
      .toList()
    return when {
      values.isEmpty() -> Status.DESELECTED
      values.size == 1 -> if (values[0]) Status.SELECTED else Status.DESELECTED
      else -> Status.INDETERMINATE
    }
  }

  override fun mouseClicked(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val model = table.model
    val vci = header.columnAtPoint(e.point)
    val mci = table.convertColumnIndexToModel(vci)
    if (header.isEnabled && mci == targetColumnIndex && model.rowCount > 0) {
      val column = table.columnModel.getColumn(vci)
      val selected = column.headerValue === Status.DESELECTED
      setAllValues(model, selected)
      setHeaderStatus(vci, if (selected) Status.SELECTED else Status.DESELECTED)
    }
  }

  private fun setAllValues(model: TableModel, selected: Boolean) {
    // Suppress the header state check for each row while updating all rows
    model.removeTableModelListener(this)
    try {
      for (i in 0..<model.rowCount) {
        model.setValueAt(selected, i, targetColumnIndex)
      }
    } finally {
      model.addTableModelListener(this)
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

private enum class Status(
  private val selected: Boolean,
  private val enabled: Boolean,
) {
  SELECTED(true, true),
  DESELECTED(false, true),
  INDETERMINATE(true, false),
  ;

  fun configureHeaderCheckBox(check: JCheckBox) {
    check.isSelected = selected
    check.isEnabled = enabled
  }
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
