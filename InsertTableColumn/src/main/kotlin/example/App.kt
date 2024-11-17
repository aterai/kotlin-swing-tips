package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

fun makeUI(): Component {
  val scroll = JScrollPane(makeTable())
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(scroll, ColumnInsertLayerUI()))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(): JTable {
  val table = object : JTable(5, 3) {
    override fun createDefaultTableHeader() = object : JTableHeader(columnModel) {
      override fun updateUI() {
        super.updateUI()
        EventQueue.invokeLater {
          val renderer = defaultRenderer
          defaultRenderer =
            TableCellRenderer { table, value, isSelected, hasFocus, row, column ->
              renderer
                .getTableCellRendererComponent(
                  table,
                  value,
                  isSelected,
                  hasFocus,
                  row,
                  column,
                ).also {
                  if (it is JLabel) {
                    it.text = convertToColumnTitle(column + 1)
                    it.horizontalAlignment = SwingConstants.CENTER
                  }
                }
            }
        }
      }
    }

    override fun updateUI() {
      super.updateUI()
      setAutoCreateColumnsFromModel(false)
      setAutoResizeMode(AUTO_RESIZE_OFF)
    }
  }
  table.model = DefaultTableModel(5, 16_384)
  table.setValueAt("0-0", 0, 0)
  table.setValueAt("0-1", 0, 1)
  table.setValueAt("0-2", 0, 2)
  return table
}

private fun convertToColumnTitle(columnNumber: Int): String {
  assert(columnNumber > 0) { "Input is not valid!" }
  val sb = StringBuilder()
  var num = columnNumber
  while (num > 0) {
    val mod = (num - 1) % 26
    val code = 'A'.code + mod
    sb.insert(0, code.toChar())
    num = (num - mod) / 26
  }
  return sb.toString()
}

private class ColumnInsertLayerUI : LayerUI<JScrollPane>() {
  private val line = Rectangle2D.Double()
  private val plus = Ellipse2D.Double(0.0, 0.0, 10.0, 10.0)

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val g2 = g.create()
    if (c is JLayer<*> && !line.isEmpty && g2 is Graphics2D) {
      val scroll = c.view as? JScrollPane
      val header = (scroll?.viewport?.view as? JTable)?.tableHeader ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      val pt0 = line.bounds.location
      val pt1 = SwingUtilities.convertPoint(header, pt0, c)
      g2.translate(pt1.getX() - pt0.getX(), pt1.getY() - pt0.getY())
      g2.paint = LINE_COLOR
      g2.fill(line)
      g2.paint = Color.WHITE
      g2.fill(plus)
      g2.paint = LINE_COLOR
      val cx = plus.centerX
      val cy = plus.centerY
      val w2 = plus.width / 2.0
      val h2 = plus.height / 2.0
      g2.draw(Line2D.Double(cx - w2, cy, cx + w2, cy))
      g2.draw(Line2D.Double(cx, cy - h2, cx, cy + h2))
      g2.draw(plus)
    }
    g2.dispose()
  }

  private fun updateLineLocation(scroll: JScrollPane, loc: Point) {
    val table = scroll.viewport.view as? JTable ?: return
    val header = table.tableHeader
    val size = table.columnCount
    val d = Dimension(LINE_WIDTH, scroll.visibleRect.height)
    for (i in 0..<size) {
      val r = header.getHeaderRect(i)
      val r1 = getWestRect(r, i)
      val r2 = getEastRect(r)
      val b = when {
        r1.contains(loc) -> {
          updateInsertLineLocation(r1, loc, d, header)
          true
        }

        r2.contains(loc) -> {
          updateInsertLineLocation(r2, loc, d, header)
          true
        }

        r.contains(loc) -> {
          line.setFrame(0.0, 0.0, 0.0, 0.0)
          header.cursor = Cursor.getDefaultCursor()
          true
        }

        else -> false
      }
      if (b) {
        return
      }
    }
  }

  private fun getWestRect(r: Rectangle, i: Int): Rectangle {
    val rect = r.bounds
    val bounds = plus.bounds
    if (i != 0) {
      rect.x -= bounds.width / 2
    }
    rect.size = bounds.size
    return rect
  }

  private fun getEastRect(r: Rectangle): Rectangle {
    val rect = r.bounds
    val bounds = plus.bounds
    rect.x += rect.width - bounds.width / 2
    rect.size = bounds.size
    return rect
  }

  private fun updateInsertLineLocation(
    r: Rectangle,
    loc: Point,
    d: Dimension,
    c: Component,
  ) {
    if (r.contains(loc)) {
      val cx = r.centerX
      val cy = r.centerY
      line.setFrame(cx - d.getWidth() / 2.0, r.getY(), d.getWidth(), d.getHeight())
      val pw = plus.width / 2.0
      val ph = plus.height / 2.0
      plus.setFrameFromCenter(cx, cy, cx - pw, cy - ph)
      c.cursor = Cursor.getDefaultCursor()
    } else {
      line.setFrame(0.0, 0.0, 0.0, 0.0)
      c.cursor = Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
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
    when (e.id) {
      MouseEvent.MOUSE_CLICKED -> {
        val scroll = l.view
        if (plus.contains(e.getPoint()) && !line.isEmpty) {
          val table = scroll.viewport.view as? JTable ?: return
          val model = table.model
          val columnCount = table.columnCount
          val maxColumn = model.columnCount
          if (columnCount < maxColumn) {
            val idx = table.columnAtPoint(line.bounds.location)
            val column = TableColumn(columnCount)
            column.headerValue = "Column$columnCount"
            table.addColumn(column)
            table.moveColumn(columnCount, idx + 1)
          }
        }
        l.repaint(scroll.bounds)
      }

      MouseEvent.MOUSE_RELEASED -> {
        l.repaint()
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseMotionEvent(e, l)
    val view = l.view
    if (e.id == MouseEvent.MOUSE_MOVED && e.component is JTableHeader) {
      updateLineLocation(view, e.point)
    } else {
      line.setFrame(0.0, 0.0, 0.0, 0.0)
    }
    l.repaint(view.bounds)
  }

  companion object {
    private val LINE_COLOR = Color(0x00_78_D7)
    private const val LINE_WIDTH = 4
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
