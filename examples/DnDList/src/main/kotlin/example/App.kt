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

fun createUI() = JPanel(BorderLayout()).also {
  val model = DefaultListModel<String>()
  model.addElement("1111")
  model.addElement("22222222")
  model.addElement("333333333333")
  model.addElement("<<<<<<---->>>>>>")
  model.addElement("============")
  model.addElement("****")

  val list = DnDList<String>()
  list.model = model

  it.add(JScrollPane(list))
  it.preferredSize = Dimension(320, 240)
}

private class DnDList<E> :
  JList<E>(),
  DragGestureListener,
  Transferable {
  private val targetLine = Rectangle()
  private var draggedIndex = -1
  private var targetIndex = -1
  private val dsl = ListDragSourceListener()

  init {
    DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, CDropTargetListener(), true)
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
      this,
      DnDConstants.ACTION_COPY_OR_MOVE,
      this,
    )
  }

  override fun updateUI() {
    cellRenderer = null
    super.updateUI()
    val renderer = cellRenderer
    setCellRenderer { list, value, index, isSelected, cellHasFocus ->
      renderer
        .getListCellRendererComponent(
          list,
          value,
          index,
          isSelected,
          cellHasFocus,
        ).also {
          if (isSelected) {
            it.foreground = list.selectionForeground
            it.background = list.selectionBackground
          } else {
            it.foreground = list.foreground
            it.background = if (index % 2 == 0) EVEN_BACKGROUND else list.background
          }
        }
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
    var index = locationToIndex(p)
    if (index < 0) {
      targetIndex = -1
      return
    }
    // Insert before the nearest cell if the cursor is in its upper half,
    // otherwise insert after it (also works with variable cell heights).
    val rect = getCellBounds(index, index)
    val lineHeight = 2
    var y = rect.y
    if (p.y >= rect.centerY) {
      index++
      y += rect.height
    }
    if (index == model.size) {
      // Keep the line inside the bottom edge of the last cell
      y -= lineHeight
    }
    targetIndex = index
    targetLine.setBounds(rect.x, y, rect.width, lineHeight)
  }

  // Interface: DragGestureListener
  override fun dragGestureRecognized(e: DragGestureEvent) {
    val oneOrMore = selectedIndices.size > 1
    draggedIndex = locationToIndex(e.dragOrigin)
    if (oneOrMore || draggedIndex < 0) {
      return
    }
    runCatching {
      e.startDrag(DragSource.DefaultMoveDrop, this, dsl)
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
      } else {
        e.rejectDrag()
        return
      }
      initTargetLine(e.location)
      repaint()
    }

    override fun dropActionChanged(e: DropTargetDragEvent) {
      // if (isDragAcceptable(e)) {
      //   e.acceptDrag(e.getDropAction())
      // } else {
      //   e.rejectDrag()
      // }
    }

    override fun drop(e: DropTargetDropEvent) {
      val model = model as? DefaultListModel<E>
      val acceptable = model != null &&
        e.isDataFlavorSupported(FLAVOR) &&
        targetIndex >= 0 &&
        draggedIndex >= 0
      if (acceptable) {
        val value = model.remove(draggedIndex)
        // The target index shifts up by one after removing an item above it
        val index = if (targetIndex > draggedIndex) targetIndex - 1 else targetIndex
        model.add(index, value)
        setSelectedIndex(index)
      }
      e.dropComplete(acceptable)
      targetIndex = -1
      repaint()
    }

    private fun isDragAcceptable(e: DropTargetDragEvent) =
      e.isDataFlavorSupported(FLAVOR)
  }

  companion object {
    private val LINE_COLOR = Color(0x64_64_FF)
    private const val NAME = "test"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
    private val EVEN_BACKGROUND = Color(0xF0_F0_F0)
  }
}

private class ListDragSourceListener : DragSourceListener {
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
    // not needed
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
