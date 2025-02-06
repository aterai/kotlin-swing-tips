package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val table = JTable(5, 3)
  val scroll = JScrollPane(table)
  scroll.columnHeader = object : JViewport() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = 24
      return d
    }
  }
  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())
  return JPanel(BorderLayout()).also {
    it.add(JLayer(scroll, ColumnDragLayerUI()))
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColumnDragLayerUI : LayerUI<JScrollPane>() {
  private val draggableRect = Rectangle()

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

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (!draggableRect.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      val icon = DragAreaIcon()
      val x = (draggableRect.centerX - icon.iconWidth / 2.0).toInt()
      val y = draggableRect.y + 1
      icon.paintIcon(c, g2, x, y)
      g2.dispose()
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseEvent(e, l)
    val c = e.component
    if (c is JTableHeader) {
      if (e.id == MouseEvent.MOUSE_PRESSED) {
        val pt = e.point
        updateIconAndCursor(c, pt, l)
      } else if (e.id == MouseEvent.MOUSE_RELEASED) {
        c.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        draggableRect.setSize(0, 0)
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    if (c is JTableHeader) {
      if (e.id == MouseEvent.MOUSE_DRAGGED) {
        val draggedColumn = c.draggedColumn
        if (!draggableRect.isEmpty && draggedColumn != null) {
          EventQueue.invokeLater {
            val modelIndex = draggedColumn.modelIndex
            val viewIndex = c.table.convertColumnIndexToView(modelIndex)
            val rect = c.getHeaderRect(viewIndex)
            rect.x += c.draggedDistance
            draggableRect.setRect(SwingUtilities.convertRectangle(c, rect, l))
            c.repaint(rect)
          }
        } else {
          e.consume() // Refuse to start drag
        }
      } else if (e.id == MouseEvent.MOUSE_MOVED) {
        val pt = e.point
        updateIconAndCursor(c, pt, l)
        c.repaint()
      }
    } else {
      c.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      draggableRect.setSize(0, 0)
    }
  }

  private fun updateIconAndCursor(header: JTableHeader, pt: Point, l: JLayer<*>) {
    val r = header.getHeaderRect(header.columnAtPoint(pt))
    r.height /= 2
    if (r.contains(pt)) {
      header.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      draggableRect.setRect(SwingUtilities.convertRectangle(header, r, l))
    } else {
      header.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      draggableRect.setSize(0, 0)
    }
  }
}

private class DragAreaIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val count = 4
    val diff = 3
    val firstColumn = (iconWidth - diff * count) / 2
    val firstRow = 1
    val secondRow = firstRow + diff
    for (i in 0..<count) {
      val column = firstColumn + i * diff
      drawSquare(g2, column, firstRow)
      drawSquare(g2, column, secondRow)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 12

  private fun drawSquare(g: Graphics, x: Int, y: Int) {
    g.color = SQUARE_COLOR
    g.fillRect(x, y, 2, 2)
  }

  companion object {
    private val SQUARE_COLOR = Color(0x64_64_64_64, true)
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
