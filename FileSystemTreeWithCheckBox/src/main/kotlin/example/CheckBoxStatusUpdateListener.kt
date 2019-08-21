package example

import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    if (adjusting) {
      return
    }
    adjusting = true
    val children = e.getChildren()
    val model = e.getSource() as? DefaultTreeModel ?: return

    val node: DefaultMutableTreeNode
    val c: CheckBoxNode
    val isOnlyOneNodeSelected = children != null && children.size == 1
    if (isOnlyOneNodeSelected) {
      node = children[0] as DefaultMutableTreeNode
      c = node.getUserObject() as CheckBoxNode
      val parent = e.getTreePath()
      var n = parent.getLastPathComponent() as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.getParent() as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    } else {
      node = model.getRoot() as DefaultMutableTreeNode
      c = node.getUserObject() as CheckBoxNode
    }
    updateAllChildrenUserObject(node, c.getStatus())
    model.nodeChanged(node)
    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance(DefaultMutableTreeNode::class.java)
      .map { it.getUserObject() }
      .filterIsInstance(CheckBoxNode::class.java)
      .map { it.getStatus() }

    (parent.getUserObject() as? CheckBoxNode)?.also {
      val status = when {
        list.all { it === Status.DESELECTED } -> Status.DESELECTED
        list.all { it === Status.SELECTED } -> Status.SELECTED
        else -> Status.INDETERMINATE
      }
      parent.setUserObject(CheckBoxNode(it.getFile(), status))
    }
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, status: Status) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance(DefaultMutableTreeNode::class.java)
      .filter { it != parent }
      .forEach {
        (it.getUserObject() as? CheckBoxNode)?.also { check ->
          it.setUserObject(CheckBoxNode(check.getFile(), status))
        }
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) { /* not needed */ }

  override fun treeNodesRemoved(e: TreeModelEvent) { /* not needed */ }

  override fun treeStructureChanged(e: TreeModelEvent) { /* not needed */ }
}
