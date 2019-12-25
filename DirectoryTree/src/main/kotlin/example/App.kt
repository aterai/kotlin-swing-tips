// -*- mode:java; encoding:utf-8 -*-
// vim:set fileencoding=utf-8:
// @homepage@
package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.io.File
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel

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
    val tree: JTree = object : JTree(treeModel) {
      override fun updateUI() {
        setCellRenderer(null)
        super.updateUI()
        val r = DefaultTreeCellRenderer()
        setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
          val c = r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
          if (c is JLabel) {
            if (selected) {
              c.setOpaque(false)
              c.setForeground(r.getTextSelectionColor())
            } else {
              c.setOpaque(true)
              c.setForeground(r.getTextNonSelectionColor())
              c.setBackground(r.getBackgroundNonSelectionColor())
            }
            if (value is DefaultMutableTreeNode) {
              (value.getUserObject() as? File)?.also {
                c.setIcon(fileSystemView.getSystemIcon(it))
                c.setText(fileSystemView.getSystemDisplayName(it))
                c.setToolTipText(it.getPath())
              }
            }
          }
          c
        }
      }
    }
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
    tree.setRootVisible(false)
    tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))
    tree.expandRow(0)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class FolderSelectionListener(private val fileSystemView: FileSystemView) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val node = e.getPath().getLastPathComponent()
    if (node !is DefaultMutableTreeNode || !node.isLeaf()) {
      return
    }
    val parent = node.getUserObject()
    if (parent !is File || !parent.isDirectory()) {
      return
    }
    val tree = e.getSource() as JTree
    val model = tree.getModel() as DefaultTreeModel
    val worker = object : BackgroundTask(fileSystemView, parent) {
      override fun process(chunks: List<File>) {
        if (isCancelled()) {
          return
        }
        if (!tree.isDisplayable()) {
          cancel(true)
          return
        }
        chunks.map { DefaultMutableTreeNode(it) }
          .forEach { model.insertNodeInto(it, node, node.getChildCount()) }
      }
    }
    worker.execute()
  }
}

open class BackgroundTask constructor(
  private val fileSystemView: FileSystemView,
  private val parent: File
) : SwingWorker<String, File>() {
  public override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory() }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
