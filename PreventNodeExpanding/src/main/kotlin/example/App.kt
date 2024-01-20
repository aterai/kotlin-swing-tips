package example

import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.event.TreeWillExpandListener
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreeCellRenderer

fun makeUI(): Component {
  val fileSystemView = FileSystemView.getFileSystemView()
  val root = DefaultMutableTreeNode()
  val treeModel = DefaultTreeModel(root)
  fileSystemView.roots
    .forEach { fileSystemRoot ->
      val node = DefaultMutableTreeNode(fileSystemRoot)
      root.add(node)
      fileSystemView.getFiles(fileSystemRoot, true)
        .filter { it.isDirectory }
        .map { DefaultMutableTreeNode(it) }
        .forEach { node.add(it) }
    }
  val tree = JTree(treeModel)
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  tree.isRootVisible = false
  tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))
  tree.cellRenderer = FileTreeCellRenderer(tree.cellRenderer, fileSystemView)
  tree.expandRow(0)
  // tree.setToggleClickCount(1)
  tree.addTreeWillExpandListener(DirectoryExpandVetoListener())

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class DirectoryExpandVetoListener : TreeWillExpandListener {
  @Throws(ExpandVetoException::class)
  override fun treeWillExpand(e: TreeExpansionEvent) {
    val path = e.path
    val o = path.lastPathComponent
    if (o is DefaultMutableTreeNode) {
      (o.userObject as? File)?.also { file ->
        val name = file.name
        if (name.isNotEmpty() && name.codePointAt(0) == '.'.code) {
          throw ExpandVetoException(e, "Tree expansion cancelled")
        }
      }
    }
  }

  override fun treeWillCollapse(e: TreeExpansionEvent) {
    // not use
  }
}

private class FolderSelectionListener(
  private val fileSystemView: FileSystemView,
) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val tree = e.source as? JTree
    val model = tree?.model as? DefaultTreeModel
    val node = e.path.lastPathComponent as? DefaultMutableTreeNode
    val parent = node?.userObject as? File
    if (model == null || parent?.isDirectory != true || !node.isLeaf) {
      return
    }

    object : BackgroundTask(fileSystemView, parent) {
      override fun process(chunks: List<File?>?) {
        if (tree.isDisplayable && !isCancelled) {
          chunks?.map { DefaultMutableTreeNode(it) }
            ?.forEach { model.insertNodeInto(it, node, node.childCount) }
        } else {
          cancel(true)
        }
      }
    }.execute()
  }
}

private open class BackgroundTask(
  private val fileSystemView: FileSystemView,
  private val parent: File,
) : SwingWorker<String, File?>() {
  public override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory }
      .forEach { publish(it) }
    return "done"
  }
}

private class FileTreeCellRenderer(
  private val tcr: TreeCellRenderer,
  private val fileSystemView: FileSystemView,
) : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = tcr.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    if (c is JLabel && value is DefaultMutableTreeNode) {
      if (selected) {
        c.isOpaque = false
        c.setForeground(getTextSelectionColor())
      } else {
        c.isOpaque = true
        c.setForeground(getTextNonSelectionColor())
        c.setBackground(getBackgroundNonSelectionColor())
      }
      (value.userObject as? File)?.also {
        c.icon = fileSystemView.getSystemIcon(it)
        c.text = fileSystemView.getSystemDisplayName(it)
        c.toolTipText = it.path
        c.setEnabled(!it.name.startsWith("."))
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
