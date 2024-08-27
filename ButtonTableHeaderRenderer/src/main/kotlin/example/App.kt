package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = object : JTable(makeModel()) {
    private var listener: MouseAdapter? = null

    override fun updateUI() {
      getTableHeader().removeMouseListener(listener)
      getTableHeader().removeMouseMotionListener(listener)
      super.updateUI()
      cursor = Cursor.getDefaultCursor()
      autoCreateRowSorter = true
      val header = getTableHeader()
      header.defaultRenderer = ButtonHeaderRenderer()
      listener = HeaderMouseListener()
      header.addMouseListener(listener)
      header.addMouseMotionListener(listener)
    }
  }
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(2, 1)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(JTable(makeModel()).also { t -> t.autoCreateRowSorter = true }))
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
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
    var sortIcon: Icon? = null
    if (table.rowSorter != null) {
      val sortKeys = table.rowSorter.sortKeys
      if (sortKeys.isNotEmpty() && sortKeys[0].column == modelColumn) {
        sortIcon = when (sortKeys[0].sortOrder) {
          SortOrder.ASCENDING -> UIManager.getIcon("Table.ascendingSortIcon")
          SortOrder.DESCENDING -> UIManager.getIcon("Table.descendingSortIcon")
          SortOrder.UNSORTED, null -> UIManager.getIcon("Table.naturalSortIcon")
        }
      }
    }
    icon = sortIcon
    return this
  }

  fun setPressedColumn(column: Int) {
    pressedColumn = column
  }

  fun setRolloverColumn(column: Int) {
    rolloverColumn = column
  }
}

private class HeaderMouseListener : MouseAdapter() {
  override fun mousePressed(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val table = header.table
    val renderer = header.defaultRenderer
    val viewColumn = table.columnAtPoint(e.point)
    if (viewColumn >= 0 && renderer is ButtonHeaderRenderer) {
      val column = table.convertColumnIndexToModel(viewColumn)
      renderer.setPressedColumn(column)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val renderer = header.defaultRenderer
    if (renderer is ButtonHeaderRenderer) {
      renderer.setPressedColumn(-1)
    }
  }

  override fun mouseMoved(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val table = header.table
    val renderer = header.defaultRenderer
    val viewColumn = table.columnAtPoint(e.point)
    if (viewColumn >= 0 && renderer is ButtonHeaderRenderer) {
      val column = table.convertColumnIndexToModel(viewColumn)
      renderer.setRolloverColumn(column)
    }
  }

  override fun mouseExited(e: MouseEvent) {
    val header = e.component as? JTableHeader ?: return
    val renderer = header.defaultRenderer
    if (renderer is ButtonHeaderRenderer) {
      renderer.setRolloverColumn(-1)
      header.cursor = Cursor.getDefaultCursor()
    }
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
