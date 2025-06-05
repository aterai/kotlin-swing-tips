package example

import java.awt.*
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = JTable(makeModel())
  table.autoCreateRowSorter = true
  val tabs = JTabbedPane()
  tabs.addTab("Default", JScrollPane(table))
  tabs.addTab("Test0", JScrollPane(makeTable0()))
  tabs.addTab("Test1", JScrollPane(makeTable1()))
  tabs.addTab("Test2", JScrollPane(makeTable2()))
  return JPanel(BorderLayout()).also {
    val mb = JMenuBar()
    mb.add(LookAndFeelUtils.createLookAndFeelMenu())
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable0() = object : JTable(makeModel()) {
  override fun updateUI() {
    super.updateUI()
    autoCreateRowSorter = true
    val r = tableHeader.defaultRenderer
    (r as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
  }
}

private fun makeTable1() = object : JTable(makeModel()) {
  override fun updateUI() {
    super.updateUI()
    autoCreateRowSorter = true
    val renderer = DefaultTableCellRenderer()
    tableHeader.setDefaultRenderer {
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
      ->
      renderer
        .getTableCellRendererComponent(
          table,
          value,
          isSelected,
          hasFocus,
          row,
          column,
        ).also {
          (it as? JLabel)?.horizontalAlignment = SwingConstants.CENTER
        }
    }
  }
}

private fun makeTable2(): JTable {
  val table = JTable(makeModel())
  table.autoCreateRowSorter = true
  listOf(SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT)
    .forEachIndexed { index, align ->
      val column = table.columnModel.getColumn(index)
      column.headerRenderer = HorizontalAlignmentHeaderRenderer(align)
    }
  return table
}

private fun makeModel(): TableModel {
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

private class HorizontalAlignmentHeaderRenderer(
  private val horAlignment: Int,
) : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    (c as? JLabel)?.horizontalAlignment = horAlignment
    return c
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
