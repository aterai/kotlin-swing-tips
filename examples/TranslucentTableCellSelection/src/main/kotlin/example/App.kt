package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import javax.swing.*
import javax.swing.event.ChangeEvent
import javax.swing.plaf.LayerUI
import javax.swing.plaf.UIResource
import javax.swing.plaf.synth.SynthTableUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  val scroll = makeScrollPane(TranslucentCellSelectionTable(makeModel()))
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(scroll, TranslucentCellSelectionLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeScrollPane(view: Component): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.background = Color.WHITE
  scroll.viewport.setOpaque(false)
  scroll.viewportBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2)
  return scroll
}

fun makeModel(): TableModel {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
    arrayOf("eee", 32, true),
    arrayOf("fff", 8, false),
    arrayOf("ggg", 64, true),
    arrayOf("hhh", 1, false),
  )
  return object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
}

private class TranslucentCellSelectionTable(
  model: TableModel,
) : JTable(model) {
  override fun updateUI() {
    super.updateUI()
    setCellSelectionEnabled(true)
    setShowGrid(false)
    intercellSpacing = Dimension(3, 3)
    autoCreateRowSorter = true
    background = Color(0x0, true)
    setRowHeight(20)
    if (getUI() is SynthTableUI) {
      setDefaultRenderer(
        Boolean::class.javaObjectType,
        SynthBooleanTableCellRenderer2(),
      )
    }
  }

  override fun prepareRenderer(
    renderer: TableCellRenderer,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareRenderer(renderer, row, column)
    if (c is JComponent) {
      c.isOpaque = false
    }
    c.foreground = foreground
    c.background = Color(0x0, true)
    return c
  }

  override fun prepareEditor(
    editor: TableCellEditor,
    row: Int,
    column: Int,
  ): Component {
    val c = super.prepareEditor(editor, row, column)
    if (c is JComponent) {
      c.isOpaque = false
    }
    return c
  }

  override fun changeSelection(
    rowIndex: Int,
    columnIndex: Int,
    toggle: Boolean,
    extend: Boolean,
  ) {
    super.changeSelection(rowIndex, columnIndex, toggle, extend)
    repaint()
  }

  override fun editingStopped(e: ChangeEvent?) {
    super.editingStopped(e)
    repaint()
  }
}

private class TranslucentCellSelectionLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    val table = getTable(c) ?: return
    val cc = table.selectedColumnCount
    val rc = table.selectedRowCount
    if (cc != 0 && rc != 0 && !table.isEditing) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val area = Area()
      for (row in table.selectedRows) {
        for (col in table.selectedColumns) {
          addArea(c, table, area, row, col)
        }
      }
      val ics = table.intercellSpacing
      val v = table.selectionBackground
      val sbc = Color(v.red, v.green, v.blue, 0x32)
      for (a in singularization(area)) {
        val r = a.bounds
        r.width -= ics.width - 1
        r.height -= ics.height - 1
        g2.paint = sbc
        g2.fill(r)
        g2.paint = v
        g2.stroke = BORDER_STROKE
        g2.draw(r)
      }
      g2.dispose()
    }
  }

  companion object {
    private val BORDER_STROKE = BasicStroke(2f)

    private fun addArea(
      c: Component?,
      table: JTable,
      area: Area,
      row: Int,
      col: Int,
    ) {
      if (table.isCellSelected(row, col)) {
        val r = table.getCellRect(row, col, true)
        area.add(Area(SwingUtilities.convertRectangle(table, r, c)))
      }
    }

    private fun getTable(c: Component?): JTable? {
      var table: JTable? = null
      if (c is JLayer<*>) {
        val c1: Component? = c.getView()
        if (c1 is JScrollPane) {
          table = c1.getViewport().view as? JTable
        }
      }
      return table
    }

    fun singularization(rect: Area): List<Area> {
      val list = ArrayList<Area>()
      val path = Path2D.Double()
      val pi = rect.getPathIterator(null)
      val c = DoubleArray(6)
      while (!pi.isDone) {
        when (pi.currentSegment(c)) {
          PathIterator.SEG_MOVETO -> path.moveTo(c[0], c[1])
          PathIterator.SEG_LINETO -> path.lineTo(c[0], c[1])
          PathIterator.SEG_QUADTO -> path.quadTo(c[0], c[1], c[2], c[3])
          PathIterator.SEG_CUBICTO -> path.curveTo(c[0], c[1], c[2], c[3], c[4], c[5])
          PathIterator.SEG_CLOSE -> listAdd(path, list)
          // else -> {}
        }
        pi.next()
      }
      return list
    }

    private fun listAdd(path: Path2D.Double, list: ArrayList<Area>) {
      path.closePath()
      list.add(Area(path))
      path.reset()
    }
  }
}

private class SynthBooleanTableCellRenderer2 :
  JCheckBox(),
  TableCellRenderer {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    horizontalAlignment = CENTER
    name = "Table.cellRenderer"
    if (isSelected) {
      foreground = unwrap(table.selectionForeground)
      background = unwrap(table.selectionBackground)
    } else {
      foreground = unwrap(table.foreground)
      background = unwrap(table.background)
    }
    setSelected(value as? Boolean == true)
    return this
  }

  override fun isOpaque() = false

  private fun unwrap(c: Color) = if (c is UIResource) Color(c.rgb) else c
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
