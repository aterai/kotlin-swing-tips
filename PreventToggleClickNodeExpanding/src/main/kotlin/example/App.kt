package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.metal.MetalTreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath

class MainPanel : JPanel(GridLayout(1, 2, 4, 4)) {
  init {
    val dir = File(".")
    val root = DefaultMutableTreeNode(dir)
    val treeModel = DefaultTreeModel(root)
    createChildren(dir, root)

    val tree1 = JTree(treeModel)
    tree1.addTreeWillExpandListener(FileExpandVetoListener())

    val tree2 = JTree(treeModel)
    tree2.setUI(object : MetalTreeUI() {
      override fun isToggleEvent(e: MouseEvent): Boolean {
        val file = getFileFromTreePath(tree.getSelectionPath())
        return file == null && super.isToggleEvent(e)
      }
    })

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(initTree(tree1)))
    add(JScrollPane(initTree(tree2)))
    setPreferredSize(Dimension(320, 240))
  }

  private fun initTree(tree: JTree): JTree {
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
    tree.setCellRenderer(FileTreeCellRenderer())
    tree.addMouseListener(object : MouseAdapter() {
      override fun mouseClicked(e: MouseEvent) {
        if (e.getClickCount() == 2) {
          val file = getFileFromTreePath(tree.getSelectionPath())
          println(file)
        }
      }
    })
    // tree.setToggleClickCount(0)
    tree.expandRow(0)
    return tree
  }

  private fun createChildren(parent: File, node: DefaultMutableTreeNode) {
    parent.listFiles()?.forEach { file ->
      val child = DefaultMutableTreeNode(file)
      node.add(child)
      if (file.isDirectory()) {
        createChildren(file, child)
      } else if (file.getName() == "App.kt") {
        child.add(DefaultMutableTreeNode("FileExpandVetoListener()"))
        child.add(DefaultMutableTreeNode("FileTreeCellRenderer()"))
        child.add(DefaultMutableTreeNode("MainPanel()"))
        child.add(DefaultMutableTreeNode("main()"))
      }
    }
  }

  private fun getFileFromTreePath(path: TreePath?): File? {
    val node = path?.getLastPathComponent() as? DefaultMutableTreeNode ?: return null
    return (node.getUserObject() as? File)?.takeIf { it.isFile() }
  }
}

class FileExpandVetoListener : TreeWillExpandListener {
  @Throws(ExpandVetoException::class)
  override fun treeWillExpand(e: TreeExpansionEvent) {
    val path = e.getPath()
    val o = path.getLastPathComponent()
    if (o is DefaultMutableTreeNode) {
      val file = o.getUserObject() as? File
      if (file == null || file.isFile()) {
        throw ExpandVetoException(e, "Tree expansion cancelled")
      }
    }
  }

  override fun treeWillCollapse(e: TreeExpansionEvent) { // throws ExpandVetoException {
    // throw new ExpandVetoException(e, "Tree collapse cancelled")
  }
}

class FileTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    if (c is JLabel) {
      if (selected) {
        c.setOpaque(false)
        c.setForeground(getTextSelectionColor())
      } else {
        c.setOpaque(true)
        c.setForeground(getTextNonSelectionColor())
        c.setBackground(getBackgroundNonSelectionColor())
      }
      (value as? DefaultMutableTreeNode)?.also {
        (it.getUserObject() as? File)?.also { file ->
          val txt = runCatching {
            file.getCanonicalFile().getName()
          }.getOrElse { "error" }
          c.setText(txt)
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
