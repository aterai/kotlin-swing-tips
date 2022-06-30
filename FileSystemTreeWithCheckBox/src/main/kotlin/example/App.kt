package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.io.File
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
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

fun makeUI(): Component {
  val fileSystemView = FileSystemView.getFileSystemView()
  val root = DefaultMutableTreeNode()
  val treeModel = DefaultTreeModel(root)
  fileSystemView.roots.forEach { fileSystemRoot ->
    val node = DefaultMutableTreeNode(CheckBoxNode(fileSystemRoot, Status.DESELECTED))
    root.add(node)
    fileSystemView.getFiles(fileSystemRoot, true)
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

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.paint = FOREGROUND
    g2.fillRect(MARGIN, (iconHeight - HEIGHT) / 2, iconWidth - MARGIN - MARGIN, HEIGHT)
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
  SELECTED, DESELECTED, INDETERMINATE
}

private data class CheckBoxNode(val file: File, val status: Status) {
  override fun toString() = file.name ?: ""
}

private class FileTreeCellRenderer(
  private val fileSystemView: FileSystemView
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
    hasFocus: Boolean
  ): Component {
    val c = tcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val l = c as? JLabel ?: return c
    l.font = tree.font
    return if (value is DefaultMutableTreeNode) {
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      (value.userObject as? CheckBoxNode)?.also {
        checkBox.icon = if (it.status == Status.INDETERMINATE) IndeterminateIcon() else null
        val file = it.file
        l.icon = fileSystemView.getSystemIcon(file)
        l.text = fileSystemView.getSystemDisplayName(file)
        l.toolTipText = file.path
        checkBox.isSelected = it.status == Status.SELECTED
      }
      panel.add(l)
      panel
    } else l
  }
}

private class CheckBoxNodeEditor(
  private val fileSystemView: FileSystemView
) : AbstractCellEditor(), TreeCellEditor {
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
    row: Int
  ): Component {
    val c = tcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false)
    c.font = tree.font
    return if (value is DefaultMutableTreeNode && c is JLabel) {
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      (value.userObject as? CheckBoxNode)?.also {
        checkBox.icon = if (it.status == Status.INDETERMINATE) IndeterminateIcon() else null
        file = it.file
        c.icon = fileSystemView.getSystemIcon(file)
        c.text = fileSystemView.getSystemDisplayName(file)
        checkBox.isSelected = it.status == Status.SELECTED
      }
      panel.add(c)
      panel
    } else c
  }

  override fun getCellEditorValue(): Any {
    val f = file ?: File("")
    return CheckBoxNode(f, if (checkBox.isSelected) Status.SELECTED else Status.DESELECTED)
  }

  override fun isCellEditable(e: EventObject): Boolean {
    val tree = e.source
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
  val fileSystemView: FileSystemView
) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val node = e.path.lastPathComponent
    if (node !is DefaultMutableTreeNode || !node.isLeaf) {
      return
    }
    val check = node.userObject
    val model = (e.source as? JTree)?.model
    if (model !is DefaultTreeModel || check !is CheckBoxNode || !check.file.isDirectory) {
      return
    }
    val parentStatus = if (check.status == Status.SELECTED) Status.SELECTED else Status.DESELECTED
    val worker = object : BackgroundTask(fileSystemView, check.file) {
      override fun process(chunks: List<File>) {
        chunks.map { CheckBoxNode(it, parentStatus) }
          .map { DefaultMutableTreeNode(it) }
          .forEach { model.insertNodeInto(it, node, node.childCount) }
      }
    }
    worker.execute()
  }
}

private open class BackgroundTask(
  private val fileSystemView: FileSystemView,
  private val parent: File
) : SwingWorker<String, File>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory }
      .forEach { this.publish(it) }
    return "done"
  }
}

private class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    if (adjusting) {
      return
    }
    adjusting = true
    val model = e.source as? DefaultTreeModel ?: return
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
    // To indicate the root has changed, childIndices and children will be null.
    val children = e.children
    val isRoot = children == null

    // If the parent node exists, update its status
    if (!isRoot) {
      val parent = e.treePath
      var n = parent.lastPathComponent as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.parent as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    }

    // Update the status of all child nodes to be the same as the current node status
    val isOnlyOneNodeSelected = children != null && children.size == 1
    val current = if (isOnlyOneNodeSelected) children[0] else model.root
    if (current is DefaultMutableTreeNode) {
      val status = (current.userObject as? CheckBoxNode)?.status ?: Status.INDETERMINATE
      updateAllChildrenUserObject(current, status)
      model.nodeChanged(current)
    }
    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .mapNotNull { (it.userObject as? CheckBoxNode)?.status }

    (parent.userObject as? CheckBoxNode)?.also { node ->
      val status = when {
        list.all { it === Status.DESELECTED } -> Status.DESELECTED
        list.all { it === Status.SELECTED } -> Status.SELECTED
        else -> Status.INDETERMINATE
      }
      parent.userObject = CheckBoxNode(node.file, status)
    }
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, status: Status) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it != parent }
      .forEach {
        (it.userObject as? CheckBoxNode)?.also { check ->
          it.userObject = CheckBoxNode(check.file, status)
        }
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) {
    /* not needed */
  }

  override fun treeNodesRemoved(e: TreeModelEvent) {
    /* not needed */
  }

  override fun treeStructureChanged(e: TreeModelEvent) {
    /* not needed */
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
