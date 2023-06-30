package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree = JTree()
  for (i in 0 until tree.rowCount) {
    tree.expandRow(i)
  }

  val emptyIcon = EmptyIcon()
  (tree.cellRenderer as? DefaultTreeCellRenderer)?.also {
    it.openIcon = emptyIcon
    it.closedIcon = emptyIcon
    it.leafIcon = emptyIcon
  }
  allNodesChanged(tree)

  val folderCheck = JCheckBox("OpenIcon, ClosedIcon")
  folderCheck.addActionListener { e ->
    (tree.cellRenderer as? DefaultTreeCellRenderer)?.also {
      if ((e.source as? JCheckBox)?.isSelected == true) {
        it.openIcon = it.defaultOpenIcon
        it.closedIcon = it.defaultClosedIcon
      } else {
        it.openIcon = emptyIcon
        it.closedIcon = emptyIcon
      }
      allNodesChanged(tree)
    }
  }

  val leafCheck = JCheckBox("LeafIcon")
  leafCheck.addActionListener { e ->
    (tree.cellRenderer as? DefaultTreeCellRenderer)?.also {
      if ((e.source as? JCheckBox)?.isSelected == true) {
        it.leafIcon = it.defaultLeafIcon
      } else {
        it.leafIcon = emptyIcon
      }
      allNodesChanged(tree)
    }
  }

  val np = JPanel()
  np.add(folderCheck)
  np.add(leafCheck)

  return JPanel(BorderLayout()).also {
    it.add(np, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun allNodesChanged(tree: JTree) {
  val model = tree.model as? DefaultTreeModel
  val root = model?.root as? DefaultMutableTreeNode ?: return
  root.preorderEnumeration().toList()
    .filterIsInstance<TreeNode>()
    .forEach { model.nodeChanged(it) }
}

private class EmptyIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    // Empty icon
  }

  override fun getIconWidth() = 2

  override fun getIconHeight() = 0
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
