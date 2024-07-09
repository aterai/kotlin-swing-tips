package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.beans.DefaultPersistenceDelegate
import java.beans.XMLDecoder
import java.beans.XMLEncoder
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.EventObject
import javax.swing.*
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

private val textArea = JTextArea()

private fun makeUI(): Component {
  val tree = makeTree()

  val save = JButton("save")
  save.addActionListener {
    runCatching {
      val file = File.createTempFile("output", ".xml")
      val d = DefaultPersistenceDelegate(arrayOf("label", "status"))
      XMLEncoder(BufferedOutputStream(Files.newOutputStream(file.toPath()))).use { xe ->
        xe.setPersistenceDelegate(CheckBoxNode::class.java, d)
        xe.writeObject(tree.model)
      }
      Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8).use {
        textArea.read(it, "temp")
      }
    }.onFailure {
      textArea.text = it.message
    }
  }

  val load = JButton("load")
  load.addActionListener {
    val text = textArea.text
    if (text.isNotEmpty()) {
      val bytes = text.toByteArray(StandardCharsets.UTF_8)
      XMLDecoder(BufferedInputStream(ByteArrayInputStream(bytes))).use { xd ->
        (xd.readObject() as? DefaultTreeModel)?.also {
          it.addTreeModelListener(CheckBoxStatusUpdateListener())
          tree.model = it
        }
      }
    }
  }

  val box = Box.createHorizontalBox().also {
    it.add(Box.createHorizontalGlue())
    it.add(save)
    it.add(Box.createHorizontalStrut(4))
    it.add(load)
  }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT).also {
    it.resizeWeight = .5
    it.topComponent = JScrollPane(tree)
    it.bottomComponent = JScrollPane(textArea)
  }

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTree(): JTree {
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      setCellRenderer(CheckBoxNodeRenderer())
      setCellEditor(CheckBoxNodeEditor())
    }
  }
  tree.isEditable = true
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)

  val model = tree.model
  (tree.model.root as? DefaultMutableTreeNode)?.also { root ->
    root
      .breadthFirstEnumeration()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach {
        it.userObject = CheckBoxNode(it.userObject?.toString() ?: "", Status.DESELECTED)
      }
  }
  model.addTreeModelListener(CheckBoxStatusUpdateListener())
  tree.expandRow(0)
  return tree
}

private open class TriStateCheckBox : JCheckBox() {
  override fun updateUI() {
    val currentIcon = icon
    icon = null
    super.updateUI()
    if (currentIcon != null) {
      icon = IndeterminateIcon()
    }
    isOpaque = false
  }
}

private class IndeterminateIcon : Icon {
  private val icon = UIManager.getIcon("CheckBox.icon")

  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
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

enum class Status {
  SELECTED,
  DESELECTED,
  INDETERMINATE,
}

private data class CheckBoxNode(
  val label: String,
  val status: Status,
) {
  override fun toString() = label
}

private class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false

  override fun treeNodesChanged(e: TreeModelEvent) {
    val children = e.children
    val model = e.source
    if (adjusting || model !is DefaultTreeModel) {
      return
    }
    adjusting = true
    val node: DefaultMutableTreeNode?
    val c: CheckBoxNode?
    val isNotRoot = children != null && children.size == 1
    if (isNotRoot) {
      node = children[0] as? DefaultMutableTreeNode
      c = node?.userObject as? CheckBoxNode
      val parent = e.treePath
      var n = parent.lastPathComponent as? DefaultMutableTreeNode
      while (n != null) {
        updateParentUserObject(n)
        n = n.parent as? DefaultMutableTreeNode ?: break
      }
      model.nodeChanged(n)
    } else {
      node = model.root as? DefaultMutableTreeNode
      c = node?.userObject as? CheckBoxNode
    }
    updateAllChildrenUserObject(node, c?.status)
    model.nodeChanged(node)
    adjusting = false
  }

