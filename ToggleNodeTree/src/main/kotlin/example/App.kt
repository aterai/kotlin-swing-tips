package example

import java.awt.*
import java.util.Enumeration
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  init {
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
        EventQueue.invokeLater({ setViewportBorder(BorderFactory.createLineBorder(getViewport().getView().getBackground(), 5)) })
      }
    }
    add(scroll)
    setPreferredSize(Dimension(320, 240))
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
    val model = tree.getModel()
    val root = model.getRoot() as DefaultMutableTreeNode

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
      val node = e.nextElement() as DefaultMutableTreeNode
      val isOverFirstLevel = node.getLevel() > 1
      if (isOverFirstLevel) { // Collapse only nodes in the first hierarchy
        return
      } else if (node.isLeaf() || node.isRoot()) {
        continue
      }
      tree.collapsePath(TreePath(node.getPath()))
    }
  }
}

fun main() {
  EventQueue.invokeLater(object : Runnable {
    override fun run() {
      try {
        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
      } catch (ex: ClassNotFoundException) {
        ex.printStackTrace()
      } catch (ex: InstantiationException) {
        ex.printStackTrace()
      } catch (ex: IllegalAccessException) {
        ex.printStackTrace()
      } catch (ex: UnsupportedLookAndFeelException) {
        ex.printStackTrace()
      }
      JFrame().apply {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
        getContentPane().add(MainPanel())
        pack()
        setLocationRelativeTo(null)
        setVisible(true)
      }
    }
  })
}
