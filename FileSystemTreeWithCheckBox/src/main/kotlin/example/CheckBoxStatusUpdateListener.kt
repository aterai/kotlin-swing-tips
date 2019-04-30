package example

import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

internal class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    if (adjusting) {
      return
    }
    adjusting = true
    val children = e.getChildren()
    val model = e.getSource() as DefaultTreeModel

    val node: DefaultMutableTreeNode
    val c: CheckBoxNode // = (CheckBoxNode) node.getUserObject();
    val isOnlyOneNodeSelected = children != null && children.size == 1
    if (isOnlyOneNodeSelected) {
      node = children[0] as DefaultMutableTreeNode
      c = node.getUserObject() as CheckBoxNode
      val parent = e.getTreePath()
      var n = parent.getLastPathComponent() as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        val tmp = n.getParent() as? DefaultMutableTreeNode
        if (tmp != null) {
          n = tmp
        } else {
          break
        }
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
    (parent.getUserObject() as? CheckBoxNode)?.also { node ->
      val list = parent.children().toList()
        .filterIsInstance(DefaultMutableTreeNode::class.java)
        .map { it.getUserObject() }
        .filterIsInstance(CheckBoxNode::class.java)
        .map { it.getStatus() }
        .distinct()
      val status = if (list.all { it == Status.DESELECTED }) {
        Status.DESELECTED
      } else if (list.all { it == Status.SELECTED }) {
        Status.SELECTED
      } else {
        Status.INDETERMINATE
      }
      parent.setUserObject(CheckBoxNode(node.getFile(), status))
    }
  }

  private fun updateAllChildrenUserObject(root: DefaultMutableTreeNode, status: Status) {
    root.breadthFirstEnumeration().toList()
      .filterIsInstance(DefaultMutableTreeNode::class.java)
      .filter { it != root }
      .forEach { node ->
        val check = node.getUserObject() as CheckBoxNode
        node.setUserObject(CheckBoxNode(check.getFile(), status))
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) {
    /* not needed */
  }

  override fun treeNodesRemoved(e: TreeModelEvent) {
    /* not needed */
  }

  override fun treeStructureChanged(e: TreeModelEvent) {
    /* not needed */
  }
}
