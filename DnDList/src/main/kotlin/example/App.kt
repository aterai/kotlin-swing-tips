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

fun makeUI() = JPanel(BorderLayout()).also {
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

private class DnDList<E> : JList<E>(), DragGestureListener, Transferable {
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
      renderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus).also {
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
    val rect = getCellBounds(0, 0)
    val cellHeight = rect.height
    val lineHeight = 2
    val modelSize = model.size
    targetIndex = -1
    targetLine.setSize(rect.width, lineHeight)
    for (i in 0 until modelSize) {
      rect.setLocation(0, cellHeight * i - cellHeight / 2)
      if (rect.contains(p)) {
        targetIndex = i
        targetLine.setLocation(0, i * cellHeight)
        break
      }
    }
    if (targetIndex < 0) {
      targetIndex = modelSize
      targetLine.setLocation(0, targetIndex * cellHeight - lineHeight)
    }
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
      val model = model as? DefaultListModel<E> ?: return
      if (isDropAcceptable(e) && targetIndex >= 0) {
        val str = model[draggedIndex]
        when {
          targetIndex == draggedIndex -> setSelectedIndex(targetIndex)

          targetIndex < draggedIndex -> model.also {
            it.remove(draggedIndex)
            it.add(targetIndex, str)
            setSelectedIndex(targetIndex)
          }

          else -> model.also {
            it.add(targetIndex, str)
            it.remove(draggedIndex)
            setSelectedIndex(targetIndex - 1)
          }
        }
        e.dropComplete(true)
      } else {
        e.dropComplete(false)
      }
      e.dropComplete(false)
      targetIndex = -1
      repaint()
    }

    private fun isDragAcceptable(e: DropTargetDragEvent) =
      isDataFlavorSupported(e.currentDataFlavors[0])

    private fun isDropAcceptable(e: DropTargetDropEvent) =
      isDataFlavorSupported(e.transferable.transferDataFlavors[0])
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
