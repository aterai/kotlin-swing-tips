package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.plaf.nimbus.AbstractRegionPainter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf(
    arrayOf("A0, Line1\nA0, Line2\nA0, Line3", "B0, Line1\nB0, Line2", "C0, Line1"),
    arrayOf("A1, Line1", "B1, Line1\nB1, Line2", "C1, Line1"),
    arrayOf("A2, Line1", "B2, Line1", "C2, Line1")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = false
  }
  val table1 = JTable(model)
  table1.autoCreateRowSorter = true
  table1.setDefaultRenderer(String::class.java, MultiLineTableCellRenderer())

  val table2 = JTable(model)
  table2.autoCreateRowSorter = true

  val d = UIDefaults()
  d["TextArea.borderPainter"] = Painter { _: Graphics2D, _: JComponent, _: Int, _: Int -> }

  val r = MultiLineTableCellRenderer()
  r.putClientProperty("Nimbus.Overrides", d)
  r.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  table2.setDefaultRenderer(String::class.java, r)

  return JPanel(GridLayout(2, 0)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(JScrollPane(table1))
    it.add(JScrollPane(table2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeJCheckBoxMenuItem(title: String, d: UIDefaults) = JCheckBoxMenuItem(title).also {
  it.putClientProperty("Nimbus.Overrides", d)
  it.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
}

private fun createMenuBar(): JMenuBar {
  val d = UIDefaults()
  d["CheckBoxMenuItem[Enabled].checkIconPainter"] = MyCheckBoxMenuItemPainter(CheckIcon.ENABLED)
  d["CheckBoxMenuItem[MouseOver].checkIconPainter"] = MyCheckBoxMenuItemPainter(CheckIcon.MOUSEOVER)
  d["CheckBoxMenuItem[Enabled+Selected].checkIconPainter"] = MyCheckBoxMenuItemPainter(CheckIcon.ENABLED_SELECTED)
  d["CheckBoxMenuItem[MouseOver+Selected].checkIconPainter"] = MyCheckBoxMenuItemPainter(CheckIcon.SELECTED_MOUSEOVER)

  val menuBar = JMenuBar()
  val menu = JMenu("Menu")
  menuBar.add(menu)
  menu.add(JCheckBoxMenuItem("Default"))
  menu.add(makeJCheckBoxMenuItem("Test1", d))
  menu.add(makeJCheckBoxMenuItem("Test2", d))
  menu.add(makeJCheckBoxMenuItem("Test3", d))

  val cmi1 = makeJCheckBoxMenuItem("Test4", d)
  cmi1.isSelected = true
  cmi1.isEnabled = false
  menu.add(cmi1)

  val cmi2 = makeJCheckBoxMenuItem("Test5", d)
  cmi2.isSelected = false
  cmi2.isEnabled = false
  menu.add(cmi2)
  menuBar.add(menu)
  return menuBar
}

private enum class CheckIcon {
  ENABLED_SELECTED, SELECTED_MOUSEOVER, ENABLED, MOUSEOVER
}

private class MyCheckBoxMenuItemPainter(private val state: CheckIcon) : AbstractRegionPainter() {
  private val ctx = PaintContext(Insets(5, 5, 5, 5), Dimension(9, 10), false, null, 1.0, 1.0)
  override fun doPaint(g: Graphics2D, c: JComponent, width: Int, height: Int, keys: Array<Any>?) {
    when (state) {
      CheckIcon.ENABLED -> paintCheckIconEnabled(g)
      CheckIcon.MOUSEOVER -> paintCheckIconMouseOver(g)
      CheckIcon.ENABLED_SELECTED -> paintCheckIconEnabledAndSelected(g)
      CheckIcon.SELECTED_MOUSEOVER -> paintCheckIconSelectedAndMouseOver(g)
    }
  }

  override fun getPaintContext() = ctx

  private fun paintCheckIconEnabled(g: Graphics2D) {
    g.paint = Color.GREEN
    g.drawOval(0, 0, 10, 10)
  }

  private fun paintCheckIconMouseOver(g: Graphics2D) {
    g.paint = Color.PINK
    g.drawOval(0, 0, 10, 10)
  }

  private fun paintCheckIconEnabledAndSelected(g: Graphics2D) {
    g.paint = Color.ORANGE
    g.fillOval(0, 0, 10, 10)
  }

  private fun paintCheckIconSelectedAndMouseOver(g: Graphics2D) {
    g.paint = Color.CYAN
    g.fillOval(0, 0, 10, 10)
  }
}

private class MultiLineTableCellRenderer : JTextArea(), TableCellRenderer {
  private val rowColHeight = mutableListOf<MutableList<Int>>()
  @Transient private var fhb: Border? = null
  @Transient private var epb: Border? = null

  override fun updateUI() {
    border = null
    super.updateUI()
    lineWrap = true
    wrapStyleWord = true
    isOpaque = true

    val b = BorderFactory.createLineBorder(Color(0x73_A4_D1))
    fhb = BorderFactory.createCompoundBorder(b, BorderFactory.createEmptyBorder(1, 4, 1, 4))
    epb = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    border = epb
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    font = table.font
    text = value?.toString() ?: ""
    border = if (hasFocus) fhb else epb
    if (isSelected) {
      foreground = table.selectionForeground
      background = table.selectionBackground
    } else {
      foreground = table.foreground
      background = table.background
    }
    bounds = table.getCellRect(row, column, false)
    val maxH = getAdjustedRowHeight(row, column)
    if (table.getRowHeight(row) != maxH) {
      table.setRowHeight(row, maxH)
    }
    return this
  }

  private fun getAdjustedRowHeight(row: Int, column: Int): Int {
    val prefH = preferredSize.height
    while (rowColHeight.size <= row) {
      rowColHeight.add(createMutableList(column))
    }
    val colHeights = rowColHeight[row]
    while (colHeights.size <= column) {
      colHeights.add(0)
    }
    colHeights[column] = prefH
    var maxH = prefH
    for (colHeight in colHeights) {
      if (colHeight > maxH) {
        maxH = colHeight
      }
    }
    return maxH
  }

  companion object {
    private fun <E> createMutableList(initialCapacity: Int) = ArrayList<E>(initialCapacity)
  }
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
