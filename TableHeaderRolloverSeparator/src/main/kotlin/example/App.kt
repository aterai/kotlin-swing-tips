package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Line2D
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader
import javax.swing.table.TableModel

fun makeUI(): Component {
  val table = JTable(makeModel())
  table.setShowGrid(false)
  table.fillsViewportHeight = true
  table.border = BorderFactory.createMatteBorder(0, 1, 1, 1, Color.GRAY)

  val header = table.tableHeader
  header.reorderingAllowed = false
  header.defaultRenderer = FlatHeaderCellRenderer()
  header.background = table.background
  header.border = BorderFactory.createMatteBorder(1, 1, 1, 1, Color.GRAY)

  val scroll = JScrollPane(table)
  scroll.border = BorderFactory.createEmptyBorder()
  scroll.viewportBorder = BorderFactory.createEmptyBorder()

  val layerUI = TableHeaderRolloverLayerUI()
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(GridLayout(2, 1, 5, 5)).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JLayer(JScrollPane(JTable(makeModel())), layerUI))
    it.add(JLayer(scroll, layerUI))
    it.isOpaque = true
    it.background = scroll.viewport.background
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
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

private class FlatHeaderCellRenderer : DefaultTableCellRenderer() {
  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    (c as? JLabel)?.horizontalAlignment = CENTER
    return c
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.height = 24
    return d
  }
}

private class TableHeaderRolloverLayerUI : LayerUI<JScrollPane>() {
  private var rollover = false

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (c is JLayer<*> && rollover) {
      val scroll = c.view as? JScrollPane
      val table = scroll?.viewport?.view as? JTable
      val header = table?.tableHeader ?: return
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = Color.GRAY
      val s: Line2D = Line2D.Double()
      val size = header.columnModel.columnCount
      val gap = 2.0
      for (i in 0..<size) {
        val r = header.getHeaderRect(i)
        val y1 = r.getY() + gap
        val y2 = r.getY() + r.getHeight() - gap - gap
        s.setLine(r.getX(), y1, r.getX(), y2)
        if (i != 0) {
          g2.draw(s)
        }
        if (i < size - 1) {
          val xx = r.getX() + r.getWidth() - gap
          s.setLine(xx, y1, xx, y2)
          g2.draw(s)
        }
      }
      g2.dispose()
    }
  }

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask =
      AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane?>) {
    super.processMouseEvent(e, l)
    val c = e.component
    if (e.id == MouseEvent.MOUSE_RELEASED) {
      rollover = c.bounds.contains(e.point)
      c.repaint()
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane?>) {
    super.processMouseMotionEvent(e, l)
    val c = e.component
    val id = e.id
    val b = id == MouseEvent.MOUSE_MOVED || id == MouseEvent.MOUSE_DRAGGED
    rollover = b && c is JTableHeader
    l.repaint(c.bounds)
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
