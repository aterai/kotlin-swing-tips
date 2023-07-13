package example

import java.awt.*
import java.io.File
import javax.swing.*
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

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

  val tree = object : JTree(treeModel) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val r = DefaultTreeCellRenderer()
      setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
        val c = r.getTreeCellRendererComponent(
          tree,
          value,
          selected,
          expanded,
          leaf,
          row,
          hasFocus
        )
        (c as? JLabel)?.also {
          if (selected) {
            it.isOpaque = false
            it.foreground = r.textSelectionColor
          } else {
            it.isOpaque = true
            it.foreground = r.textNonSelectionColor
            it.background = r.backgroundNonSelectionColor
          }
          ((value as? DefaultMutableTreeNode)?.userObject as? File)?.also { file ->
            it.icon = fileSystemView.getSystemIcon(file)
            it.text = fileSystemView.getSystemDisplayName(file)
            it.toolTipText = file.path
          }
        }
      }
    }
  }
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  tree.isRootVisible = false
  tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))
  tree.expandRow(0)

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class FolderSelectionListener(
  private val fileSystemView: FileSystemView
) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val tree = e.source as? JTree
    val node = e.path.lastPathComponent as? DefaultMutableTreeNode
    val model = tree?.model as? DefaultTreeModel
    val parent = node?.userObject as? File
    if (model != null && parent != null && node.isLeaf && parent.isDirectory) {
      val worker = object : BackgroundTask(fileSystemView, parent) {
        override fun process(chunks: List<File>) {
          if (tree.isDisplayable && !isCancelled) {
            chunks.map { DefaultMutableTreeNode(it) }
              .forEach { model.insertNodeInto(it, node, node.childCount) }
          } else {
            cancel(true)
          }
        }
      }
      worker.execute()
    }
  }
}

private open class BackgroundTask constructor(
  private val fileSystemView: FileSystemView,
  private val parent: File
) : SwingWorker<String, File>() {
  public override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory }
      .forEach { publish(it) }
    return "done"
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
