package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.util.EnumSet
import java.util.EventObject
import javax.swing.*
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
    val model = e.source as? DefaultTreeModel
    if (adjusting || model == null) {
      return
    }
    adjusting = true
    try {
      // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
      // To indicate the root has changed, childIndices and children will be null.
      val children = e.children
      val isRoot = children == null
      val changedNodes = children ?: arrayOf(model.root)

      // Update the status of all descendant nodes to be the same as the changed node status
      changedNodes.filterIsInstance<DefaultMutableTreeNode>().forEach { node ->
        (node.userObject as? CheckBoxNode)?.also {
          updateAllChildrenUserObject(node, it.status)
          model.nodeChanged(node)
        }
      }

      // If the parent node exists, update the status of the ancestor nodes up to the root
      if (!isRoot) {
        generateSequence(e.treePath) { it.parentPath }
          .mapNotNull { it.lastPathComponent as? DefaultMutableTreeNode }
          .forEach {
            updateParentUserObject(it)
            model.nodeChanged(it)
          }
      }
    } finally {
      // Always reset the flag, otherwise this listener stops working after an exception
      adjusting = false
    }
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    (parent.userObject as? CheckBoxNode)?.also { check ->
      val set = parent
        .children()
        .toList()
        .filterIsInstance<DefaultMutableTreeNode>()
        .mapNotNullTo(EnumSet.noneOf(Status::class.java)) {
          (it.userObject as? CheckBoxNode)?.status
        }
      // SELECTED or DESELECTED only if all children have that same status
      val status = set.singleOrNull() ?: Status.INDETERMINATE
      parent.userObject = CheckBoxNode(check.label, status)
    }
  }

  private fun updateAllChildrenUserObject(
    parent: DefaultMutableTreeNode,
    status: Status,
  ) {
    parent
      .breadthFirstEnumeration()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it != parent }
      .forEach { node ->
        (node.userObject as? CheckBoxNode)?.also {
          node.userObject = CheckBoxNode(it.label, status)
        }
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) {
    // not needed
  }

  override fun treeNodesRemoved(e: TreeModelEvent) {
    // not needed
  }

  override fun treeStructureChanged(e: TreeModelEvent) {
    // not needed
  }
}

class CheckBoxNodeRenderer : TreeCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val checkBox = TriStateCheckBox()
  private val tcr = DefaultTreeCellRenderer()

  init {
    // These properties do not depend on the node, so set them only once
    panel.isFocusable = false
    panel.isRequestFocusEnabled = false
    panel.isOpaque = false
    panel.add(checkBox, BorderLayout.WEST)
    checkBox.isFocusable = false
    checkBox.isOpaque = false
  }

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = tcr.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    c.font = tree.font

    val treeNode = value as? DefaultMutableTreeNode ?: return c
    checkBox.isEnabled = tree.isEnabled
    checkBox.font = tree.font
    (treeNode.userObject as? CheckBoxNode)?.also {
      val indeterminate = it.status === Status.INDETERMINATE
      checkBox.icon = if (indeterminate) IndeterminateIcon() else null
      (c as? JLabel)?.text = it.label
      checkBox.isSelected = it.status === Status.SELECTED
    }
    panel.add(c)
    return panel
  }
}

class CheckBoxNodeEditor :
  AbstractCellEditor(),
  TreeCellEditor {
  private val panel = JPanel(BorderLayout())
  private val checkBox = object : TriStateCheckBox() {
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
  private var label: String? = null

  init {
    panel.isFocusable = false
    panel.isRequestFocusEnabled = false
    panel.isOpaque = false
    panel.add(checkBox, BorderLayout.WEST)
  }

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
  ): Component {
    val c = renderer.getTreeCellRendererComponent(
      tree,
      value,
      true,
      expanded,
      leaf,
      row,
      true,
    )
    c.font = tree.font

    val treeNode = value as? DefaultMutableTreeNode ?: return c
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
      label = it.label
    }
    panel.add(c)
    return panel
  }

  override fun getCellEditorValue(): Any {
    val v = if (checkBox.isSelected) Status.SELECTED else Status.DESELECTED
    return CheckBoxNode(label ?: "", v)
  }

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
