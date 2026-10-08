package example

import java.awt.*
import java.awt.event.InputEvent
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.table.JTableHeader

fun createUI(): Component {
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
  private val dragAreaIcon = DragAreaIcon()

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

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    if (!draggableRect.isEmpty) {
      val iw = dragAreaIcon.iconWidth
      val x = draggableRect.x + (draggableRect.width - iw) / 2
      val y = draggableRect.y + 1
      dragAreaIcon.paintIcon(c, g, x, y)
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    super.processMouseEvent(e, l)
    val c = e.component
    if (c is JTableHeader) {
      val id = e.id
      if (id == MouseEvent.MOUSE_PRESSED) {
        updateIconAndCursor(c, e.point, l)
      } else if (id == MouseEvent.MOUSE_RELEASED) {
        c.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
        clearDraggableRect(c)
      } else if (id == MouseEvent.MOUSE_EXITED && !isMouseButtonDown(e)) {
        // Hide the drag handle icon when the cursor leaves the header,
        // but keep it while dragging a column outside the header
        clearDraggableRect(c)
      }
    }
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component
    if (c is JTableHeader) {
      if (e.id == MouseEvent.MOUSE_DRAGGED) {
        mouseDragged(e, l, c)
      } else if (e.id == MouseEvent.MOUSE_MOVED) {
        updateIconAndCursor(c, e.point, l)
        c.repaint()
      }
    }
  }

  private fun mouseDragged(
    e: MouseEvent,
    l: JLayer<out JScrollPane>,
    header: JTableHeader,
  ) {
    val draggedColumn = header.draggedColumn
    if (!draggableRect.isEmpty && draggedColumn != null) {
      // The dragged distance is updated by BasicTableHeaderUI after this
      // event is processed, so read it later on the EDT
      EventQueue.invokeLater {
        // Using columnAtPoint(...) would make the rectangle jump at the moment
        // the columns are swapped, so convert the model index of the dragged column
        val modelIndex = draggedColumn.modelIndex
        val viewIndex = header.table.convertColumnIndexToView(modelIndex)
        val rect = header.getHeaderRect(viewIndex)
        rect.x += header.draggedDistance
        draggableRect.bounds = SwingUtilities.convertRectangle(header, rect, l)
        header.repaint(rect)
      }
    } else {
      e.consume() // Refuse to start drag
    }
  }

  private fun updateIconAndCursor(header: JTableHeader, pt: Point, l: JLayer<*>) {
    val r = header.getHeaderRect(header.columnAtPoint(pt))
    r.height /= 2
    if (r.contains(pt)) {
      header.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
      draggableRect.bounds = SwingUtilities.convertRectangle(header, r, l)
    } else {
      header.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
      draggableRect.setSize(0, 0)
    }
  }

  private fun clearDraggableRect(header: JTableHeader) {
    if (!draggableRect.isEmpty) {
      draggableRect.setSize(0, 0)
      header.repaint()
    }
  }

  private fun isMouseButtonDown(e: MouseEvent): Boolean {
    val mask = InputEvent.BUTTON1_DOWN_MASK or
      InputEvent.BUTTON2_DOWN_MASK or
      InputEvent.BUTTON3_DOWN_MASK
    return (e.modifiersEx and mask) != 0
  }
}

private class DragAreaIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = SQUARE_COLOR
    // Center the 2 x 4 grid of squares horizontally
    val gridWidth = COLUMN_STEP * (COLUMN_COUNT - 1) + SQUARE_SIZE
    val firstColumn = (iconWidth - gridWidth) / 2
    val firstRow = 1
    val secondRow = firstRow + ROW_STEP
    for (i in 0..<COLUMN_COUNT) {
      val column = firstColumn + i * COLUMN_STEP
      g2.fillRect(column, firstRow, SQUARE_SIZE, SQUARE_SIZE)
      g2.fillRect(column, secondRow, SQUARE_SIZE, SQUARE_SIZE)
    }
    g2.dispose()
  }

  override fun getIconWidth() = 16

  override fun getIconHeight() = 12

  companion object {
    private val SQUARE_COLOR = Color(0x64_64_64_64, true)
    private const val SQUARE_SIZE = 2
    private const val COLUMN_COUNT = 4
    private const val COLUMN_STEP = 4
    private const val ROW_STEP = 3
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
