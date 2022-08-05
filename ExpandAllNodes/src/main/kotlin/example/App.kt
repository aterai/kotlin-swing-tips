package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

// Expanding or Collapsing All Nodes in a JTree Component (Java Developers Almanac Example)
// http://www.exampledepot.com/egs/javax.swing.tree/ExpandAll.html
fun visitAll(tree: JTree, parent: TreePath, expand: Boolean) {
  (parent.lastPathComponent as? TreeNode)?.also { node ->
    node.children()
      .toList()
      .filterIsInstance<TreeNode>()
      .forEach { visitAll(tree, parent.pathByAddingChild(it), expand) }
  }
  if (expand) {
    tree.expandPath(parent)
  } else {
    tree.collapsePath(parent)
  }
}

// Expand or collapse a JTree - Real's Java How-to
// https://www.rgagnon.com/javadetails/java-0210.html
fun expandAll(tree: JTree) {
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row)
    row++
  }
}

fun collapseAll(tree: JTree) {
  var row = tree.rowCount - 1
  while (row >= 0) {
    tree.collapseRow(row)
    row--
  }
}

fun makeUI(): Component {
  val tree = JTree()

  val button1 = JButton("expand A")
  button1.addActionListener { expandAll(tree) }

  val button2 = JButton("collapse A")
  button2.addActionListener { collapseAll(tree) }

  val button3 = JButton("expand B")
  button3.addActionListener {
    (tree.model.root as? TreeNode)?.also {
      visitAll(tree, TreePath(it), true)
    }
  }

  val button4 = JButton("collapse B")
  button4.addActionListener {
    (tree.model.root as? TreeNode)?.also {
      visitAll(tree, TreePath(it), false)
    }
  }

  val p = JPanel(GridLayout(0, 1, 2, 2))
  listOf(button1, button2, button3, button4).forEach { p.add(it) }

  val panel = JPanel(BorderLayout())
  panel.add(p, BorderLayout.NORTH)

  return JPanel(BorderLayout()).also {
    it.add(panel, BorderLayout.EAST)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
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
