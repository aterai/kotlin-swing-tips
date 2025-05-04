package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table1 = makeTable1()
  val table2 = makeTable2()
  val tabs = JTabbedPane()
  tabs.addTab("HeaderRenderer", JScrollPane(table1))
  tabs.addTab("JLayer", JLayer(JScrollPane(table2), SortingLayerUI()))
  val btn = JButton("clear SortKeys")
  btn.addActionListener {
    listOf(table1, table2).forEach { it.rowSorter.sortKeys = null }
  }
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.setJMenuBar(mb) }
    it.add(tabs)
    it.add(btn, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf<Any>("aaa", 12, true),
    arrayOf<Any>("bbb", 5, false),
    arrayOf<Any>("CCC", 92, true),
    arrayOf<Any>("DDD", 0, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private fun makeTable1(): JTable {
  val table = object : JTable(makeModel()) {
    override fun updateUI() {
      super.updateUI()
      val cm = getColumnModel()
      val r = ColumnHeaderRenderer()
      for (i in 0..<cm.columnCount) {
        cm.getColumn(i).setHeaderRenderer(r)
      }
    }
  }
  table.setAutoCreateRowSorter(true)
  return table
}

private fun makeTable2(): JTable {
  val table = JTable(makeModel())
  table.setAutoCreateRowSorter(true)
  return table
}

private class ColumnHeaderRenderer : TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val r = table.getTableHeader().defaultRenderer
    val b = isSortingColumn(table, column) || isSelected
    val c = r.getTableCellRendererComponent(table, value, b, hasFocus, row, column)
    if (b) {
      c.setBackground(SORTING_BGC)
    }
    return c
  }

  private fun isSortingColumn(table: JTable, column: Int) = table
    .rowSorter
    ?.sortKeys
    ?.takeUnless { it.isEmpty() }
    ?.let { it[0].column }
    ?.let { column == table.convertColumnIndexToView(it) }
    ?: false

  companion object {
    private val SORTING_BGC = Color(0xA4_CF_EF)
  }
}

private class SortingLayerUI : LayerUI<JScrollPane>() {
  override fun installUI(c: JComponent?) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.setLayerEventMask(
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK,
      )
    }
  }

  override fun uninstallUI(c: JComponent?) {
    if (c is JLayer<*>) {
      c.setLayerEventMask(0)
    }
    super.uninstallUI(c)
  }

  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    if (c is JLayer<*>) {
      val r = c
        .view
        ?.takeIf { JScrollPane::class.java.isInstance(it) }
        ?.let { JScrollPane::class.java.cast(it) }
        ?.viewport
        ?.view
        ?.takeIf { JTable::class.java.isInstance(it) }
        ?.let { JTable::class.java.cast(it) }
        ?.let { getSortingColumnBounds(c, it) }
        ?: Rectangle()
      if (!r.isEmpty) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = Color(0x64_FE_AE_FF, true)
        g2.fill(r)
        g2.dispose()
      }
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane?>?) {
    super.processMouseEvent(e, l)
    val c = e.component
    if (c is JTableHeader && e.getID() == MouseEvent.MOUSE_CLICKED) {
      c.repaint()
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane?>?) {
    val c = e.component
    if (c is JTableHeader && e.getID() == MouseEvent.MOUSE_DRAGGED) {
      c.repaint()
    }
  }

  private fun getSortingColumnBounds(layer: JLayer<*>, table: JTable): Rectangle {
    val sortingColumn = table
      .rowSorter
      ?.sortKeys
      ?.takeUnless { it.isEmpty() }
      ?.let { it[0].column }
      ?.let { table.convertColumnIndexToView(it) }
      ?: -1
    return if (sortingColumn >= 0) {
      val r = getSortingRect(table, sortingColumn)
      val h = r.height / 6
      r.y += r.height - h
      r.height = h
      SwingUtilities.convertRectangle(table.getTableHeader(), r, layer)
    } else {
      Rectangle()
    }
  }

  private fun getSortingRect(table: JTable, sortingColumn: Int): Rectangle {
    val header = table.getTableHeader()
    val r = header.getHeaderRect(sortingColumn)
    val draggedColumn = header.getDraggedColumn()
    if (draggedColumn != null) {
      val modelIndex = draggedColumn.getModelIndex()
      val viewIndex = table.convertColumnIndexToView(modelIndex)
      if (viewIndex == sortingColumn) {
        r.x += header.getDraggedDistance()
      }
    }
    return r
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
