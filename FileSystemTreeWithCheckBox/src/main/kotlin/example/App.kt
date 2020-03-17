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

class MainPanel : JPanel(BorderLayout()) {
  init {
    val fileSystemView = FileSystemView.getFileSystemView()
    val root = DefaultMutableTreeNode()
    val treeModel = DefaultTreeModel(root)
    fileSystemView.getRoots().forEach { fileSystemRoot ->
      val node = DefaultMutableTreeNode(CheckBoxNode(fileSystemRoot, Status.DESELECTED))
      root.add(node)
      fileSystemView.getFiles(fileSystemRoot, true)
        .filter { it.isDirectory() }
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
    tree.setRootVisible(false)
    tree.addTreeSelectionListener(FolderSelectionListener(fileSystemView))

    tree.setEditable(true)
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))

    tree.expandRow(0)

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

open class TriStateCheckBox : JCheckBox() {
  override fun updateUI() {
    val currentIcon = getIcon()
    setIcon(null)
    super.updateUI()
    currentIcon?.also {
      setIcon(IndeterminateIcon())
    }
    setOpaque(false)
  }
}

class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    icon.paintIcon(c, g2, 0, 0)
    g2.setPaint(FOREGROUND)
    g2.fillRect(SIDE_MARGIN, (getIconHeight() - HEIGHT) / 2, getIconWidth() - SIDE_MARGIN - SIDE_MARGIN, HEIGHT)
    g2.dispose()
  }

  override fun getIconWidth() = icon.getIconWidth()

  override fun getIconHeight() = icon.getIconHeight()

  companion object {
    private val FOREGROUND = Color(0xC8_32_14_FF.toInt(), true)
    private const val SIDE_MARGIN = 4
    private const val HEIGHT = 2
  }
}

enum class Status {
  SELECTED, DESELECTED, INDETERMINATE
}

data class CheckBoxNode(val file: File, val status: Status) {
  override fun toString() = file.getName() ?: ""
}

class FileTreeCellRenderer(private val fileSystemView: FileSystemView) : TreeCellRenderer {
  private val checkBox = TriStateCheckBox().also { it.setOpaque(false) }
  private val renderer = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.setFocusable(false)
    it.setRequestFocusEnabled(false)
    it.setOpaque(false)
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
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val l = c as? JLabel ?: return c
    l.setFont(tree.getFont())
    return if (value is DefaultMutableTreeNode) {
      checkBox.setEnabled(tree.isEnabled())
      checkBox.setFont(tree.getFont())
      (value.getUserObject() as? CheckBoxNode)?.also {
        checkBox.setIcon(if (it.status == Status.INDETERMINATE) IndeterminateIcon() else null)
        val file = it.file
        l.setIcon(fileSystemView.getSystemIcon(file))
        l.setText(fileSystemView.getSystemDisplayName(file))
        l.setToolTipText(file.getPath())
        checkBox.setSelected(it.status == Status.SELECTED)
      }
      panel.add(l)
      panel
    } else l
  }
}

class CheckBoxNodeEditor(private val fileSystemView: FileSystemView) : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = TriStateCheckBox().also {
    it.setOpaque(false)
    it.setFocusable(false)
    it.addActionListener { stopCellEditing() }
  }
  private val renderer = DefaultTreeCellRenderer()
  private val panel = JPanel(BorderLayout()).also {
    it.setFocusable(false)
    it.setRequestFocusEnabled(false)
    it.setOpaque(false)
    it.add(checkBox, BorderLayout.WEST)
  }
  private var file: File? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false)
    val l = c as? JLabel ?: return c
    l.setFont(tree.getFont())
    return if (value is DefaultMutableTreeNode) {
      checkBox.setEnabled(tree.isEnabled())
      checkBox.setFont(tree.getFont())
      (value.getUserObject() as? CheckBoxNode)?.also {
        checkBox.setIcon(if (it.status == Status.INDETERMINATE) IndeterminateIcon() else null)
        file = it.file
        l.setIcon(fileSystemView.getSystemIcon(file))
        l.setText(fileSystemView.getSystemDisplayName(file))
        checkBox.setSelected(it.status == Status.SELECTED)
      }
      panel.add(l)
      panel
    } else l
  }

  override fun getCellEditorValue(): Any {
    val f = file ?: File("")
    return CheckBoxNode(f, if (checkBox.isSelected()) Status.SELECTED else Status.DESELECTED)
  }

  override fun isCellEditable(e: EventObject): Boolean {
    val tree = e.getSource()
    if (e is MouseEvent && tree is JTree) {
      val p = e.getPoint()
      val path = tree.getPathForLocation(p.x, p.y)
      return tree.getPathBounds(path)?.let {
        it.width = checkBox.getPreferredSize().width
        it.contains(p)
      } ?: false
    }
    return false
  }
}

