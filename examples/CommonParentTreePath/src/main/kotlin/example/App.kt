package example

import java.awt.*
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import kotlin.math.min

fun makeUI(): Component {
  val tree = JTree()
  val sm = tree.getSelectionModel()
  sm.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
  val popup = TreePopupMenu()
  tree.setComponentPopupMenu(popup)
  return JPanel(BorderLayout(2, 2)).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  private val field = object : JTextField(24) {
    private var listener: AncestorListener? = null

    override fun updateUI() {
      removeAncestorListener(listener)
      super.updateUI()
      listener = FocusAncestorListener()
      addAncestorListener(listener)
    }
  }
  private var path: TreePath? = null

  init {
    add("getCommonParent").addActionListener { showCommonParent() }
    addSeparator()
    add("add").addActionListener { addNode() }
    add("add & reload").addActionListener { addAndReload() }
    add("edit").addActionListener { edit() }
    addSeparator()
    add("remove").addActionListener { remove() }
  }

  private fun showCommonParent() {
    val tree = getInvoker() as? JTree
    tree
      ?.selectionPaths
      ?.takeIf { it.size > 1 }
      ?.let { findCommonParent(it) }
      ?.also {
        val node = it.lastPathComponent
        val title = "common parent"
        JOptionPane.showMessageDialog(
          tree,
          node,
          title,
          JOptionPane.INFORMATION_MESSAGE,
        )
      }
  }

  private fun addNode() {
    val tree = getInvoker() as? JTree
    val model = tree?.model as? DefaultTreeModel
    val parent = path?.lastPathComponent as? DefaultMutableTreeNode ?: return
    val child = DefaultMutableTreeNode("New node")
    model?.insertNodeInto(child, parent, parent.childCount)
    tree?.scrollPathToVisible(TreePath(child.path))
  }

  private fun addAndReload() {
    val tree = getInvoker() as? JTree
    val model = tree?.model as? DefaultTreeModel
    val parent = path?.lastPathComponent as? DefaultMutableTreeNode ?: return
    val child = DefaultMutableTreeNode("New node")
    parent.add(child)
    model?.reload(parent)
    tree?.scrollPathToVisible(TreePath(child.path))
  }

  private fun edit() {
    val node = path?.lastPathComponent
    if (node !is DefaultMutableTreeNode) {
      return
    }
    field.text = node.getUserObject().toString()
    val tree = getInvoker() as? JTree
    val ret = JOptionPane.showConfirmDialog(
      tree,
      field,
      "edit",
      JOptionPane.YES_NO_OPTION,
    )
    if (ret == JOptionPane.OK_OPTION) {
      tree?.model?.valueForPathChanged(path, field.getText())
    }
  }

  private fun remove() {
    val node = path?.lastPathComponent as? DefaultMutableTreeNode
    if (node?.isRoot != true) {
      val tree = getInvoker() as? JTree
      val model = tree?.model as? DefaultTreeModel
      model?.removeNodeFromParent(node)
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    if (c is JTree) {
      path = c.getPathForLocation(x, y)
      super.show(c, x, y)
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent?) {
    // not needed
  }

  override fun ancestorRemoved(e: AncestorEvent?) {
    // not needed
  }
}

fun findCommonParent(paths: Array<TreePath>) = paths
  .map { it.getPath() }
  .reduce { n1, n2 -> getCommonPath(n1, n2) }
  .let { TreePath(it) }

fun <T> getCommonPath(node1: Array<T>, node2: Array<T>): Array<T?> {
  val min = min(node1.size, node2.size)
  for (len in min downTo 1) {
    val a1 = node1.copyOf(len)
    val a2 = node2.copyOf(len)
    if (a1.contentDeepEquals(a2)) {
      return a1
    }
  }
  return node1.copyOf(1)
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
