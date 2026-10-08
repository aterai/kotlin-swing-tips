package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Path2D
import java.awt.geom.PathIterator
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.plaf.LayerUI
import javax.swing.plaf.UIResource
import javax.swing.plaf.synth.SynthTableUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun createUI(): Component {
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  val scroll = createScrollPane(TranslucentCellSelectionTable(createModel()))
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(scroll, TranslucentCellSelectionLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createScrollPane(view: Component): JScrollPane {
  val scroll = JScrollPane(view)
  scroll.background = Color.WHITE
  scroll.viewport.setOpaque(false)
  scroll.viewportBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2)
  return scroll
}

fun createModel(): TableModel {
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
  init {
    // The selection outline is painted over the whole JLayer, so repaint the
    // entire table when editing starts, stops, or is canceled to hide/show it.
    addPropertyChangeListener("tableCellEditor") { repaint() }
  }

  override fun updateUI() {
    super.updateUI()
    setCellSelectionEnabled(true)
    setShowGrid(false)
    intercellSpacing = Dimension(3, 3)
    autoCreateRowSorter = true
    background = TRANSPARENT
    setRowHeight(20)
    if (getUI() is SynthTableUI) {
      setDefaultRenderer(
        Boolean::class.javaObjectType,
        SynthBooleanTableCellRenderer(),
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
    c.background = TRANSPARENT
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

  // JTable repaints only the changed rows or columns, which would leave a part
  // of the old selection outline, so repaint the entire table on any selection
  // change (mouse, keyboard, selectAll(), clearSelection(), etc.).
  override fun valueChanged(e: ListSelectionEvent?) {
    super.valueChanged(e)
    repaint()
  }

  override fun columnSelectionChanged(e: ListSelectionEvent?) {
    super.columnSelectionChanged(e)
    repaint()
  }

  companion object {
    private val TRANSPARENT = Color(0x0, true)
  }
}

private class TranslucentCellSelectionLayerUI : LayerUI<JScrollPane>() {
  override fun paint(g: Graphics, c: JComponent?) {
    super.paint(g, c)
    val scroll = getScrollPane(c) ?: return
    val table = getTable(scroll) ?: return
    if (hasSelectedCells(table) && !table.isEditing) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      // Clip to the viewport (including its border) so that the selection
      // scrolled out of view is not painted over the header or scrollbars.
      g2.clip(SwingUtilities.convertRectangle(scroll, scroll.viewportBorderBounds, c))
      val area = Area()
      for (row in table.selectedRows) {
        for (col in table.selectedColumns) {
          addArea(c, table, area, row, col)
        }
      }
      val ics = table.intercellSpacing
      val selectionColor = table.selectionBackground
      val translucentColor = Color(
        selectionColor.red,
        selectionColor.green,
        selectionColor.blue,
        0x32,
      )
      g2.stroke = BORDER_STROKE
      for (a in splitIntoSingleLoopAreas(area)) {
        val r = a.bounds
        r.width -= ics.width - 1
        r.height -= ics.height - 1
        g2.paint = translucentColor
        g2.fill(r)
        g2.paint = selectionColor
        g2.draw(r)
      }
      g2.dispose()
    }
  }

  companion object {
    private val BORDER_STROKE = BasicStroke(2f)

    private fun hasSelectedCells(table: JTable) =
      table.selectedRowCount > 0 && table.selectedColumnCount > 0

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

    private fun getScrollPane(c: Component?) = (c as? JLayer<*>)?.view as? JScrollPane

    private fun getTable(scroll: JScrollPane) = scroll.viewport.view as? JTable

    fun splitIntoSingleLoopAreas(area: Area): List<Area> {
      val subArea = mutableListOf<Area>()
      val path = Path2D.Double()
      val pi = area.getPathIterator(null)
      val coords = DoubleArray(6)
      while (!pi.isDone) {
        val pathSegmentType = pi.currentSegment(coords)
        when (pathSegmentType) {
          PathIterator.SEG_MOVETO -> path.moveTo(
            coords[0],
            coords[1],
          )

          PathIterator.SEG_LINETO -> path.lineTo(
            coords[0],
            coords[1],
          )

          PathIterator.SEG_QUADTO -> path.quadTo(
            coords[0],
            coords[1],
            coords[2],
            coords[3],
          )

          PathIterator.SEG_CUBICTO -> path.curveTo(
            coords[0],
            coords[1],
            coords[2],
            coords[3],
            coords[4],
            coords[5],
          )

          PathIterator.SEG_CLOSE -> path.also {
            it.closePath()
            subArea.add(Area(it))
            it.reset()
          }
        }
        pi.next()
      }
      return subArea
    }
  }
}

private class SynthBooleanTableCellRenderer :
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
