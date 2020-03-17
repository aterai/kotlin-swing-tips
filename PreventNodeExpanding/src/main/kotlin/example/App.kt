package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports
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

class MainPanel : JPanel(BorderLayout()) {
  init {
    val fileSystemView = FileSystemView.getFileSystemView()
    val root = DefaultMutableTreeNode()
    val treeModel = DefaultTreeModel(root)
    fileSystemView.getRoots()
      .forEach { fileSystemRoot ->
        val node = DefaultMutableTreeNode(fileSystemRoot)
        root.add(node)
        fileSystemView.getFiles(fileSystemRoot, true)
          .filter { it.isDirectory() }
          .map { DefaultMutableTreeNode(it) }
          .forEach { node.add(it) }
      }
    val tree = JTree(treeModel)
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
    tree.setRootVisible(false)
    tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))
    tree.setCellRenderer(FileTreeCellRenderer(tree.getCellRenderer(), fileSystemView))
    tree.expandRow(0)
    // tree.setToggleClickCount(1)
    tree.addTreeWillExpandListener(DirectoryExpandVetoListener())
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class DirectoryExpandVetoListener : TreeWillExpandListener {
  @Throws(ExpandVetoException::class)
  override fun treeWillExpand(e: TreeExpansionEvent) {
    val path = e.getPath()
    val o = path.getLastPathComponent()
    if (o is DefaultMutableTreeNode) {
      (o.getUserObject() as? File)?.also { file ->
        val name = file.getName()
        if (name.isNotEmpty() && name.codePointAt(0) == '.'.toInt()) {
          throw ExpandVetoException(e, "Tree expansion cancelled")
        }
      }
    }
  }

  override fun treeWillCollapse(e: TreeExpansionEvent) {
    /* not use */
  }
}

class FolderSelectionListener(private val fileSystemView: FileSystemView) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val node = e.getPath().getLastPathComponent() as? DefaultMutableTreeNode
    if (node == null || !node.isLeaf()) {
      return
    }
    val parent = node.getUserObject() as? File
    if (parent == null || !parent.isDirectory()) {
      return
    }
    val tree = e.getSource() as JTree
    val model = tree.getModel() as DefaultTreeModel

    object : BackgroundTask(fileSystemView, parent) {
      override fun process(chunks: List<File?>?) {
        if (isCancelled()) {
          return
        }
        if (!tree.isDisplayable()) {
          cancel(true)
          return
        }
        chunks
          ?.map { DefaultMutableTreeNode(it) }
          ?.forEach { model.insertNodeInto(it, node, node.childCount) }
      }
    }.execute()
  }
}

open class BackgroundTask(
  private val fileSystemView: FileSystemView,
  private val parent: File
) : SwingWorker<String, File?>() {
  public override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory() }
      .forEach { publish(it) }
    return "done"
  }
}

class FileTreeCellRenderer(
  private val renderer: TreeCellRenderer,
  private val fileSystemView: FileSystemView
) : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
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
          c.setIcon(fileSystemView.getSystemIcon(file))
          c.setText(fileSystemView.getSystemDisplayName(file))
          c.setToolTipText(file.getPath())
          c.setEnabled(!file.getName().startsWith("."))
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
