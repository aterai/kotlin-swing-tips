package example

import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeModelEvent
import javax.swing.event.TreeModelListener
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer

fun makeUI(): Component {
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      isEditable = true
      isRootVisible = false
      setShowsRootHandles(false)
      setCellRenderer(CheckBoxNodeRenderer())
      setCellEditor(CheckBoxNodeEditor())
    }
  }
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row++)
  }
  tree.model.addTreeModelListener(CheckBoxStatusUpdateListener())
  val handler = object : TreeWillExpandListener {
    @Throws(ExpandVetoException::class)
    override fun treeWillExpand(e: TreeExpansionEvent) {
      throw ExpandVetoException(e, "Tree expansion cancelled")
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(e: TreeExpansionEvent) {
      throw ExpandVetoException(e, "Tree collapse cancelled")
    }
  }
  tree.addTreeWillExpandListener(handler)

  val verticalBox = Box.createVerticalBox()
  val map = mutableMapOf<String, Component>()
  val model = tree.model
  (model.root as? DefaultMutableTreeNode)?.preorderEnumeration()?.toList()
    ?.filterIsInstance<DefaultMutableTreeNode>()
    ?.forEach {
      val title = it.userObject?.toString() ?: ""
      it.userObject = CheckBoxNode(title, false, !it.isLeaf)
      if (!it.isRoot) {
        val c = JCheckBox(title, false)
        map[title] = c
        if (!it.isLeaf) {
          verticalBox.add(Box.createVerticalStrut(5))
          c.addActionListener { e ->
            val selected = (e.source as? JCheckBox)?.isSelected == true
            it.children().toList()
              .filterIsInstance<DefaultMutableTreeNode>()
              .forEach { child ->
                val cn = child.userObject as? CheckBoxNode
                map[cn?.text]?.isEnabled = selected
              }
          }
        }
        c.isEnabled = !it.isLeaf
        val box = Box.createHorizontalBox()
        box.add(Box.createHorizontalStrut((it.level - 1) * 16))
        box.alignmentX = Component.LEFT_ALIGNMENT
        box.add(c)
        verticalBox.add(box)
      }
    }

  val p = JPanel(BorderLayout())
  p.add(verticalBox, BorderLayout.NORTH)

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("Box", JScrollPane(p)))
    it.add(makeTitledPanel("JTree", JScrollPane(tree)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(c)
}

private data class CheckBoxNode(val text: String, val selected: Boolean, val enabled: Boolean) {
  override fun toString() = text
}

private class CheckBoxNodeRenderer : TreeCellRenderer {
  private val checkBox = JCheckBox()
  private val tcr = DefaultTreeCellRenderer()
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    checkBox.text = value?.toString() ?: ""
    if (value is DefaultMutableTreeNode) {
      checkBox.isOpaque = false
      (value.userObject as? CheckBoxNode)?.also { node ->
        checkBox.text = node.text
        checkBox.isSelected = node.selected
        checkBox.isEnabled = node.enabled
      }
      return checkBox
    }
    return tcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
  }
}

private class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = object : JCheckBox() {
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

  override fun getTreeCellEditorComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int
  ): Component {
    if (value is DefaultMutableTreeNode) {
      val node = value.userObject
      if (node is CheckBoxNode) {
        checkBox.isEnabled = node.enabled
        checkBox.isSelected = node.selected
      } else {
        checkBox.isSelected = false
      }
      checkBox.text = value.toString()
    }
    return checkBox
  }

  override fun getCellEditorValue() =
    CheckBoxNode(checkBox.text, checkBox.isSelected, checkBox.isEnabled)

  override fun isCellEditable(e: EventObject?) = e is MouseEvent
}

private class CheckBoxStatusUpdateListener : TreeModelListener {
  private var adjusting = false
  override fun treeNodesChanged(e: TreeModelEvent) {
    val model = e.source as? DefaultTreeModel
    if (model == null || adjusting) {
      return
    }
    adjusting = true
    val children = e.children
    val isOnlyOneNodeSelected = children?.size == 1
    val current = if (isOnlyOneNodeSelected) children[0] else model.root
    if (current is DefaultMutableTreeNode) {
      val selected = (current.userObject as? CheckBoxNode)?.selected == true
      e.children.filterIsInstance<DefaultMutableTreeNode>().forEach {
        updateAllChildrenUserObject(it, selected)
      }
      model.nodeChanged(e.treePath.lastPathComponent as? DefaultMutableTreeNode)
    }
    adjusting = false
  }

  private fun updateAllChildrenUserObject(parent: DefaultMutableTreeNode, enabled: Boolean) {
    parent.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { parent != it }
      .forEach { node ->
        (node.userObject as? CheckBoxNode)?.also {
          node.userObject = CheckBoxNode(it.text, it.selected, enabled)
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
