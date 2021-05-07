package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.dnd.DnDConstants
import java.awt.dnd.DragGestureEvent
import java.awt.dnd.DragGestureListener
import java.awt.dnd.DragGestureRecognizer
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
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree = DnDTree()
  tree.model = makeModel()
  for (i in 0 until tree.rowCount) {
    tree.expandRow(i)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultTreeModel {
  val set1 = DefaultMutableTreeNode("Set 001")
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(DefaultMutableTreeNode("33333"))
  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("asd fas df as"))
  set2.add(DefaultMutableTreeNode("asd f"))
  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("asd fas dfa sdf"))
  set3.add(DefaultMutableTreeNode("5555555555"))
  set3.add(DefaultMutableTreeNode("66666666666666"))
  val root = DefaultMutableTreeNode("Root")
  root.add(set1)
  root.add(set2)
  set2.add(set3)
  return DefaultTreeModel(root)
}

private class DnDTree : JTree() {
  private var dragGestureHandler: DragGestureRecognizer? = null
  private var treeDropTarget: DropTarget? = null
  private var dropTargetNode: TreeNode? = null
  private var draggedNode: TreeNode? = null

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setCellRenderer(DnDTreeCellRenderer())
    if (dragGestureHandler == null || treeDropTarget == null) {
      dragGestureHandler = DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
        this, DnDConstants.ACTION_MOVE, NodeDragGestureListener()
      )
      treeDropTarget = DropTarget(this, NodeDropTargetListener())
    }
  }

  private inner class NodeDragGestureListener : DragGestureListener {
    override fun dragGestureRecognized(dge: DragGestureEvent) {
      val pt = dge.dragOrigin
      val path = getPathForLocation(pt.x, pt.y)
      if (path == null || path.parentPath == null) {
        return
      }
      draggedNode = (path.lastPathComponent as? TreeNode)?.also {
        val trans = TreeNodeTransferable(it)
        DragSource.getDefaultDragSource().startDrag(dge, Cursor.getDefaultCursor(), trans, NodeDragSourceListener())
      }
    }
  }

  private inner class NodeDropTargetListener : DropTargetListener {
    override fun dropActionChanged(dtde: DropTargetDragEvent) {
      /* not needed */
    }

    override fun dragEnter(dtde: DropTargetDragEvent) {
      /* not needed */
    }

    override fun dragExit(dte: DropTargetEvent) {
      /* not needed */
    }

    override fun dragOver(e: DropTargetDragEvent) {
      val f = e.currentDataFlavors
      val isDataFlavorSupported = f[0].humanPresentableName == TreeNodeTransferable.NAME
      val pt = e.location
      val path = getPathForLocation(pt.x, pt.y)
      if (!isDataFlavorSupported || path == null) {
        rejectDrag(e)
        return
      }
      val draggingNode = selectionPath?.lastPathComponent as? MutableTreeNode
      val targetNode = path.lastPathComponent as? DefaultMutableTreeNode
      val parent = targetNode?.parent
      if (parent is DefaultMutableTreeNode && parent.path.contains(draggingNode)) {
        rejectDrag(e)
        return
      }
      dropTargetNode = targetNode
      e.acceptDrag(e.dropAction)
      repaint()
    }

    override fun drop(e: DropTargetDropEvent) {
      val draggingNode = selectionPath?.lastPathComponent
      val pt = e.location
      val path = getPathForLocation(pt.x, pt.y)
      if (path == null || draggingNode !is MutableTreeNode) {
        e.dropComplete(false)
        return
      }
      val model = model as? DefaultTreeModel
      val targetNode = path.lastPathComponent as? DefaultMutableTreeNode
      if (model == null || targetNode == null || targetNode == draggingNode) {
        e.dropComplete(false)
        return
      }
      e.acceptDrop(DnDConstants.ACTION_MOVE)
      model.removeNodeFromParent(draggingNode)
      val parent = targetNode.parent
      if (parent is MutableTreeNode && targetNode.isLeaf) {
        model.insertNodeInto(draggingNode, parent, parent.getIndex(targetNode))
      } else {
        model.insertNodeInto(draggingNode, targetNode, targetNode.childCount)
      }
      e.dropComplete(true)
      dropTargetNode = null
      draggedNode = null
      repaint()
    }

    private fun rejectDrag(e: DropTargetDragEvent) {
      e.rejectDrag()
      dropTargetNode = null
      repaint()
    }
  }

  private inner class DnDTreeCellRenderer : DefaultTreeCellRenderer() {
    private var isTargetNode = false
    private var isTargetNodeLeaf = false
    override fun getTreeCellRendererComponent(
      tree: JTree,
      value: Any?,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ): Component {
      if (value is TreeNode) {
        isTargetNode = value == dropTargetNode
        isTargetNodeLeaf = isTargetNode && value.isLeaf
      }
      return super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (isTargetNode) {
        g.color = Color.BLACK
        if (isTargetNodeLeaf) {
          g.drawLine(0, 0, size.width, 0)
        } else {
          g.drawRect(0, 0, size.width - 1, size.height - 1)
        }
      }
    }
  }
}

private class TreeNodeTransferable(private val obj: Any) : Transferable {
  @Throws(UnsupportedFlavorException::class)
  override fun getTransferData(df: DataFlavor) = if (isDataFlavorSupported(df)) {
    obj
  } else {
    throw UnsupportedFlavorException(df)
  }

  override fun isDataFlavorSupported(df: DataFlavor) = df.humanPresentableName == NAME

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  companion object {
    const val NAME = "TREE-TEST"
    private val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
  }
}

private class NodeDragSourceListener : DragSourceListener {
  override fun dragDropEnd(dsde: DragSourceDropEvent) {
    // dropTargetNode = null
    // draggedNode = null
    // repaint()
  }

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
