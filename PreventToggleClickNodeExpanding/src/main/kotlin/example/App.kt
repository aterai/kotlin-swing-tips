package example

import java.awt.*
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.metal.MetalTreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val dir = File(".")
  val root = DefaultMutableTreeNode(dir)
  val treeModel = DefaultTreeModel(root)
  createChildren(dir, root)

  val tree1 = JTree(treeModel)
  tree1.addTreeWillExpandListener(FileExpandVetoListener())

  val tree2 = JTree(treeModel)
  tree2.ui = object : MetalTreeUI() {
    override fun isToggleEvent(e: MouseEvent): Boolean {
      val file = getFileFromTreePath(tree.selectionPath)
      return file == null && super.isToggleEvent(e)
    }
  }

  return JPanel(GridLayout(1, 2, 4, 4)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(initTree(tree1)))
    it.add(JScrollPane(initTree(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun initTree(tree: JTree): JTree {
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  tree.cellRenderer = FileTreeCellRenderer()
  // val ml = object : MouseAdapter() {
  //   override fun mouseClicked(e: MouseEvent) {
  //     if (e.clickCount == 2) {
  //       val file = getFileFromTreePath(tree.selectionPath)
  //       println(file)
  //     }
  //   }
  // }
  // tree.addMouseListener(ml)
  // tree.setToggleClickCount(0)
  tree.expandRow(0)
  return tree
}

private fun createChildren(
  parent: File,
  node: DefaultMutableTreeNode,
) {
  parent.listFiles()?.forEach { file ->
    val child = DefaultMutableTreeNode(file)
    node.add(child)
    if (file.isDirectory) {
      createChildren(file, child)
    } else if (file.name == "App.kt") {
      child.add(DefaultMutableTreeNode("FileExpandVetoListener()"))
      child.add(DefaultMutableTreeNode("FileTreeCellRenderer()"))
      child.add(DefaultMutableTreeNode("MainPanel()"))
      child.add(DefaultMutableTreeNode("main()"))
    }
  }
}

private fun getFileFromTreePath(path: TreePath?): File? {
  val node = path?.lastPathComponent as? DefaultMutableTreeNode ?: return null
  return (node.userObject as? File)?.takeIf { it.isFile }
}

private class FileExpandVetoListener : TreeWillExpandListener {
  @Throws(ExpandVetoException::class)
  override fun treeWillExpand(e: TreeExpansionEvent) {
    val path = e.path
    val o = path.lastPathComponent
    if (o is DefaultMutableTreeNode) {
      val file = o.userObject as? File
      if (file == null || file.isFile) {
        throw ExpandVetoException(e, "Tree expansion cancelled")
      }
    }
  }

  override fun treeWillCollapse(e: TreeExpansionEvent) { // throws ExpandVetoException {
    // throw ExpandVetoException(e, "Tree collapse cancelled")
  }
}

private class FileTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    if (c is JLabel) {
      if (selected) {
        c.isOpaque = false
        c.setForeground(getTextSelectionColor())
      } else {
        c.isOpaque = true
        c.setForeground(getTextNonSelectionColor())
        c.setBackground(getBackgroundNonSelectionColor())
      }
      (value as? DefaultMutableTreeNode)?.also {
        (it.userObject as? File)?.also { file ->
          val txt = runCatching {
            file.canonicalFile.name
          }.getOrElse { "error" }
          c.text = txt
        }
      }
    }
    return c
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
