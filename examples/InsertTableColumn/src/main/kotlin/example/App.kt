package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.event.TableColumnModelEvent
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableColumn
import javax.swing.table.TableColumnModel

fun createUI(): Component {
  val scroll = JScrollPane(makeTable())
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(scroll, ColumnInsertLayerUI()))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(): JTable {
  val table = object : JTable(5, 3) {
    override fun updateUI() {
      super.updateUI()
      setAutoCreateColumnsFromModel(false)
      setAutoResizeMode(AUTO_RESIZE_OFF)
    }

    override fun columnAdded(e: TableColumnModelEvent) {
      super.columnAdded(e)
      updateHeaderValues(getColumnModel())
    }

    override fun columnMoved(e: TableColumnModelEvent) {
      super.columnMoved(e)
      if (e.fromIndex != e.toIndex) {
        updateHeaderValues(getColumnModel())
      }
    }
  }
  // println(toColumnTitle(16_384)) // -> XFD
  table.model = DefaultTableModel(5, 16_384)
  table.setValueAt("0-0", 0, 0)
  table.setValueAt("0-1", 0, 1)
  table.setValueAt("0-2", 0, 2)
  return table
}

// Name the columns in view order (A, B, ..., Z, AA, ...)
private fun updateHeaderValues(columnModel: TableColumnModel) {
  for (i in 0..<columnModel.columnCount) {
    columnModel.getColumn(i).headerValue = toColumnTitle(i + 1)
  }
}

private const val RADIX = 26

// Bijective base-26: 1 -> A, 26 -> Z, 27 -> AA, 16384 -> XFD
private fun toColumnTitle(columnNumber: Int): String {
  require(columnNumber > 0) { "columnNumber must be positive: $columnNumber" }
  val sb = StringBuilder()
  var n = columnNumber
  while (n > 0) {
    sb.append('A' + (n - 1) % RADIX)
    n = (n - 1) / RADIX
  }
  return sb.reverse().toString()
}

private class ColumnInsertLayerUI : LayerUI<JScrollPane>() {
  private val line = Rectangle2D.Double()
  private val plus = Ellipse2D.Double()
  private var insertIndex = -1

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val scroll = (c as? JLayer<*>)?.view as? JScrollPane
    if (insertIndex >= 0 && scroll != null) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      // Do not paint over the scroll bars
      val clip = scroll.viewport.bounds
      scroll.columnHeader?.also { clip.add(it.bounds) }
      g2.clip(SwingUtilities.convertRectangle(scroll, clip, c))
      // line and plus are in the JTableHeader coordinate system
      val header = getTable(scroll).tableHeader
      val pt = SwingUtilities.convertPoint(header, 0, 0, c)
      g2.translate(pt.x, pt.y)
      // paint Insert Line
      g2.paint = LINE_COLOR
      g2.fill(line)
      // paint Plus Icon
      g2.paint = Color.WHITE
      g2.fill(plus)
      g2.paint = LINE_COLOR
      val cx = plus.centerX
      val cy = plus.centerY
      val r = plus.width / 2.0
      g2.draw(Line2D.Double(cx - r, cy, cx + r, cy))
      g2.draw(Line2D.Double(cx, cy - r, cx, cy + r))
      g2.draw(plus)
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask =
        AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    if (c is JLayer<*>) {
      c.layerEventMask = 0
    }
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseEvent(e, l)
    val header = e.component as? JTableHeader ?: return
    when (e.id) {
      MouseEvent.MOUSE_CLICKED -> {
        val pt = e.point
        if (insertIndex >= 0 && plus.contains(pt)) {
          insertColumn(header.table, insertIndex)
          updateInsertLocation(l.view, header, pt)
          l.repaint()
        }
      }

      MouseEvent.MOUSE_EXITED -> clearInsertLocation(l)
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseMotionEvent(e, l)
    val c = e.component
    if (e.id == MouseEvent.MOUSE_MOVED && c is JTableHeader) {
      updateInsertLocation(l.view, c, e.point)
      l.repaint()
    } else {
      clearInsertLocation(l)
    }
  }

  private fun clearInsertLocation(l: JLayer<out JScrollPane>) {
    if (insertIndex >= 0) {
      insertIndex = -1
      l.repaint()
    }
  }

  private fun updateInsertLocation(
    scroll: JScrollPane,
    header: JTableHeader,
    pt: Point,
  ) {
    insertIndex = getInsertIndex(header, pt)
    if (insertIndex >= 0) {
      val x = getBoundaryX(header, insertIndex)
      val height = header.height + scroll.viewport.height
      val lx = maxOf(0, x - LINE_WIDTH / 2).toDouble()
      line.setFrame(lx, 0.0, LINE_WIDTH.toDouble(), height.toDouble())
      val cx = maxOf(x.toDouble(), PLUS_SIZE / 2.0)
      val cy = header.height / 2.0
      val s = PLUS_SIZE.toDouble()
      plus.setFrame(cx - s / 2.0, cy - s / 2.0, s, s)
    }
  }

  companion object {
    private val LINE_COLOR = Color(0x00_78_D7)
    private const val LINE_WIDTH = 4
    private const val PLUS_SIZE = 10

    // Returns the view index at which a new column is inserted, or -1 if the
    // point is not near a column boundary
    private fun getInsertIndex(header: JTableHeader, pt: Point): Int {
      val column = header.columnAtPoint(pt)
      var index = -1
      if (column >= 0) {
        val r = header.getHeaderRect(column)
        // The left edge of the first column has no column on its left side,
        // so the whole hit area is placed inside the first column
        val west = if (column == 0) PLUS_SIZE else PLUS_SIZE / 2
        if (pt.x < r.x + west) {
          index = column
        } else if (pt.x >= r.x + r.width - PLUS_SIZE / 2) {
          index = column + 1
        }
      }
      return index
    }

    private fun getBoundaryX(header: JTableHeader, index: Int) = if (index == 0) {
      header.getHeaderRect(0).x
    } else {
      val r = header.getHeaderRect(index - 1)
      r.x + r.width
    }

    // JTable and TableColumnModel have no method to insert a TableColumn at
    // the specified position, so add it to the end and then move it
    private fun insertColumn(table: JTable, index: Int) {
      val viewCount = table.columnCount
      if (viewCount < table.model.columnCount) {
        table.addColumn(TableColumn(viewCount))
        table.moveColumn(viewCount, index)
      }
    }

    private fun getTable(scroll: JScrollPane) = scroll.viewport.view as JTable
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
