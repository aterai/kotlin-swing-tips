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
    val children = e.getChildren()
    val model = e.getSource() as? DefaultTreeModel ?: return

    val node: DefaultMutableTreeNode
    val c: CheckBoxNode
    val isNotRootAndOnlyOneNodeChanged = children != null && children.size == 1
    if (isNotRootAndOnlyOneNodeChanged) {
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
    updateAllChildrenUserObject(node, c.status)
    model.nodeChanged(node)
    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance(DefaultMutableTreeNode::class.java)
      .map { it.getUserObject() }
      .filterIsInstance(CheckBoxNode::class.java)
      .map { it.status }

    (parent.getUserObject() as? CheckBoxNode)?.also {
      val status = when {
        list.all { it === Status.DESELECTED } -> Status.DESELECTED
        list.all { it === Status.SELECTED } -> Status.SELECTED
        else -> Status.INDETERMINATE
      }
      parent.setUserObject(CheckBoxNode(it.label, status))
    }
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, status: Status) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance(DefaultMutableTreeNode::class.java)
      .filter { it != parent }
      .forEach {
        val check = it.getUserObject() as CheckBoxNode
        it.setUserObject(CheckBoxNode(check.label, status))
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) {} // not needed

  override fun treeNodesRemoved(e: TreeModelEvent) {} // not needed

  override fun treeStructureChanged(e: TreeModelEvent) {} // not needed
}

class CheckBoxNodeRenderer : TreeCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val checkBox = TriStateCheckBox()
  private val renderer = DefaultTreeCellRenderer()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val l = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus) as JLabel
    l.setFont(tree.getFont())

    val treeNode = value as? DefaultMutableTreeNode ?: return l
    panel.setFocusable(false)
    panel.setRequestFocusEnabled(false)
    panel.setOpaque(false)
    checkBox.setEnabled(tree.isEnabled())
    checkBox.setFont(tree.getFont())
    checkBox.setFocusable(false)
    checkBox.setOpaque(false)
    (treeNode.getUserObject() as? CheckBoxNode)?.also {
      checkBox.setIcon(if (it.status === Status.INDETERMINATE) IndeterminateIcon() else null)
      l.setText(it.label)
      checkBox.setSelected(it.status === Status.SELECTED)
    }
    panel.add(checkBox, BorderLayout.WEST)
    panel.add(l)
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
      setOpaque(false)
      setFocusable(false)
      handler = ActionListener { stopCellEditing() }
      addActionListener(handler)
    }
  }
  private val renderer = DefaultTreeCellRenderer()
  private var str: String? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    val l = renderer.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true) as JLabel
    l.setFont(tree.getFont())

    val treeNode = value as? DefaultMutableTreeNode ?: return l
    panel.setFocusable(false)
    panel.setRequestFocusEnabled(false)
    panel.setOpaque(false)
    checkBox.setEnabled(tree.isEnabled())
    checkBox.setFont(tree.getFont())
    // checkBox.setFocusable(false)
    // checkBox.setOpaque(false)
    (treeNode.getUserObject() as? CheckBoxNode)?.also {
      if (it.status === Status.INDETERMINATE) {
        checkBox.setIcon(IndeterminateIcon())
      } else {
        checkBox.setIcon(null)
      }
      l.setText(it.label)
      checkBox.setSelected(it.status === Status.SELECTED)
      str = it.label
    }
    panel.add(checkBox, BorderLayout.WEST)
    panel.add(l)
    return panel
  }

  override fun getCellEditorValue() =
    CheckBoxNode(str ?: "", if (checkBox.isSelected()) Status.SELECTED else Status.DESELECTED)

  override fun isCellEditable(e: EventObject): Boolean {
    if (e is MouseEvent && e.getSource() is JTree) {
      val p = e.getPoint()
      val tree = e.getSource() as JTree
      val path = tree.getPathForLocation(p.x, p.y)
      return tree.getPathBounds(path)?.let { r ->
        r.width = checkBox.getPreferredSize().width
        r.contains(p)
      } ?: false
    }
    return false
  }
}