  private fun updateParentUserObject(pn: DefaultMutableTreeNode) {
    val list = pn
      .children()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .map { it.userObject }
      .filterIsInstance<CheckBoxNode>()
      .map { it.status }
    (pn.userObject as? CheckBoxNode)?.label?.also { l ->
      when {
        list.all { it == Status.DESELECTED } ->
          pn.userObject = CheckBoxNode(l, Status.DESELECTED)

        list.all { it == Status.SELECTED } ->
          pn.userObject = CheckBoxNode(l, Status.SELECTED)

        else ->
          pn.userObject = CheckBoxNode(l, Status.INDETERMINATE)
      }
    }
  }

  private fun updateAllChildrenUserObject(
    parent: DefaultMutableTreeNode?,
    status: Status?,
  ) {
    if (parent == null || status == null) {
      return
    }
    parent
      .breadthFirstEnumeration()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { parent != it }
      .forEach {
        it.userObject = CheckBoxNode((it.userObject as? CheckBoxNode)?.label ?: "", status)
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

private class CheckBoxNodeRenderer : TreeCellRenderer {
  private val panel = JPanel(BorderLayout())
  private val checkBox = TriStateCheckBox()
  private val tcr = DefaultTreeCellRenderer()

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
    if (value is DefaultMutableTreeNode) {
      panel.isFocusable = false
      panel.isRequestFocusEnabled = false
      panel.isOpaque = false
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      checkBox.isFocusable = false
      checkBox.isOpaque = false
      val userObject = value.userObject
      if (userObject is CheckBoxNode) {
        if (userObject.status == Status.INDETERMINATE) {
          checkBox.icon = IndeterminateIcon()
        } else {
          checkBox.icon = null
        }
        (c as? JLabel)?.text = userObject.label
        checkBox.isSelected = userObject.status == Status.SELECTED
      }
      panel.add(checkBox, BorderLayout.WEST)
      panel.add(c)
      return panel
    }
    return c
  }
}

private class CheckBoxNodeEditor :
  AbstractCellEditor(),
  TreeCellEditor {
  private val panel = JPanel(BorderLayout())
  private val checkBox = object : TriStateCheckBox() {
    private var handler: ActionListener? = null

    override fun updateUI() {
      removeActionListener(handler)
      super.updateUI()
      isOpaque = false
      isFocusable = false
      handler = ActionListener { stopCellEditing() }
      addActionListener(handler)
    }
  }
  private val tcr = DefaultTreeCellRenderer()
  private var str: String? = null

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
  ): Component {
    val c = tcr.getTreeCellRendererComponent(tree, value, true, expanded, leaf, row, true)
    c.font = tree.font
    if (value is DefaultMutableTreeNode) {
      panel.isFocusable = false
      panel.isRequestFocusEnabled = false
      panel.isOpaque = false
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      val userObject = value.userObject
      if (userObject is CheckBoxNode) {
        if (userObject.status == Status.INDETERMINATE) {
          checkBox.icon = IndeterminateIcon()
        } else {
          checkBox.icon = null
        }
        (c as? JLabel)?.text = userObject.label
        checkBox.isSelected = userObject.status == Status.SELECTED
        str = userObject.label
      }
      panel.add(checkBox, BorderLayout.WEST)
      panel.add(c)
      return panel
    }
    return c
  }

  override fun getCellEditorValue() =
    CheckBoxNode(str ?: "", if (checkBox.isSelected) Status.SELECTED else Status.DESELECTED)

  override fun isCellEditable(e: EventObject?) =
    ((e as? MouseEvent)?.component as? JTree)?.let { pathContainsPoint(it, e.point) } ?: false

  private fun pathContainsPoint(tree: JTree, p: Point) =
    tree.getPathBounds(tree.getPathForLocation(p.x, p.y))?.let {
      it.width = checkBox.preferredSize.width
      it.contains(p)
    } ?: false
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
