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
    val model = e.getSource() as? DefaultTreeModel ?: return
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
    // To indicate the root has changed, childIndices and children will be null.
    val children = e.getChildren()
    val isRoot = children == null

    // If the parent node exists, update its status
    if (!isRoot) {
      val parent = e.getTreePath()
      var n = parent.getLastPathComponent() as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.getParent() as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    }

    // Update the status of all child nodes to be the same as the current node status
    val isOnlyOneNodeSelected = children != null && children.size == 1
    val current = if (isOnlyOneNodeSelected) children[0] else model.getRoot()
    if (current is DefaultMutableTreeNode) {
      val status = (current.getUserObject() as? CheckBoxNode)?.getStatus() ?: Status.INDETERMINATE
      updateAllChildrenUserObject(current, status)
      model.nodeChanged(current)
    }

    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .mapNotNull { (it.getUserObject() as? CheckBoxNode)?.getStatus() }

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
      .filterIsInstance<DefaultMutableTreeNode>()
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
