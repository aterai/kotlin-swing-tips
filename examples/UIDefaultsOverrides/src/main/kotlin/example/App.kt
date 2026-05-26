package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.plaf.nimbus.AbstractRegionPainter
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val model = createModel()
  val table1 = JTable(model)
  table1.autoCreateRowSorter = true
  table1.setDefaultRenderer(String::class.java, MultiLineTableCellRenderer())

  val table2 = JTable(model)
  table2.autoCreateRowSorter = true

  val d = UIDefaults()
  d["TextArea.borderPainter"] = Painter { _, _: Component, _, _ -> }

  val r = MultiLineTableCellRenderer()
  r.putClientProperty("Nimbus.Overrides", d)
  r.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  table2.setDefaultRenderer(String::class.java, r)

  return JPanel(GridLayout(2, 0)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = createMenuBar() }
    it.add(JScrollPane(table1))
    it.add(JScrollPane(table2))
    it.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createCheckBoxMenuItem(
  title: String,
  d: UIDefaults,
): JMenuItem {
  val item = JCheckBoxMenuItem(title)
  item.putClientProperty("Nimbus.Overrides", d)
  item.putClientProperty("Nimbus.Overrides.InheritDefaults", false)
  return item
}

private fun createMenuBar(): JMenuBar {
  val d = UIDefaults()
  d["CheckBoxMenuItem[Enabled].checkIconPainter"] =
    MyCheckBoxMenuItemPainter(CheckIcon.ENABLED)
  d["CheckBoxMenuItem[MouseOver].checkIconPainter"] =
    MyCheckBoxMenuItemPainter(CheckIcon.HOVER)
  d["CheckBoxMenuItem[Enabled+Selected].checkIconPainter"] =
    MyCheckBoxMenuItemPainter(CheckIcon.ENABLED_SELECTED)
  d["CheckBoxMenuItem[MouseOver+Selected].checkIconPainter"] =
    MyCheckBoxMenuItemPainter(CheckIcon.SELECTED_HOVER)

  val menuBar = JMenuBar()
  val menu = JMenu("Menu")
  menuBar.add(menu)
  menu.add(JCheckBoxMenuItem("Default"))
  menu.add(createCheckBoxMenuItem("Test1", d))
  menu.add(createCheckBoxMenuItem("Test2", d))
  menu.add(createCheckBoxMenuItem("Test3", d))

  val cmi1 = createCheckBoxMenuItem("Test4", d)
  cmi1.isSelected = true
  cmi1.isEnabled = false
  menu.add(cmi1)

  val cmi2 = createCheckBoxMenuItem("Test5", d)
  cmi2.isSelected = false
  cmi2.isEnabled = false
  menu.add(cmi2)
  menuBar.add(menu)
  return menuBar
}

private fun createModel(): TableModel {
  val columnNames = arrayOf("A", "B", "C")
  val data = arrayOf<Array<Any>>(
    arrayOf("A0, Line1\nA0, Line2\nA0, Line3", "B0, Line1\nB0, Line2", "C0, Line1"),
    arrayOf("A1, Line1", "B1, Line1\nB1, Line2", "C1, Line1"),
    arrayOf("A2, Line1", "B2, Line1", "C2, Line1"),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(
      row: Int,
      column: Int,
    ) = false
  }
}

private enum class CheckIcon {
  ENABLED {
    override fun paint(g: Graphics2D) {
      g.paint = Color.GREEN
      g.drawOval(0, 0, 10, 10)
    }
  },
  HOVER {
    override fun paint(g: Graphics2D) {
      g.paint = Color.PINK
      g.drawOval(0, 0, 10, 10)
    }
  },
  ENABLED_SELECTED {
    override fun paint(g: Graphics2D) {
      g.paint = Color.ORANGE
      g.fillOval(0, 0, 10, 10)
    }
  },
  SELECTED_HOVER {
    override fun paint(g: Graphics2D) {
      g.paint = Color.CYAN
      g.fillOval(0, 0, 10, 10)
    }
  }, ;

  abstract fun paint(g: Graphics2D)
}

private class MyCheckBoxMenuItemPainter(
  private val state: CheckIcon,
) : AbstractRegionPainter() {
  private val ctx: PaintContext

  init {
    val ins = Insets(5, 5, 5, 5)
    val dim = Dimension(9, 10)
    this.ctx = PaintContext(ins, dim, false, null, 1.0, 1.0)
  }

  override fun doPaint(
    g: Graphics2D,
    c: JComponent,
    width: Int,
    height: Int,
    keys: Array<Any>,
  ) {
    state.paint(g)
  }

  override fun getPaintContext() = ctx
}

private class MultiLineTableCellRenderer :
  JTextArea(),
  TableCellRenderer {
  private val rowColHeight = mutableListOf<MutableList<Int>>()
  private var fhb: Border? = null
  private var epb: Border? = null

  override fun updateUI() {
    border = null
    super.updateUI()
    lineWrap = true
    wrapStyleWord = true
    isOpaque = true

    val outside = BorderFactory.createLineBorder(Color(0x73_A4_D1))
    val inside = BorderFactory.createEmptyBorder(1, 4, 1, 4)
    fhb = BorderFactory.createCompoundBorder(outside, inside)
    epb = BorderFactory.createEmptyBorder(2, 5, 2, 5)
    border = epb
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
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

  private fun getAdjustedRowHeight(
    row: Int,
    column: Int,
  ): Int {
    val prefH = preferredSize.height
    while (rowColHeight.size <= row) {
      rowColHeight.add(mutableListOf())
    }
    val colHeights = rowColHeight[row]
    while (colHeights.size <= column) {
      colHeights.add(0)
    }
    colHeights[column] = prefH
    return colHeights.max()
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
