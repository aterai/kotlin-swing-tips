package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val table1 = JTable(createModel())
  table1.autoCreateRowSorter = true
  val table2 = ButtonHeaderTable(createModel())
  val menuBar = JMenuBar()
  menuBar.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(2, 1)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = menuBar }
    it.add(JScrollPane(table1))
    it.add(JScrollPane(table2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private class ButtonHeaderTable(
  model: TableModel,
) : JTable(model) {
  private var listener: MouseAdapter? = null

  override fun updateUI() {
    getTableHeader().removeMouseListener(listener)
    getTableHeader().removeMouseMotionListener(listener)
    super.updateUI()
    setAutoCreateRowSorter(true)
    val header = getTableHeader()
    header.setCursor(Cursor.getDefaultCursor())
    header.defaultRenderer = ButtonHeaderRenderer()
    listener = HeaderMouseListener()
    header.addMouseListener(listener)
    header.addMouseMotionListener(listener)
  }
}

private class ButtonHeaderRenderer :
  JButton(),
  TableCellRenderer {
  private var pressedColumn = -1
  private var rolloverColumn = -1

  override fun updateUI() {
    super.updateUI()
    horizontalTextPosition = LEFT
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    text = value?.toString() ?: ""
    val modelColumn = table.convertColumnIndexToModel(column)
    val header = table.tableHeader
    if (header != null) {
      font = header.font
      getModel().also {
        val isPressed = modelColumn == pressedColumn
        it.isPressed = isPressed
        it.isArmed = isPressed
        it.isRollover = modelColumn == rolloverColumn
        it.isSelected = isSelected
      }
    }
    icon = table.rowSorter
      ?.sortKeys
      ?.firstOrNull()
      ?.takeIf { it.column == modelColumn }
      ?.let { SortIconType.getIcon(it.sortOrder) }
    return this
  }

  fun setPressedColumn(column: Int) {
    pressedColumn = column
  }

  fun getRolloverColumn() = rolloverColumn

  fun setRolloverColumn(column: Int) {
    rolloverColumn = column
  }
}

private enum class SortIconType(
  private val sortOrder: SortOrder?,
  private val uiKey: String,
) {
  ASCENDING(SortOrder.ASCENDING, "Table.ascendingSortIcon"),
  DESCENDING(SortOrder.DESCENDING, "Table.descendingSortIcon"),
  UNSORTED(null, "Table.naturalSortIcon"),
  ;

  val icon: Icon?
    get() = UIManager.getIcon(uiKey)

  companion object {
    fun getIcon(order: SortOrder?): Icon? {
      val iconType = entries
        .firstOrNull { it.sortOrder == order }
        ?: UNSORTED
      return iconType.icon
    }
  }
}

private class HeaderMouseListener : MouseAdapter() {
  override fun mousePressed(e: MouseEvent) {
    getButtonHeaderRenderer(e)?.also {
      it.setPressedColumn(getModelColumn(e))
      e.component.repaint()
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    getButtonHeaderRenderer(e)?.also {
      it.setPressedColumn(-1)
      e.component.repaint()
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    getButtonHeaderRenderer(e)?.also {
      val column = getModelColumn(e)
      // Repaint only when the rollover column changes, because
      // BasicTableHeaderUI (e.g. MetalLookAndFeel) does not repaint it.
      if (it.getRolloverColumn() != column) {
        it.setRolloverColumn(column)
        e.component.repaint()
      }
    }
  }

  override fun mouseExited(e: MouseEvent) {
    getButtonHeaderRenderer(e)?.also {
      it.setRolloverColumn(-1)
      e.component.cursor = Cursor.getDefaultCursor()
      e.component.repaint()
    }
  }

  private fun getButtonHeaderRenderer(e: MouseEvent) =
    (e.component as? JTableHeader)?.defaultRenderer as? ButtonHeaderRenderer

  // Returns -1 if the point is outside the columns (e.g. the empty area on the right).
  private fun getModelColumn(e: MouseEvent): Int {
    val header = e.component as? JTableHeader ?: return -1
    val viewColumn = header.columnAtPoint(e.point)
    return if (viewColumn < 0) -1 else header.table.convertColumnIndexToModel(viewColumn)
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
