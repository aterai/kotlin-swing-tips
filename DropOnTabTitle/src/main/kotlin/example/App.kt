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

fun makeUI(): Component {
  val tabs = JTabbedPane()
  tabs.addTab("00000000", JScrollPane(DnDList(makeModel(0))))
  tabs.addTab("11111111", JScrollPane(DnDList(makeModel(1))))
  tabs.addTab("22222222", JScrollPane(DnDList(makeModel(2))))
  DropTarget(tabs, DnDConstants.ACTION_MOVE, TabTitleDropTargetListener(), true)

  return JPanel(BorderLayout()).also {
    it.add(tabs)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(index: Int) = DefaultListModel<String>().also {
  it.addElement("$index - 1111")
  it.addElement("$index - 22222222")
  it.addElement("$index - 333333333333")
  it.addElement("$index - 444444444444444444")
  it.addElement("$index - 5555555555")
  it.addElement("$index - ****")
}

private class DnDList<E>(
  model: ListModel<E>
) : JList<E>(model), DragGestureListener, DragSourceListener, Transferable {
  init {
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
      this,
      DnDConstants.ACTION_MOVE,
      this
    )
  }

  // Interface: DragGestureListener
  override fun dragGestureRecognized(e: DragGestureEvent) {
    runCatching {
      e.startDrag(DragSource.DefaultMoveDrop, this, this)
    }
  }

  // Interface: DragSourceListener
  override fun dragEnter(e: DragSourceDragEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveDrop
  }

  override fun dragExit(e: DragSourceEvent) {
    e.dragSourceContext.cursor = DragSource.DefaultMoveNoDrop
  }

  override fun dragOver(e: DragSourceDragEvent) {
    // not needed
  }

  override fun dragDropEnd(e: DragSourceDropEvent) {
    // not needed
  }

  override fun dropActionChanged(e: DragSourceDragEvent) {
    // not needed
  }

  // Interface: Transferable
  override fun getTransferData(flavor: DataFlavor) = this

  override fun getTransferDataFlavors() =
    arrayOf(DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME))

  override fun isDataFlavorSupported(flavor: DataFlavor) = flavor.humanPresentableName == NAME

  companion object {
    private const val NAME = "test"
  }
}

private class TabTitleDropTargetListener : DropTargetListener {
  private var targetTabIndex = -1

  override fun dropActionChanged(e: DropTargetDragEvent) {
    // repaint()
  }

  override fun dragExit(e: DropTargetEvent) {
    // repaint()
  }

  override fun dragEnter(e: DropTargetDragEvent) {
    // repaint()
  }

  override fun dragOver(e: DropTargetDragEvent) {
    if (isDropAcceptable(e)) {
      e.acceptDrag(e.dropAction)
    } else {
      e.rejectDrag()
    }
    e.dropTargetContext.component.repaint()
  }

  @Suppress("UNCHECKED_CAST")
  override fun drop(e: DropTargetDropEvent) {
    runCatching {
      val c = e.dropTargetContext
      val o = c.component
      val t = e.transferable
      val f = t.transferDataFlavors
      if (o is JTabbedPane) {
        val sp = o.getComponentAt(targetTabIndex) as? JScrollPane
        val vp = sp?.viewport
        val targetList = SwingUtilities.getUnwrappedView(vp) as? JList<String>
        val sourceList = t.getTransferData(f[0]) as? JList<String>
        val tm = targetList?.model as? DefaultListModel<String>
        val sm = sourceList?.model as? DefaultListModel<String>
        val indices = sourceList?.selectedIndices
        if (indices != null && tm != null && sm != null) {
          for (i in indices.indices.reversed()) {
            tm.addElement(sm.remove(indices[i]))
          }
          e.dropComplete(true)
        } else {
          e.dropComplete(false)
        }
      } else {
        e.dropComplete(false)
      }
    }.onFailure {
      e.dropComplete(false)
    }
  }

  private fun isDropAcceptable(e: DropTargetDragEvent): Boolean {
    val c = e.dropTargetContext
    val t = e.transferable
    val f = t.transferDataFlavors
    val pt = e.location
    targetTabIndex = -1
    val tabs = c.component
    if (tabs is JTabbedPane) {
      for (i in 0 until tabs.tabCount) {
        if (tabs.getBoundsAt(i).contains(pt)) {
          targetTabIndex = i
          break
        }
      }
      val b = targetTabIndex >= 0 && targetTabIndex != tabs.selectedIndex
      return b && t.isDataFlavorSupported(f[0])
    }
    return false
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
