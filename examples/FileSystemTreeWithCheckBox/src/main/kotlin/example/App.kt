package example

import java.awt.*
import java.awt.event.MouseEvent
import java.io.File
import java.util.EnumSet
import java.util.EventObject
import javax.swing.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.event.TreeSelectionEvent
import javax.swing.event.TreeSelectionListener
import javax.swing.filechooser.FileSystemView
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

fun createUI(): Component {
  val fileSystemView = FileSystemView.getFileSystemView()
  val root = DefaultMutableTreeNode()
  val treeModel = DefaultTreeModel(root)
  fileSystemView.roots.forEach { fileSystemRoot ->
    val userObject = CheckBoxNode(fileSystemRoot, Status.DESELECTED)
    val node = DefaultMutableTreeNode(userObject)
    root.add(node)
    fileSystemView
      .getFiles(fileSystemRoot, true)
      .filter { it.isDirectory }
      .map { CheckBoxNode(it, Status.DESELECTED) }
      .map { DefaultMutableTreeNode(it) }
      .forEach { node.add(it) }
  }
  treeModel.addTreeModelListener(CheckBoxStatusUpdateListener())

  val tree = object : JTree(treeModel) {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      setCellRenderer(FileTreeCellRenderer(fileSystemView))
      setCellEditor(CheckBoxNodeEditor(fileSystemView))
    }
  }
  tree.isRootVisible = false
  tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))

  tree.isEditable = true
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

  tree.expandRow(0)

  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class TriStateCheckBox : JCheckBox() {
  override fun updateUI() {
    val currentIcon = icon
    icon = null
    super.updateUI()
    currentIcon?.also {
      icon = IndeterminateIcon()
    }
    isOpaque = false
  }
}

private class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.paint = FOREGROUND
    g2.fillRect(
      MARGIN,
      (iconHeight - HEIGHT) / 2,
      iconWidth - MARGIN - MARGIN,
      HEIGHT,
    )
    g2.dispose()
  }

  override fun getIconWidth() = icon.iconWidth

  override fun getIconHeight() = icon.iconHeight

  companion object {
    private val FOREGROUND = Color(0xC8_32_14_FF.toInt(), true)
    private const val MARGIN = 4
    private const val HEIGHT = 2
  }
}

private enum class Status {
  SELECTED,
  DESELECTED,
  INDETERMINATE,
}

private data class CheckBoxNode(
  val file: File,
  val status: Status,
) {
  override fun toString() = file.name ?: ""
}

private class FileTreeCellRenderer(
  private val fileSystemView: FileSystemView,
) : TreeCellRenderer {
  private val checkBox = TriStateCheckBox().also { it.isOpaque = false }
  private val tcr = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.isFocusable = false
    it.isRequestFocusEnabled = false
    it.isOpaque = false
    it.add(checkBox, BorderLayout.WEST)
  }

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
    c.font = tree.font
    val uo = (value as? DefaultMutableTreeNode)?.userObject
    if (c is JLabel && uo is CheckBoxNode) {
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      checkBox.icon = if (uo.status == Status.INDETERMINATE) {
        IndeterminateIcon()
      } else {
        null
      }
      val file = uo.file
      c.icon = fileSystemView.getSystemIcon(file)
      c.text = fileSystemView.getSystemDisplayName(file)
      c.toolTipText = file.path
      checkBox.isSelected = uo.status == Status.SELECTED
      panel.add(c)
      return panel
    }
    return c
  }
}

