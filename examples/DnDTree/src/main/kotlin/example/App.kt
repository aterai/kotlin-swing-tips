package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
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
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun createUI(): Component {
  val tree = DnDTree()
  tree.model = createModel()
  for (i in 0..<tree.rowCount) {
    tree.expandRow(i)
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel(): DefaultTreeModel {
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
  private var draggedPath: TreePath? = null
  private var dropTargetNode: TreeNode? = null

  init {
    // Register the gesture recognizer and the DropTarget only once here, not in updateUI()
    DragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
      this,
      DnDConstants.ACTION_MOVE,
      NodeDragGestureListener(),
    )
    // This DropTarget replaces the one installed by the TransferHandler.
    // It is not a UIResource, so it is kept even if updateUI() is called.
    dropTarget = DropTarget(this, NodeDropTargetListener())
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setCellRenderer(DnDTreeCellRenderer())
  }

  private fun updateDropTargetNode(node: TreeNode?) {
    // Repaint only when the drop target node changes, not on every dragOver
    if (dropTargetNode != node) {
      dropTargetNode = node
      repaint()
    }
  }

  private fun canDrop(path: TreePath?): Boolean {
    // TreePath#isDescendant(...) is also true for the same path,
    // so this prevents dropping a node onto itself or onto one of its descendants
    val dragged = draggedPath
    return dragged != null &&
      path?.lastPathComponent is MutableTreeNode &&
      !dragged.isDescendant(path)
  }

  private fun moveNode(
    node: MutableTreeNode,
    target: MutableTreeNode,
  ) {
    val model = model as? DefaultTreeModel ?: return
    model.removeNodeFromParent(node)
    val parent = target.parent
    if (parent is MutableTreeNode && target.isLeaf) {
      // Insert the node before the leaf target node
      model.insertNodeInto(node, parent, parent.getIndex(target))
    } else {
      // Append the node as the last child of the target node
      model.insertNodeInto(node, target, target.childCount)
    }
  }

  private inner class NodeDragGestureListener : DragGestureListener {
    override fun dragGestureRecognized(e: DragGestureEvent) {
      val pt = e.dragOrigin
      val path = getPathForLocation(pt.x, pt.y)
      // The root node cannot be dragged
      val isNotRoot = path?.parentPath != null
      if (isNotRoot && path.lastPathComponent is MutableTreeNode) {
        draggedPath = path
        val t = TreeNodeTransferable(path.lastPathComponent)
        e.startDrag(Cursor.getDefaultCursor(), t, NodeDragSourceListener())
      }
    }
  }

  private inner class NodeDropTargetListener : DropTargetListener {
    override fun dropActionChanged(e: DropTargetDragEvent) {
      // not needed
    }

    override fun dragEnter(e: DropTargetDragEvent) {
      // not needed
    }

    override fun dragExit(e: DropTargetEvent) {
      // Erase the drop location indicator when the cursor leaves the JTree
      updateDropTargetNode(null)
    }

    override fun dragOver(e: DropTargetDragEvent) {
      val pt = e.location
      val path = getPathForLocation(pt.x, pt.y)
      // Reject unsupported DataFlavors (e.g. files from the desktop),
      // non-node locations (e.g. margin area of JTree),
      // and dropping a parent node onto its child node
      if (e.isDataFlavorSupported(TreeNodeTransferable.FLAVOR) && canDrop(path)) {
        updateDropTargetNode(path?.lastPathComponent as? TreeNode)
        e.acceptDrag(e.dropAction)
      } else {
        e.rejectDrag()
        updateDropTargetNode(null)
      }
    }

    override fun drop(e: DropTargetDropEvent) {
      val pt = e.location
      val path = getPathForLocation(pt.x, pt.y)
      val node = draggedPath?.lastPathComponent as? MutableTreeNode
      val target = path?.lastPathComponent as? MutableTreeNode
      val supported = e.isDataFlavorSupported(TreeNodeTransferable.FLAVOR)
      if (supported && canDrop(path) && node != null && target != null) {
        e.acceptDrop(DnDConstants.ACTION_MOVE)
        moveNode(node, target)
        e.dropComplete(true)
      } else {
        e.rejectDrop()
      }
      updateDropTargetNode(null)
    }
  }

  private inner class NodeDragSourceListener : DragSourceListener {
    override fun dragDropEnd(e: DragSourceDropEvent) {
      draggedPath = null
      updateDropTargetNode(null)
    }

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
      hasFocus: Boolean,
    ): Component {
      isTargetNode = value != null && value == dropTargetNode
      isTargetNodeLeaf = isTargetNode && leaf
      return super.getTreeCellRendererComponent(
        tree,
        value,
        selected,
        expanded,
        leaf,
        row,
        hasFocus,
      )
    }

    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (isTargetNode) {
        g.color = Color.BLACK
        if (isTargetNodeLeaf) {
          // The dragged node will be inserted before this leaf node
          g.drawLine(0, 0, width, 0)
        } else {
          // The dragged node will be appended as a child of this node
          g.drawRect(0, 0, width - 1, height - 1)
        }
      }
    }
  }
}

private class TreeNodeTransferable(
  private val obj: Any,
) : Transferable {
  @Throws(UnsupportedFlavorException::class)
  override fun getTransferData(df: DataFlavor) = if (isDataFlavorSupported(df)) {
    obj
  } else {
    throw UnsupportedFlavorException(df)
  }

  override fun isDataFlavorSupported(df: DataFlavor) = FLAVOR == df

  override fun getTransferDataFlavors() = arrayOf(FLAVOR)

  companion object {
    private const val NAME = "TREE-TEST"
    val FLAVOR = DataFlavor(DataFlavor.javaJVMLocalObjectMimeType, NAME)
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
