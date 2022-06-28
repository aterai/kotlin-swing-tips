package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      setCellEditor(null)
      super.updateUI()
      setCellRenderer(CheckBoxNodeRenderer())
      setCellEditor(CheckBoxNodeEditor())
    }
  }
  (tree.model.root as? DefaultMutableTreeNode)?.also { root ->
    root.breadthFirstEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it.isLeaf }
      .forEach {
        val isEven = it.parent.getIndex(it) % 2 == 0
        it.userObject = CheckBoxNode(it.userObject.toString(), isEven)
      }
  }
  tree.isEditable = true
  tree.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  for (i in 0 until tree.rowCount) {
    tree.expandRow(i)
  }
  return JPanel(BorderLayout()).also {
    it.border = BorderFactory.createTitledBorder("JCheckBoxes as JTree Leaf Nodes")
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
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
    if (leaf && value is DefaultMutableTreeNode) {
      checkBox.isEnabled = tree.isEnabled
      checkBox.font = tree.font
      checkBox.isOpaque = false
      checkBox.isFocusable = false
      (value.userObject as? CheckBoxNode)?.also {
        checkBox.text = it.text
        checkBox.isSelected = it.selected
      }
      return checkBox
    }
    return tcr.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
  }
}

private class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = object : JCheckBox() {
    @Transient
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
    if (leaf && value is DefaultMutableTreeNode) {
      checkBox.isSelected = (value.userObject as? CheckBoxNode)?.selected == true
      checkBox.text = value.toString()
    }
    return checkBox
  }

  override fun getCellEditorValue() = CheckBoxNode(checkBox.text, checkBox.isSelected)

  override fun isCellEditable(e: EventObject) = (e as? MouseEvent)
    ?.let { it.component as? JTree }?.getPathForLocation(e.x, e.y)
    ?.let { it.lastPathComponent as? TreeNode }?.isLeaf == true
}

private data class CheckBoxNode(val text: String, val selected: Boolean) {
  override fun toString() = text
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
