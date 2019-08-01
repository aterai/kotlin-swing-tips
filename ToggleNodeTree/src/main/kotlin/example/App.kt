package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree(makeModel())
  tree.setRootVisible(false)
  tree.addTreeWillExpandListener(object : TreeWillExpandListener {
    private var isAdjusting: Boolean = false
    @Throws(ExpandVetoException::class)
    override fun treeWillExpand(e: TreeExpansionEvent) {
      // collapseAll(tree); // StackOverflowError when collapsing nodes below 2nd level
      if (isAdjusting) {
        return
      }
      isAdjusting = true
      collapseFirstHierarchy(tree)
      tree.setSelectionPath(e.getPath())
      isAdjusting = false
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(e: TreeExpansionEvent) {
      // throw new ExpandVetoException(e, "Tree collapse cancelled");
    }
  })

  val scroll = object : JScrollPane(tree) {
    override fun updateUI() {
      setViewportBorder(null)
      super.updateUI()
      EventQueue.invokeLater {
        val bgc = getViewport().getView().getBackground()
        setViewportBorder(BorderFactory.createLineBorder(bgc, 5))
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.setPreferredSize(Dimension(320, 240))
  }
}

fun makeModel(): DefaultTreeModel {
  val set1 = DefaultMutableTreeNode("Set 001")
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(DefaultMutableTreeNode("33333"))

  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("asdfasdfas"))
  set2.add(DefaultMutableTreeNode("asdf"))

  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("asdfasdfasdf"))
  set3.add(DefaultMutableTreeNode("qwerqwer"))
  set3.add(DefaultMutableTreeNode("zvxcvzxcvzxzxcvzxcv"))

  val set4 = DefaultMutableTreeNode("Set 004")
  set4.add(DefaultMutableTreeNode("444"))

  val root = DefaultMutableTreeNode("Root")
  root.add(set1)
  root.add(set2)
  set2.add(set3)
  root.add(set4)
  return DefaultTreeModel(root)
}

fun collapseFirstHierarchy(tree: JTree) {
  val root = tree.getModel().getRoot() as? DefaultMutableTreeNode ?: return

  // // Java 9:
  // Collections.list(root.breadthFirstEnumeration()).stream()
  //     .filter(DefaultMutableTreeNode.class::isInstance)
  //     .map(DefaultMutableTreeNode.class::cast)
  //     .takeWhile(node -> node.getLevel() <= 1)
  //     .dropWhile(DefaultMutableTreeNode::isRoot)
  //     .dropWhile(DefaultMutableTreeNode::isLeaf)
  //     .map(DefaultMutableTreeNode::getPath)
  //     .map(TreePath::new)
  //     .forEach(tree::collapsePath);
  // Java 9: Enumeration<TreeNode> e = root.breadthFirstEnumeration();
  val e = root.breadthFirstEnumeration()
  while (e.hasMoreElements()) {
    val node = e.nextElement() as? DefaultMutableTreeNode ?: continue
    val isOverFirstLevel = node.getLevel() > 1
    if (isOverFirstLevel) { // Collapse only nodes in the first hierarchy
      return
    } else if (node.isLeaf() || node.isRoot()) {
      continue
    }
    tree.collapsePath(TreePath(node.getPath()))
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
