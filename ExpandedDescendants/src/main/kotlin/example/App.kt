package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Enumeration
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

@Transient var expandedState: Enumeration<TreePath>? = null

fun makeUI(): Component {
  val root = makeTreeRoot()
  val tree = JTree(DefaultTreeModel(root))
  val rootPath = TreePath(root)

  val save = JButton("Save")
  save.addActionListener {
    expandedState = tree.getExpandedDescendants(rootPath)
  }

  val load = JButton("Load")
  load.addActionListener {
    visitAll(tree, rootPath, false)
    if (expandedState != null) {
      expandedState?.toList()?.forEach { tree.expandPath(it) }
      expandedState = tree.getExpandedDescendants(rootPath)
    }
  }

  val expand = JButton("Expand")
  expand.addActionListener {
    visitAll(tree, rootPath, true)
  }

  val collapse = JButton("Collapse")
  collapse.addActionListener {
    visitAll(tree, rootPath, false)
  }

  val box = JPanel(GridLayout(1, 4))
  box.add(save)
  box.add(load)
  box.add(expand)
  box.add(collapse)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun visitAll(tree: JTree, parent: TreePath, expand: Boolean) {
  (parent.lastPathComponent as? TreeNode)?.also { node ->
    if (!node.isLeaf) {
      node.children().toList().forEach {
        visitAll(tree, parent.pathByAddingChild(it), expand)
      }
    }
  }
  if (expand) {
    tree.expandPath(parent)
  } else if (tree.isRootVisible || parent.parentPath != null) {
    tree.collapsePath(parent)
  }
}

private fun makeTreeRoot(): DefaultMutableTreeNode {
  val set4 = DefaultMutableTreeNode("Set 004")
  set4.add(DefaultMutableTreeNode("22222222222"))
  set4.add(DefaultMutableTreeNode("eee eee eee eee"))
  set4.add(DefaultMutableTreeNode("bbb bbb bbb"))
  set4.add(DefaultMutableTreeNode("zzz zz zz"))

  val set1 = DefaultMutableTreeNode("Set 001")
  set1.add(DefaultMutableTreeNode("3333333333333333"))
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(set4)
  set1.add(DefaultMutableTreeNode("222222"))
  set1.add(DefaultMutableTreeNode("222222222"))

  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("eee eee eee ee ee"))
  set2.add(DefaultMutableTreeNode("bbb bbb"))

  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("zzz zz zz"))
  set3.add(DefaultMutableTreeNode("aaa aaa aaa aaa"))
  set3.add(DefaultMutableTreeNode("ccc ccc ccc"))

  val root = DefaultMutableTreeNode("Root")
  root.add(DefaultMutableTreeNode("xxx xxx xxx xx xx"))
  root.add(set3)
  root.add(DefaultMutableTreeNode("eee eee eee ee ee"))
  root.add(set1)
  root.add(set2)
  root.add(DefaultMutableTreeNode("222222222222"))
  root.add(DefaultMutableTreeNode("bbb bbb bbb bbb"))
  return root
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
