package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

class MainPanel : JPanel(BorderLayout()) {
  init {
    val table0 = makeTable()
    (table0.tableHeader.defaultRenderer as JLabel).horizontalAlignment = SwingConstants.CENTER
    val table1 = makeTable()
    table1.tableHeader.defaultRenderer = object : DefaultTableCellRenderer() {
      override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
      ): Component {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        horizontalAlignment = SwingConstants.CENTER
        return this
      }
    }

    val table2 = makeTable()
    val cm = table2.columnModel
    cm.getColumn(0).headerRenderer = HorizontalAlignmentHeaderRenderer(SwingConstants.LEFT)
    cm.getColumn(1).headerRenderer = HorizontalAlignmentHeaderRenderer(SwingConstants.CENTER)
    cm.getColumn(2).headerRenderer = HorizontalAlignmentHeaderRenderer(SwingConstants.RIGHT)

    val tabs = JTabbedPane()
    tabs.addTab("Default", JScrollPane(makeTable()))
    tabs.addTab("Test0", JScrollPane(table0))
    tabs.addTab("Test1", JScrollPane(table1))
    tabs.addTab("Test2", JScrollPane(table2))
    add(tabs)
    preferredSize = Dimension(320, 240)
  }

  private fun makeTable(): JTable {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
      arrayOf("aaa", 12, true),
      arrayOf("bbb", 5, false),
      arrayOf("CCC", 92, true),
      arrayOf("DDD", 0, false)
    )
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }
    val table = JTable(model)
    table.autoCreateRowSorter = true
    return table
  }
}

class HorizontalAlignmentHeaderRenderer(private val horizontalAlignment: Int) : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    (c as? JLabel)?.horizontalAlignment = horizontalAlignment
    return c
  }
}

object LookAndFeelUtil {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.getName()
  fun createLookAndFeelMenu() = JMenu("LookAndFeel").also {
    val lafRadioGroup = ButtonGroup()
    for (lafInfo in UIManager.getInstalledLookAndFeels()) {
      it.add(createLookAndFeelItem(lafInfo.getName(), lafInfo.getClassName(), lafRadioGroup))
    }
  }

  private fun createLookAndFeelItem(lafName: String, lafClassName: String, lafRadioGroup: ButtonGroup): JMenuItem {
    val lafItem = JRadioButtonMenuItem(lafName, lafClassName == lookAndFeel)
    lafItem.setActionCommand(lafClassName)
    lafItem.setHideActionText(true)
    lafItem.addActionListener {
      val m = lafRadioGroup.getSelection()
      runCatching {
        setLookAndFeel(m.getActionCommand())
      }.onFailure {
        it.printStackTrace()
        Toolkit.getDefaultToolkit().beep()
      }
    }
    lafRadioGroup.add(lafItem)
    return lafItem
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class
  )
  private fun setLookAndFeel(lookAndFeel: String) {
    val oldLookAndFeel = LookAndFeelUtil.lookAndFeel
    if (oldLookAndFeel != lookAndFeel) {
      UIManager.setLookAndFeel(lookAndFeel)
      LookAndFeelUtil.lookAndFeel = lookAndFeel
      updateLookAndFeel()
      // firePropertyChange("lookAndFeel", oldLookAndFeel, lookAndFeel)
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
  }
} /* Singleton */

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      val mb = JMenuBar()
      mb.add(LookAndFeelUtil.createLookAndFeelMenu())
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      setJMenuBar(mb)
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
