package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree1 = JTree()
  tree1.expandsSelectedPaths = false

  val tree2 = JTree()
  tree2.expandsSelectedPaths = true

  val p1 = JPanel(GridLayout(1, 2))
  p1.border = BorderFactory.createTitledBorder("setExpandsSelectedPaths")
  p1.add(makeTitledScrollPane(tree1, "false"))
  p1.add(makeTitledScrollPane(tree2, "true"))

  val textField = JTextField("soccer")
  val button = JButton("Select")
  button.addActionListener {
    val q = textField.text.trim()
    searchTree(tree1, tree1.getPathForRow(0), q)
    searchTree(tree2, tree2.getPathForRow(0), q)
  }

  val p2 = JPanel(BorderLayout())
  p2.add(textField)
  p2.add(button, BorderLayout.EAST)

  return JPanel(BorderLayout()).also {
    it.add(p1)
    it.add(p2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledScrollPane(
  view: Component,
  title: String,
) = JScrollPane(view).also {
  it.border = BorderFactory.createTitledBorder(title)
}

private fun searchTree(
  tree: JTree,
  path: TreePath,
  q: String,
) {
  (path.lastPathComponent as? TreeNode)?.also { node ->
    if (node.toString() == q) {
      tree.addSelectionPath(path)
    }
    if (!node.isLeaf) {
      node.children().toList().forEach { searchTree(tree, path.pathByAddingChild(it), q) }
    }
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
