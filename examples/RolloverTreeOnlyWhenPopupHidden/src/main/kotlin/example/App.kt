package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import javax.swing.*
import javax.swing.event.PopupMenuEvent
import javax.swing.event.PopupMenuListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileSystemView
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

fun createUI(): Component {
  val tree = FileSystemViewTree()
  tree.isRootVisible = false
  tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
  tree.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  // Add the listener only once, not in updateUI() that is called on every Look&Feel change
  tree.addTreeSelectionListener(
    FolderSelectionListener(FileSystemView.getFileSystemView()),
  )

  val listModel = DefaultListModel<File>()
  tree.addTreeSelectionListener { e ->
    (e.newLeadSelectionPath?.lastPathComponent as? DefaultMutableTreeNode)?.also {
      updateListFiles(it.userObject, listModel)
    }
  }

  UIManager.put("PopupMenu.consumeEventOnClose", false)
  val popup = JPopupMenu()
  popup.addPopupMenuListener(TreePopupMenuListener(tree))
  popup.add("JMenuItem1")
  popup.add("JMenuItem2")
  popup.add("JMenuItem3")
  tree.componentPopupMenu = popup

  val s1 = JScrollPane(tree)
  val s2 = JScrollPane(JList(listModel))
  val split = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, s1, s2)
  split.resizeWeight = .5
  return JPanel(BorderLayout()).also {
    it.add(split)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun updateListFiles(userObject: Any, model: DefaultListModel<File>) {
  if (userObject is File) {
    val files = userObject.listFiles()
    model.clear()
    if (files != null) {
      for (f in files) {
        model.addElement(f)
      }
    }
  }
}

private class FileSystemViewTree : JTree() {
  private var rolloverRowIndex = -1
  private var rolloverHandler: MouseAdapter? = null
  private var fileSystemView: FileSystemView? = null

  override fun paintComponent(g: Graphics) {
    // The tree is not opaque, so paint the background, the rollover row and
    // the selected rows across the full width even when nothing is selected
    g.color = background
    g.fillRect(0, 0, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    // getRowBounds(...) returns null if the row index is invalid
    getRowBounds(rolloverRowIndex)?.also {
      g2.paint = ROLLOVER_COLOR
      g2.fillRect(0, it.y, width, it.height)
    }
    g2.paint = SELECTED_COLOR
    selectionRows
      ?.asList()
      ?.mapNotNull { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.let { getPathBounds(it) }?.also { r ->
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    removeMouseListener(rolloverHandler)
    removeMouseMotionListener(rolloverHandler)
    // Must be set before the TreeUI is installed, since BasicTreeUI reads it in installDefaults()
    UIManager.put("Tree.repaintWholeRow", true)
    super.updateUI()
    setUI(WholeRowSelectTreeUI())
    fileSystemView = FileSystemView.getFileSystemView().also {
      EventQueue.invokeLater { model = createFileTreeModel(it) }
    }
    val renderer = DefaultTreeCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, _ ->
      val c = renderer.getTreeCellRendererComponent(
        tree,
        value,
        selected,
        expanded,
        leaf,
        row,
        false,
      )
      val rollover = row == rolloverRowIndex
      updateForeground(c, renderer.textSelectionColor, rollover)
      updateBackground(c, tree.background, selected, rollover)
      updateIcon(c, value, selected)
      c
    }
    isOpaque = false
    rolloverHandler = RolloverHandler()
    addMouseListener(rolloverHandler)
    addMouseMotionListener(rolloverHandler)
  }

  private fun updateIcon(
    c: Component,
    value: Any,
    selected: Boolean,
  ) {
    if (value is DefaultMutableTreeNode && c is JLabel) {
      c.isOpaque = !selected
      val file = value.userObject
      if (file is File) {
        c.icon = fileSystemView!!.getSystemIcon(file)
        c.text = fileSystemView!!.getSystemDisplayName(file)
        c.toolTipText = file.path
      }
    }
  }

  private fun updateForeground(
    c: Component,
    color: Color,
    rollover: Boolean,
  ) {
    if (rollover) {
      c.foreground = color
    }
  }

  private fun updateBackground(
    c: Component,
    color: Color,
    selected: Boolean,
    rollover: Boolean,
  ) {
    c.background = when {
      selected -> SELECTED_COLOR
      rollover -> ROLLOVER_COLOR
      else -> color
    }
    (c as? JComponent)?.isOpaque = true
  }

  fun updateRolloverIndex() {
    // Wait until the JPopupMenu is actually hidden, then update the rollover row
    // from the current mouse position (null if the cursor is outside the tree)
    EventQueue.invokeLater {
      val pt = mousePosition
      if (pt == null) {
        clearRollover()
      } else {
        updateRolloverIndex(pt)
      }
    }
  }

  fun updateRolloverIndex(pt: Point) {
    val row = getRowForLocation(pt.x, pt.y)
    val isPopupVisible = componentPopupMenu.isVisible
    if (rolloverRowIndex != row && !isPopupVisible) {
      rolloverRowIndex = row
      repaint()
    }
  }

  private fun clearRollover() {
    rolloverRowIndex = -1
    repaint()
  }

  private inner class RolloverHandler : MouseAdapter() {
    override fun mouseMoved(e: MouseEvent) {
      updateRolloverIndex(e.point)
    }

    override fun mouseEntered(e: MouseEvent) {
      updateRolloverIndex(e.point)
    }

    override fun mouseExited(e: MouseEvent) {
      // Opening the JPopupMenu fires mouseExited, so keep the rollover row while it is visible
      val isPopupVisible = componentPopupMenu.isVisible
      if (!isPopupVisible) {
        clearRollover()
      }
    }
  }

  private fun createFileTreeModel(fileSystemView: FileSystemView): DefaultTreeModel {
    val root = DefaultMutableTreeNode()
    val treeModel = DefaultTreeModel(root)
    fileSystemView.roots
      .forEach { fileSystemRoot ->
        val node = DefaultMutableTreeNode(fileSystemRoot)
        root.add(node)
        fileSystemView
          .getFiles(fileSystemRoot, true)
          .filter { it.isDirectory }
          .map { DefaultMutableTreeNode(it) }
          .forEach { node.add(it) }
      }
    return treeModel
  }

  companion object {
    private val SELECTED_COLOR = Color(0x0078D7)
    private val ROLLOVER_COLOR = Color(0x6496C8)
  }
}

private class WholeRowSelectTreeUI : BasicTreeUI() {
  override fun getPathBounds(
    tree: JTree,
    path: TreePath?,
  ) = getTreePathBounds(path, Rectangle())

  private fun getTreePathBounds(
    path: TreePath?,
    bounds: Rectangle,
  ): Rectangle? {
    val r = treeState?.getBounds(path, bounds)
    if (r != null) {
      r.width = tree.width
      r.y += tree.insets.top
    }
    return r
  }
}

private class TreePopupMenuListener(
  private val tree: FileSystemViewTree,
) : PopupMenuListener {
  override fun popupMenuWillBecomeVisible(e: PopupMenuEvent) {
    // not needed
  }

  override fun popupMenuWillBecomeInvisible(e: PopupMenuEvent) {
    tree.updateRolloverIndex()
  }

  override fun popupMenuCanceled(e: PopupMenuEvent) {
    // not needed: popupMenuWillBecomeInvisible is also fired when canceled
  }
}

private class FolderSelectionListener(
  private val fileSystemView: FileSystemView,
) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val tree = e.source as? JTree
    // e.getPath() may return a deselected path, so use the new lead selection path
    val node = e.newLeadSelectionPath?.lastPathComponent as? DefaultMutableTreeNode
    val model = tree?.model as? DefaultTreeModel ?: return
    val parent = node?.userObject as? File
    if (parent != null && node.isLeaf && parent.isDirectory) {
      val worker = object : BackgroundTask(fileSystemView, parent) {
        override fun process(chunks: List<File>) {
          if (tree.isDisplayable && !isCancelled) {
            chunks
              .map { DefaultMutableTreeNode(it) }
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

private open class BackgroundTask(
  private val fileSystemView: FileSystemView,
  private val parent: File,
) : SwingWorker<String, File>() {
  override fun doInBackground(): String {
    fileSystemView
      .getFiles(parent, true)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