private class CheckBoxNodeEditor(
  private val fileSystemView: FileSystemView,
) : AbstractCellEditor(),
  TreeCellEditor {
  private val checkBox = TriStateCheckBox().also {
    it.isOpaque = false
    it.isFocusable = false
    it.addActionListener { stopCellEditing() }
  }
  private val tcr = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.isFocusable = false
    it.isRequestFocusEnabled = false
    it.isOpaque = false
    it.add(checkBox, BorderLayout.WEST)
  }
  private var file: File? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
  ): Component {
    val c = tcr.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      false,
    )
    c.font = tree.font
    val uo = (value as? DefaultMutableTreeNode)?.userObject
    if (c is JLabel && uo is CheckBoxNode) {
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      checkBox.icon = if (uo.status == Status.INDETERMINATE) {
        IndeterminateIcon()
      } else {
        null
      }
      file = uo.file
      c.icon = fileSystemView.getSystemIcon(file)
      c.text = fileSystemView.getSystemDisplayName(file)
      checkBox.isSelected = uo.status == Status.SELECTED
      panel.add(c)
      return panel
    }
    return c
  }

  override fun getCellEditorValue(): Any {
    val f = file ?: File("")
    val s = if (checkBox.isSelected) Status.SELECTED else Status.DESELECTED
    return CheckBoxNode(f, s)
  }

  override fun isCellEditable(e: EventObject?): Boolean {
    val tree = e?.source
    if (e is MouseEvent && tree is JTree) {
      val p = e.point
      val path = tree.getPathForLocation(p.x, p.y)
      return tree.getPathBounds(path)?.let {
        it.width = checkBox.preferredSize.width
        it.contains(p)
      } ?: false
    }
    return false
  }
}

private class FolderSelectionListener(
  val fileSystemView: FileSystemView,
) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    // The event is also fired when the path is removed from the selection,
    // so load the subdirectories only for a newly selected path
    val node = e.path.lastPathComponent
    if (!e.isAddedPath || node !is DefaultMutableTreeNode || !node.isLeaf) {
      return
    }
    val userObject = node.userObject
    val m = (e.source as? JTree)?.model
    if (m is DefaultTreeModel && userObject is CheckBoxNode && userObject.file.isDirectory) {
      // A leaf node is never INDETERMINATE, so the child nodes inherit its check status
      val status = if (userObject.status == Status.SELECTED) {
        Status.SELECTED
      } else {
        Status.DESELECTED
      }
      val worker = object : BackgroundTask(fileSystemView, userObject.file) {
        override fun process(chunks: List<File>) {
          chunks
            .map { CheckBoxNode(it, status) }
            .map { DefaultMutableTreeNode(it) }
            .forEach { m.insertNodeInto(it, node, node.childCount) }
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
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    fileSystemView
      .getFiles(parent, true)
      .filter { it.isDirectory }
      .forEach { this.publish(it) }
    return "done"
  }
}

private class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
    // To indicate the root has changed, childIndices and children will be null.
    // The root of this sample has no CheckBoxNode, so there is nothing to update.
    val children = e.children
    val model = e.source as? DefaultTreeModel
    if (adjusting || children == null || model == null) {
      return
    }
    adjusting = true
    try {
      // Update the status of all descendant nodes to be the same as the changed node status
      children
        .filterIsInstance<DefaultMutableTreeNode>()
        .forEach { node ->
          (node.userObject as? CheckBoxNode)?.also {
            updateAllChildrenUserObject(node, it.status)
            model.nodeChanged(node)
          }
        }
      // Update the status of the ancestor nodes from the nearest parent up to the root
      generateSequence(e.treePath) { it.parentPath }
        .mapNotNull { it.lastPathComponent as? DefaultMutableTreeNode }
        .forEach { parent ->
          updateParentUserObject(parent)
          model.nodeChanged(parent)
        }
    } finally {
      // Always reset the flag, otherwise this listener stops working after an exception
      adjusting = false
    }
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    (parent.userObject as? CheckBoxNode)?.also { node ->
      val set = parent
        .children()
        .toList()
        .filterIsInstance<DefaultMutableTreeNode>()
        .mapNotNull { (it.userObject as? CheckBoxNode)?.status }
        .toCollection(EnumSet.noneOf(Status::class.java))
      // SELECTED or DESELECTED only if all children have that same status
      val status = set.singleOrNull() ?: Status.INDETERMINATE
      parent.userObject = CheckBoxNode(node.file, status)
    }
  }

  private fun updateAllChildrenUserObject(
    parent: DefaultMutableTreeNode,
    status: Status,
  ) {
    parent
      .breadthFirstEnumeration()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it != parent }
      .forEach {
        (it.userObject as? CheckBoxNode)?.also { check ->
          it.userObject = CheckBoxNode(check.file, status)
        }
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) {
    // not needed
  }

  override fun treeNodesRemoved(e: TreeModelEvent) {
    // not needed
  }

  override fun treeStructureChanged(e: TreeModelEvent) {
    // not needed
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
