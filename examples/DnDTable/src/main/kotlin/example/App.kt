package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragSource
import java.awt.dnd.DragSourceDragEvent
import java.awt.dnd.DragSourceDropEvent
import java.awt.dnd.DragSourceEvent
import java.awt.dnd.DragSourceListener
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.awt.dnd.DropTargetListener
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

private val COLUMN_CLASSES = arrayOf(
  String::class.java,
  Int::class.javaObjectType,
  Boolean::class.javaObjectType,
)

fun createUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
    arrayOf("eee", 1, true),
    arrayOf("GGG", 3, false),
    arrayOf("hhh", 72, true),
    arrayOf("fff", 4, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    // Do not depend on getValueAt(0, column): all rows can be deleted from the popup menu
    override fun getColumnClass(column: Int) = COLUMN_CLASSES[column]
  }
  val table = DnDTable(model)
  table.fillsViewportHeight = true
  table.componentPopupMenu = TablePopupMenu()
  table.columnModel.getColumn(0).also {
    it.minWidth = 60
    it.maxWidth = 60
    it.resizable = false
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf<Any>("New row", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }
}

private class DnDTable(
  model: TableModel?,
) : JTable(model),
  DragGestureListener,
  Transferable {
  private val targetLine = Rectangle()
  private var draggedIndex = -1
  private var targetIndex = -1
  private val dsl = TableDragSourceListener()

  init {
    DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, CDropTargetListener(), true)
    val ds = DragSource.getDefaultDragSource()
    ds.createDefaultDragGestureRecognizer(
      this,
      DnDConstants.ACTION_COPY_OR_MOVE,
      this,
    )
  }

  override fun prepareRenderer(
    tcr: TableCellRenderer,
    row: Int,
    column: Int,
  ): Component =
    super.prepareRenderer(tcr, row, column).also {
      if (isRowSelected(row)) {
        it.foreground = getSelectionForeground()
        it.background = getSelectionBackground()
      } else {
        it.foreground = foreground
        it.background = if (row % 2 == 0) EVEN_BACKGROUND else background
      }
    }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    if (targetIndex >= 0) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = LINE_COLOR
      g2.fill(targetLine)
      g2.dispose()
    }
  }

  private fun updateTargetLine(p: Point) {
    val rowCount = rowCount
    val row = rowAtPoint(p)
    targetIndex = if (row < 0) {
      rowCount
    } else {
      // Insert after the row if the cursor is in its lower half
      val r = getCellRect(row, 0, true)
      if (p.y < r.centerY) row else row + 1
    }
    val y = when {
      targetIndex < rowCount -> getCellRect(targetIndex, 0, true).y

      rowCount > 0 -> getCellRect(rowCount - 1, 0, true).let {
        it.y + it.height -
          LINE_HEIGHT
      }

      else -> 0
    }
    targetLine.setBounds(0, y, width, LINE_HEIGHT)
  }

  // Interface: DragGestureListener
  override fun dragGestureRecognized(e: DragGestureEvent) {
    draggedIndex = rowAtPoint(e.dragOrigin)
    // Only a single selected row under the drag origin can be moved
    if (selectedRowCount == 1 && draggedIndex >= 0 && isRowSelected(draggedIndex)) {
      runCatching {
        e.startDrag(DragSource.DefaultMoveDrop, this, dsl)
      }
    }
  }

  // Interface: Transferable
  override fun getTransferData(flavor: DataFlavor) = this

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  override fun isDataFlavorSupported(flavor: DataFlavor) = FLAVOR == flavor

  private inner class CDropTargetListener : DropTargetListener {
    override fun dragExit(e: DropTargetEvent) {
      targetIndex = -1
      repaint()
    }

    override fun dragEnter(e: DropTargetDragEvent) {
      if (isDragAcceptable(e)) {
        e.acceptDrag(e.dropAction)
      } else {
        e.rejectDrag()
      }
    }

    override fun dragOver(e: DropTargetDragEvent) {
      if (isDragAcceptable(e)) {
        e.acceptDrag(e.dropAction)
        cursor = DragSource.DefaultMoveDrop
      } else {
        e.rejectDrag()
        cursor = DragSource.DefaultMoveNoDrop
        return
      }
      updateTargetLine(e.location)
      repaint()
    }

    override fun dropActionChanged(e: DropTargetDragEvent) {
      // not needed
    }

    override fun drop(e: DropTargetDropEvent) {
      val model = model as? DefaultTableModel
      if (model != null && isDropAcceptable(e)) {
        e.acceptDrop(e.dropAction)
        // Removing the dragged row shifts the following rows up by one
        val toIndex = if (targetIndex > draggedIndex) targetIndex - 1 else targetIndex
        if (toIndex != draggedIndex) {
          model.moveRow(draggedIndex, draggedIndex, toIndex)
        }
        setRowSelectionInterval(toIndex, toIndex)
        e.dropComplete(true)
      } else {
        e.rejectDrop()
      }
      cursor = Cursor.getDefaultCursor()
      targetIndex = -1
      repaint()
    }

    private fun isDragAcceptable(e: DropTargetDragEvent) =
      draggedIndex >= 0 && e.isDataFlavorSupported(FLAVOR)

    private fun isDropAcceptable(e: DropTargetDropEvent) =
      draggedIndex >= 0 && targetIndex >= 0 && e.isDataFlavorSupported(FLAVOR)
  }

  private inner class TableDragSourceListener : DragSourceListener {
    override fun dragEnter(e: DragSourceDragEvent) {
      e.dragSourceContext.cursor = DragSource.DefaultMoveDrop
    }

    override fun dragExit(e: DragSourceEvent) {
      e.dragSourceContext.cursor = DragSource.DefaultMoveNoDrop
    }

    override fun dragOver(e: DragSourceDragEvent) {
      // not needed
    }

    override fun dropActionChanged(e: DragSourceDragEvent) {
      // not needed
    }

    override fun dragDropEnd(e: DragSourceDropEvent) {
      // Always called at the end of a drag, including a cancel or a drop outside the table
      cursor = Cursor.getDefaultCursor()
      draggedIndex = -1
      targetIndex = -1
      repaint()
    }
  }

  companion object {
    private const val LINE_HEIGHT = 2
    private val LINE_COLOR = Color(0xFF_64_64)
    private const val NAME = "test"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
    private val EVEN_BACKGROUND = Color(0xF0_F0_F0)
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
