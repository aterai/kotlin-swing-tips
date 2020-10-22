package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private val field = JTextField("foo")
private val tree = JTree()

fun makeUI(): Component {
  val dl = object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun removeUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun changedUpdate(e: DocumentEvent) {
      /* not needed */
    }
  }
  field.document.addDocumentListener(dl)

  val n = JPanel(BorderLayout())
  n.add(field)
  n.border = BorderFactory.createTitledBorder("Tree filter")
  tree.rowHeight = -1

  val model = tree.model
  val root = model.root
  if (root is DefaultMutableTreeNode) {
    root.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach {
        it.userObject = FilterableNode(it.userObject.toString())
      }
  }
  model.addTreeModelListener(FilterableStatusUpdateListener())
  tree.cellRenderer = FilterTreeCellRenderer()
  fireDocumentChangeEvent()

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(n, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun fireDocumentChangeEvent() {
  val q = field.text
  val rtp = tree.getPathForRow(0)
  if (q.isEmpty()) {
    resetAll(rtp, true)
    (tree.model as? DefaultTreeModel)?.reload()
  } else {
    visitAll(tree, rtp, false)
    searchTree(tree, rtp, q)
  }
}

fun searchTree(tree: JTree, path: TreePath, q: String) {
  val node = path.lastPathComponent as? DefaultMutableTreeNode
  val uo = node?.userObject as? FilterableNode
  if (uo != null) {
    uo.status = node.toString().startsWith(q)
    (tree.model as? DefaultTreeModel)?.nodeChanged(node)
    if (uo.status) {
      tree.expandPath(if (node.isLeaf) path.parentPath else path)
    }
    if (!uo.status && !node.isLeaf) {
      node.children().toList()
        .filterIsInstance<TreeNode>()
        .forEach {
          searchTree(tree, path.pathByAddingChild(it), q)
        }
    }
  }
}

fun resetAll(parent: TreePath, match: Boolean) {
  val node = parent.lastPathComponent as? DefaultMutableTreeNode ?: return
  (node.userObject as? FilterableNode)?.status = match
  if (!node.isLeaf) {
    node.children().toList()
      .forEach { resetAll(parent.pathByAddingChild(it), match) }
  }
}

fun visitAll(tree: JTree, parent: TreePath, expand: Boolean) {
  val node = parent.lastPathComponent as? TreeNode ?: return
  if (!node.isLeaf) {
    node.children().toList()
      .forEach { visitAll(tree, parent.pathByAddingChild(it), expand) }
  }
  if (expand) {
    tree.expandPath(parent)
  } else {
    tree.collapsePath(parent)
  }
}

@Suppress("DataClassShouldBeImmutable")
private data class FilterableNode(val label: String) {
  var status = false

  override fun toString() = label
}

private class FilterableStatusUpdateListener : TreeModelListener {
  private var adjusting = false
  override fun treeNodesChanged(e: TreeModelEvent) {
    val model = e.source
    if (adjusting || model !is DefaultTreeModel) {
      return
    }
    adjusting = true
    val children = e.children
    val node: DefaultMutableTreeNode?
    val c: FilterableNode?
    if (children != null && children.size == 1) {
      node = children[0] as? DefaultMutableTreeNode
      c = node?.userObject as? FilterableNode
      val parent = e.treePath
      var n = parent.lastPathComponent as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.parent as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    } else {
      node = model.root as? DefaultMutableTreeNode
      c = node?.userObject as? FilterableNode
    }
    updateAllChildrenUserObject(node, c?.status)
    model.nodeChanged(node)
    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val uo = parent.userObject as? FilterableNode ?: return
    val children = parent.children()
    while (children.hasMoreElements()) {
      val node = children.nextElement() as? DefaultMutableTreeNode
      val check = node?.userObject as? FilterableNode
      if (check?.status == true) {
        uo.status = true
        return
      }
    }
    uo.status = false
  }

  private fun updateAllChildrenUserObject(root: DefaultMutableTreeNode?, match: Boolean?) {
    val breadth = root?.breadthFirstEnumeration() ?: return
    while (breadth.hasMoreElements()) {
      val node = breadth.nextElement() as? DefaultMutableTreeNode
      if (root == node) {
        continue
      }
      (node?.userObject as? FilterableNode)?.status = match ?: false
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

private class FilterTreeCellRenderer : DefaultTreeCellRenderer() {
  private val emptyLabel = JLabel()
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val node = value as? DefaultMutableTreeNode
    return if ((node?.userObject as? FilterableNode)?.status == true) c else emptyLabel
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
