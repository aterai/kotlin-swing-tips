package example

import java.awt.*
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
import java.awt.event.ActionEvent
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

fun makeUI(): Component {
  val tree = JTree()
  tree.dragEnabled = true
  tree.dropMode = DropMode.ON_OR_INSERT
  tree.transferHandler = TreeTransferHandler()
  tree.selectionModel.selectionMode = TreeSelectionModel.CONTIGUOUS_TREE_SELECTION
  val empty: Action = object : AbstractAction() {
    override fun actionPerformed(e: ActionEvent) {
      // do nothing
    }
  }
  tree.actionMap.put(TransferHandler.getCutAction().getValue(Action.NAME), empty)
  expandTree(tree)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun expandTree(tree: JTree) {
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row)
    row++
  }
}

private class TreeTransferHandler : TransferHandler() {
  private val nodesFlavor = DataFlavor(MutableList::class.java, "List of TreeNode")

  override fun getSourceActions(c: JComponent) =
    if (c is JTree && canStartDrag(c)) COPY_OR_MOVE else NONE

  override fun createTransferable(c: JComponent): Transferable? {
    var transferable: Transferable? = null
    (c as? JTree)?.selectionPaths?.also { selPaths ->
      val copies = mutableListOf<MutableTreeNode>()
      selPaths.forEach { path ->
        (path.lastPathComponent as? DefaultMutableTreeNode)?.also { node ->
          val clone = DefaultMutableTreeNode(node.userObject)
          copies.add(deepCopy(node, clone))
        }
      }
      transferable = object : Transferable {
        override fun getTransferDataFlavors() = arrayOf(nodesFlavor)

        override fun isDataFlavorSupported(flavor: DataFlavor) = nodesFlavor == flavor

        @Throws(UnsupportedFlavorException::class)
        override fun getTransferData(flavor: DataFlavor): Any {
          if (isDataFlavorSupported(flavor)) {
            return copies
          } else {
            throw UnsupportedFlavorException(flavor)
          }
        }
      }
    }
    return transferable
  }

  override fun canImport(support: TransferSupport): Boolean {
    val dl = support.dropLocation
    val c = support.component
    return support.isDrop &&
      support.isDataFlavorSupported(nodesFlavor) &&
      c is JTree &&
      dl is JTree.DropLocation &&
      canImportDropLocation(c, dl)
  }

  override fun importData(support: TransferSupport): Boolean {
    val c = support.component
    val dl = support.dropLocation
    val transferable = support.transferable
    return canImport(support) &&
      c is JTree &&
      dl is JTree.DropLocation &&
      insertNode(c, dl, transferable)
  }

  private fun insertNode(
    tree: JTree,
    dl: JTree.DropLocation,
    transferable: Transferable,
  ): Boolean {
    val path = dl.path
    val parent = path.lastPathComponent
    val model = tree.model
    val nodes = getTransferData(transferable)
    if (parent is MutableTreeNode && model is DefaultTreeModel) {
      val index = AtomicInteger(getDropIndex(parent, dl.childIndex))
      nodes
        .filterIsInstance<MutableTreeNode>()
        .forEach {
          model.insertNodeInto(it, parent, index.getAndIncrement())
        }
    }
    return nodes.isNotEmpty()
  }

  private fun getTransferData(t: Transferable) = runCatching {
    t.getTransferData(nodesFlavor) as? List<*>
  }.getOrNull() ?: emptyList<Any>()

  override fun exportDone(src: JComponent, data: Transferable, action: Int) {
    if (src is JTree && action and MOVE == MOVE) {
      cleanup(src)
    }
  }

  private fun cleanup(tree: JTree) {
    val model = tree.model
    val selectionPaths = tree.selectionPaths
    if (selectionPaths != null && model is DefaultTreeModel) {
      for (path in selectionPaths) {
        model.removeNodeFromParent(path.lastPathComponent as? MutableTreeNode)
      }
    }
  }
}

private fun getDropIndex(parent: MutableTreeNode, childIndex: Int): Int {
  var index = childIndex // DropMode.INSERT
  if (childIndex == -1) { // DropMode.ON
    index = parent.childCount
  }
  return index
}

fun canStartDrag(tree: JTree) = tree.selectionPaths?.let { canStartDragPaths(it) } ?: false

fun canStartDragPaths(paths: Array<TreePath>) = paths
  .asSequence()
  .map { it.lastPathComponent }
  .filterIsInstance<DefaultMutableTreeNode>()
  .map { it.level }
  .distinct()
  .count { it != 0 } == 1

fun canImportDropLocation(tree: JTree, dl: JTree.DropLocation): Boolean {
  val pt = dl.dropPoint
  val dropRow = tree.getRowForLocation(pt.x, pt.y)
  return tree.selectionRows?.asSequence()?.none {
    it == dropRow || isDescendant(tree, it, dropRow)
  } ?: false
}

private fun isDescendant(tree: JTree, selRow: Int, dropRow: Int): Boolean {
  val node = tree.getPathForRow(selRow).lastPathComponent
  return node is DefaultMutableTreeNode && isDescendant2(tree, dropRow, node)
}

private fun isDescendant2(
  tree: JTree,
  dropRow: Int,
  node: DefaultMutableTreeNode,
) = node
  .depthFirstEnumeration()
  .asSequence()
  .filterIsInstance<DefaultMutableTreeNode>()
  .map { it.path }
  .map { TreePath(it) }
  .toList()
  .map { tree.getRowForPath(it) }
  .any { it == dropRow }

fun deepCopy(src: MutableTreeNode, tgt: DefaultMutableTreeNode): DefaultMutableTreeNode {
  src
    .children()
    .asSequence()
    .filterIsInstance<DefaultMutableTreeNode>()
    .forEach {
      val clone = DefaultMutableTreeNode(it.userObject)
      tgt.add(clone)
      if (!it.isLeaf) {
        deepCopy(it, clone)
      }
    }
  return tgt
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
