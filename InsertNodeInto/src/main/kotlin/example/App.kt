package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree1 = JTree()
  val tree2 = JTree()

  val p1 = JPanel(GridLayout(1, 2))
  p1.add(makeTitledScrollPane(tree1, "p.add(c) & m.reload(p)"))
  p1.add(makeTitledScrollPane(tree2, "m.insertNodeInto(c, p, p.size)"))

  val expandButton = JButton("expand all")
  expandButton.addActionListener {
    expandAll(tree1)
    expandAll(tree2)
  }

  val addButton = JButton("add")
  addButton.addActionListener {
    val date = LocalDateTime.now(ZoneId.systemDefault())

    val model1 = tree1.model as? DefaultTreeModel
    val parent1 = model1?.root as? DefaultMutableTreeNode
    val child1 = DefaultMutableTreeNode(date)
    parent1?.add(child1)
    model1?.reload(parent1)
    tree1.scrollPathToVisible(TreePath(child1.path))

    val model2 = tree2.model as? DefaultTreeModel
    val parent2 = model2?.root as? DefaultMutableTreeNode
    val child2 = DefaultMutableTreeNode(date)
    if (parent2 != null) {
      model2.insertNodeInto(child2, parent2, parent2.childCount)
      tree2.scrollPathToVisible(TreePath(child2.path))
    }
  }

  val p2 = JPanel(GridLayout(1, 2))
  p2.add(expandButton)
  p2.add(addButton)

  return JPanel(BorderLayout()).also {
    it.add(p1)
    it.add(p2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

fun expandAll(tree: JTree) {
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row)
    row++
  }
}

fun makeTitledScrollPane(view: Component, title: String) = JScrollPane(view).also {
  it.border = BorderFactory.createTitledBorder(title)
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
