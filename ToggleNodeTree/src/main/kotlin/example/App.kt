package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree(makeModel())
  tree.isRootVisible = false
  tree.addTreeWillExpandListener(object : TreeWillExpandListener {
    private var isAdjusting = false
    // @Throws(ExpandVetoException::class)
    override fun treeWillExpand(e: TreeExpansionEvent) {
      // collapseAll(tree) // StackOverflowError when collapsing nodes below 2nd level
      if (isAdjusting) {
        return
      }
      isAdjusting = true
      collapseFirstHierarchy(tree)
      tree.selectionPath = e.path
      isAdjusting = false
    }

    // @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(e: TreeExpansionEvent) {
      // throw ExpandVetoException(e, "Tree collapse cancelled")
    }
  })

  val scroll = object : JScrollPane(tree) {
    override fun updateUI() {
      viewportBorder = null
      super.updateUI()
      EventQueue.invokeLater {
        val bgc = getViewport().view.background
        viewportBorder = BorderFactory.createLineBorder(bgc, 5)
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeModel(): DefaultTreeModel {
  val set1 = DefaultMutableTreeNode("Set 001")
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(DefaultMutableTreeNode("33333"))

  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("4444444444444"))
  set2.add(DefaultMutableTreeNode("5555555"))

  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("6666666666"))
  set3.add(DefaultMutableTreeNode("77777777"))
  set3.add(DefaultMutableTreeNode("888888888888888"))

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
  val root = tree.model.root as? DefaultMutableTreeNode ?: return
  root.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .takeWhile { it.level <= 1 }
      .dropWhile { it.isRoot || it.isLeaf }
      .map { TreePath(it.path) }
      .forEach { tree.collapsePath(it) }
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