class FolderSelectionListener(val fileSystemView: FileSystemView) : TreeSelectionListener {
  override fun valueChanged(e: TreeSelectionEvent) {
    val node = e.getPath().getLastPathComponent()
    if (node !is DefaultMutableTreeNode || !node.isLeaf()) {
      return
    }
    val check = node.getUserObject()
    val model = (e.getSource() as? JTree)?.getModel()
    if (model !is DefaultTreeModel || check !is CheckBoxNode || !check.file.isDirectory()) {
      return
    }
    val parentStatus = if (check.status == Status.SELECTED) Status.SELECTED else Status.DESELECTED
    val worker = object : BackgroundTask(fileSystemView, check.file) {
      override fun process(chunks: List<File>) {
        chunks.map { CheckBoxNode(it, parentStatus) }
          .map { DefaultMutableTreeNode(it) }
          .forEach { model.insertNodeInto(it, node, node.getChildCount()) }
      }
    }
    worker.execute()
  }
}

open class BackgroundTask(
  private val fileSystemView: FileSystemView,
  private val parent: File
) : SwingWorker<String, File>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    fileSystemView.getFiles(parent, true)
      .filter { it.isDirectory() }
      .forEach { this.publish(it) }
    return "done"
  }
}

class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    if (adjusting) {
      return
    }
    adjusting = true
    val model = e.getSource() as? DefaultTreeModel ?: return
    // https://docs.oracle.com/javase/8/docs/api/javax/swing/event/TreeModelListener.html#treeNodesChanged-javax.swing.event.TreeModelEvent-
    // To indicate the root has changed, childIndices and children will be null.
    val children = e.getChildren()
    val isRoot = children == null

    // If the parent node exists, update its status
    if (!isRoot) {
      val parent = e.getTreePath()
      var n = parent.getLastPathComponent() as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.getParent() as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    }

    // Update the status of all child nodes to be the same as the current node status
    val isOnlyOneNodeSelected = children != null && children.size == 1
    val current = if (isOnlyOneNodeSelected) children[0] else model.getRoot()
    if (current is DefaultMutableTreeNode) {
      val status = (current.getUserObject() as? CheckBoxNode)?.status ?: Status.INDETERMINATE
      updateAllChildrenUserObject(current, status)
      model.nodeChanged(current)
    }
    adjusting = false
  }

  private fun updateParentUserObject(parent: DefaultMutableTreeNode) {
    val list = parent.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .mapNotNull { (it.getUserObject() as? CheckBoxNode)?.status }

    (parent.getUserObject() as? CheckBoxNode)?.also { node ->
      val status = when {
        list.all { it === Status.DESELECTED } -> Status.DESELECTED
        list.all { it === Status.SELECTED } -> Status.SELECTED
        else -> Status.INDETERMINATE
      }
      parent.setUserObject(CheckBoxNode(node.file, status))
    }
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, status: Status) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it != parent }
      .forEach {
        (it.getUserObject() as? CheckBoxNode)?.also { check ->
          it.setUserObject(CheckBoxNode(check.file, status))
        }
      }
  }

  override fun treeNodesInserted(e: TreeModelEvent) { /* not needed */ }

  override fun treeNodesRemoved(e: TreeModelEvent) { /* not needed */ }

  override fun treeStructureChanged(e: TreeModelEvent) { /* not needed */ }
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
