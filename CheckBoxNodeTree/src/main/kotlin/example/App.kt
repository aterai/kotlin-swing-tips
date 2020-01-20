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

class MainPanel : JPanel(BorderLayout()) {
  init {
    val tree = object : JTree() {
      override fun updateUI() {
        setCellRenderer(null)
        setCellEditor(null)
        super.updateUI()
        setCellRenderer(CheckBoxNodeRenderer())
        setCellEditor(CheckBoxNodeEditor())
      }
    }
    (tree.getModel().getRoot() as? DefaultMutableTreeNode)?.also { root ->
      root.breadthFirstEnumeration().toList()
        .filterIsInstance<DefaultMutableTreeNode>()
        .filter { it.isLeaf() }
        .forEach {
          val isEven = it.getParent().getIndex(it) % 2 == 0
          it.setUserObject(CheckBoxNode(it.getUserObject().toString(), isEven))
        }
    }
    tree.setEditable(true)
    tree.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4))
    for (i in 0 until tree.getRowCount()) {
      tree.expandRow(i)
    }
    setBorder(BorderFactory.createTitledBorder("JCheckBoxes as JTree Leaf Nodes"))
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class CheckBoxNodeRenderer : TreeCellRenderer {
  private val checkBox = JCheckBox()
  private val renderer = DefaultTreeCellRenderer()
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
      checkBox.setEnabled(tree.isEnabled())
      checkBox.setFont(tree.getFont())
      checkBox.setOpaque(false)
      checkBox.setFocusable(false)
      (value.getUserObject() as? CheckBoxNode)?.also {
        checkBox.setText(it.text)
        checkBox.setSelected(it.selected)
      }
      return checkBox
    }
    return renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
  }
}

class CheckBoxNodeEditor : AbstractCellEditor(), TreeCellEditor {
  private val checkBox = object : JCheckBox() {
    @Transient
    private var handler: ActionListener? = null

    override fun updateUI() {
      removeActionListener(handler)
      super.updateUI()
      setOpaque(false)
      setFocusable(false)
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
      checkBox.setSelected((value.getUserObject() as? CheckBoxNode)?.selected == true)
      checkBox.setText(value.toString())
    }
    return checkBox
  }

  override fun getCellEditorValue() = CheckBoxNode(checkBox.text, checkBox.isSelected)

  override fun isCellEditable(e: EventObject) = (e as? MouseEvent)
    ?.let { it.getComponent() as? JTree }?.getPathForLocation(e.x, e.y)
    ?.let { it.getLastPathComponent() as? TreeNode }?.isLeaf() == true
}

data class CheckBoxNode(val text: String, val selected: Boolean) {
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
