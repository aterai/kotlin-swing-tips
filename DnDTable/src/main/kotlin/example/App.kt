package example

import java.awt.* // ktlint-disable no-wildcard-imports
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
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
    arrayOf("eee", 1, true),
    arrayOf("GGG", 3, false),
    arrayOf("hhh", 72, true),
    arrayOf("fff", 4, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
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
        model.addRow(arrayOf("New row", model.rowCount, false))
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

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
    }
  }
}

private class DnDTable(model: TableModel?) : JTable(model), DragGestureListener, Transferable {
  private val targetLine = Rectangle()
  private var draggedIndex = -1
  private var targetIndex = -1

  init {
    DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, CDropTargetListener(), true)
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(this, DnDConstants.ACTION_COPY_OR_MOVE, this)
  }

  override fun prepareRenderer(tcr: TableCellRenderer, row: Int, column: Int): Component =
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

  private fun initTargetLine(p: Point) {
    val rect = Rectangle()
    val cellHeight = getRowHeight()
    val lineWidth = width
    val lineHeight = 2
    rect.setSize(lineWidth, cellHeight)
    targetLine.setSize(lineWidth, lineHeight)
    targetIndex = -1
    val rowCount = rowCount
    for (i in 0 until rowCount) {
      rect.setLocation(0, cellHeight * i - cellHeight / 2)
      if (rect.contains(p)) {
        targetIndex = i
        targetLine.setLocation(0, i * cellHeight)
        break
      }
    }
    if (targetIndex < 0) {
      targetIndex = rowCount
      targetLine.setLocation(0, targetIndex * cellHeight - lineHeight)
    }
  }

  // Interface: DragGestureListener
  override fun dragGestureRecognized(e: DragGestureEvent) {
    val oneOrMore = selectedRowCount > 1
    draggedIndex = rowAtPoint(e.dragOrigin)
    if (oneOrMore || draggedIndex < 0) {
      return
    }
    runCatching {
      e.startDrag(DragSource.DefaultMoveDrop, this, TableDragSourceListener())
    }
  }

  // Interface: Transferable
  override fun getTransferData(flavor: DataFlavor) = this

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.humanPresentableName == NAME

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
      initTargetLine(e.location)
      repaint()
    }

    override fun dropActionChanged(e: DropTargetDragEvent) {
      // if (isDragAcceptable(e)) {
      //   e.acceptDrag(e.getDropAction());
      // } else {
      //   e.rejectDrag();
      // }
    }

    override fun drop(e: DropTargetDropEvent) {
      val model = model as? DefaultTableModel ?: return
      if (isDropAcceptable(e)) {
        if (targetIndex == draggedIndex) {
          setRowSelectionInterval(targetIndex, targetIndex)
        } else {
          val tg = if (targetIndex < draggedIndex) targetIndex else targetIndex - 1
          model.moveRow(draggedIndex, draggedIndex, tg)
          setRowSelectionInterval(tg, tg)
        }
        e.dropComplete(true)
      } else {
        e.dropComplete(false)
      }
      e.dropComplete(false)
      cursor = Cursor.getDefaultCursor()
      targetIndex = -1
      repaint()
    }

    private fun isDragAcceptable(e: DropTargetDragEvent) = isDataFlavorSupported(e.currentDataFlavors[0])

    private fun isDropAcceptable(e: DropTargetDropEvent) = isDataFlavorSupported(e.transferable.transferDataFlavors[0])
  }

  companion object {
    private val LINE_COLOR = Color(0xFF_64_64)
    private const val NAME = "test"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
    private val EVEN_BACKGROUND = Color(0xF0_F0_F0)
  }
}

private class TableDragSourceListener : DragSourceListener {
  override fun dragEnter(e: DragSourceDragEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveDrop
  }

  override fun dragExit(e: DragSourceEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveNoDrop
  }

  override fun dragOver(e: DragSourceDragEvent) {
    /* not needed */
  }

  override fun dropActionChanged(e: DragSourceDragEvent) {
    /* not needed */
  }

  override fun dragDropEnd(e: DragSourceDropEvent) {
    // e.getDragSourceContext().setCursor(Cursor.getDefaultCursor());
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
