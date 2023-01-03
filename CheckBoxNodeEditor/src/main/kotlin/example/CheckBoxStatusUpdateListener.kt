package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    if (adjusting) {
      return
    }
    adjusting = true
    val model = e.source as? DefaultTreeModel ?: return
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
    // To indicate the root has changed, childIndices and children will be null.
    val children = e.children
    val isRoot = children == null

    // If the parent node exists, update its status
    if (!isRoot) {
      val parent = e.treePath
      var n = parent.lastPathComponent as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.parent as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    }

    // Update the status of all child nodes to be the same as the current node status
    val isOnlyOneNodeSelected = children != null && children.size == 1
    val current = if (isOnlyOneNodeSelected) children[0] else model.root
    if (current is DefaultMutableTreeNode) {
      val status = (current.userObject as? CheckBoxNode)?.status ?: Status.INDETERMINATE
      updateAllChildrenUserObject(current, status)
      model.nodeChanged(current)
    }

    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .mapNotNull { (it.userObject as? CheckBoxNode)?.status }

    (parent.userObject as? CheckBoxNode)?.also { check ->
      val status = when {
        list.all { it === Status.DESELECTED } -> Status.DESELECTED
        list.all { it === Status.SELECTED } -> Status.SELECTED
        else -> Status.INDETERMINATE
      }
      parent.userObject = CheckBoxNode(check.label, status)
    }
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, status: Status) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it != parent }
      .forEach {
        val label = (it.userObject as? CheckBoxNode)?.label ?: ""
        it.userObject = CheckBoxNode(label, status)
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) { /* not needed */ }

  override fun treeNodesRemoved(e: TreeModelEvent) { /* not needed */ }

  override fun treeStructureChanged(e: TreeModelEvent) { /* not needed */ }
}

class CheckBoxNodeRenderer : TreeCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val checkBox = TriStateCheckBox()
  private val tcr = DefaultTreeCellRenderer()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = tcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    c.font = tree.font

    val treeNode = value as? DefaultMutableTreeNode ?: return c
    panel.isFocusable = false
    panel.isRequestFocusEnabled = false
    panel.isOpaque = false
    checkBox.isEnabled = tree.isEnabled
    checkBox.font = tree.font
    checkBox.isFocusable = false
    checkBox.isOpaque = false
    (treeNode.userObject as? CheckBoxNode)?.also {
      checkBox.icon = if (it.status === Status.INDETERMINATE) IndeterminateIcon() else null
      (c as? JLabel)?.text = it.label
      checkBox.isSelected = it.status === Status.SELECTED
    }
    panel.add(checkBox, BorderLayout.WEST)
    panel.add(c)
    return panel
  }
}

class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val panel = JPanel(BorderLayout())
  private val checkBox = object : TriStateCheckBox() {
    @Transient
    private var handler: ActionListener? = null

    override fun updateUI() {
      removeActionListener(handler)
      super.updateUI()
      isOpaque = false
      isFocusable = false
      handler = ActionListener { stopCellEditing() }
      addActionListener(handler)
    }
  }
  private val renderer = DefaultTreeCellRenderer()
  private var str: String? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true)
    c.font = tree.font

    val treeNode = value as? DefaultMutableTreeNode ?: return c
    panel.isFocusable = false
    panel.isRequestFocusEnabled = false
    panel.isOpaque = false
    checkBox.isEnabled = tree.isEnabled
    checkBox.font = tree.font
    // checkBox.setFocusable(false)
    // checkBox.setOpaque(false)
    (treeNode.userObject as? CheckBoxNode)?.also {
      if (it.status === Status.INDETERMINATE) {
        checkBox.icon = IndeterminateIcon()
      } else {
        checkBox.icon = null
      }
      (c as? JLabel)?.text = it.label
      checkBox.isSelected = it.status === Status.SELECTED
      str = it.label
    }
    panel.add(checkBox, BorderLayout.WEST)
    panel.add(c)
    return panel
  }

  override fun getCellEditorValue() =
    CheckBoxNode(str ?: "", if (checkBox.isSelected) Status.SELECTED else Status.DESELECTED)

  override fun isCellEditable(e: EventObject?): Boolean {
    val tree = e?.source
    if (e is MouseEvent && tree is JTree) {
      val p = e.point
      val path = tree.getPathForLocation(p.x, p.y)
      return tree.getPathBounds(path)?.let { r ->
        r.width = checkBox.preferredSize.width
        r.contains(p)
      } ?: false
    }
    return false
  }
}
