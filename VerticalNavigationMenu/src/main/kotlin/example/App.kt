package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.*

class MainPanel : JPanel(BorderLayout(2, 2)) {
  init {
    val emptyIcon = EmptyIcon()
    UIManager.put("Tree.openIcon", emptyIcon)
    UIManager.put("Tree.closedIcon", emptyIcon)
    UIManager.put("Tree.leafIcon", emptyIcon)
    UIManager.put("Tree.expandedIcon", emptyIcon)
    UIManager.put("Tree.collapsedIcon", emptyIcon)
    UIManager.put("Tree.leftChildIndent", 10)
    UIManager.put("Tree.rightChildIndent", 0)
    UIManager.put("Tree.paintLines", false)
    val model = makeModel()
    val cardLayout = CardLayout()
    val p = JPanel(cardLayout)

    // https://ateraimemo.com/Swing/TraverseAllNodes.html
    val root = model.root as DefaultMutableTreeNode
    root.postorderEnumeration().toList()
        .filterIsInstance<DefaultMutableTreeNode>()
        .map { it.getUserObject().toString() }
        .forEach {p.add(JLabel(it), it) }
    val tree: JTree = RowSelectionTree()
    tree.setModel(model)
    tree.setRowHeight(32)
    tree.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2))

    // https://ateraimemo.com/Swing/ExpandAllNodes.html
    var row = 0
    while (row < tree.getRowCount()) {
      tree.expandRow(row++)
    }
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION)
    tree.addTreeSelectionListener { e ->
      // https://ateraimemo.com/Swing/CardLayoutTabbedPane.html
      (e.getNewLeadSelectionPath().getLastPathComponent() as? DefaultMutableTreeNode)?.also {
        cardLayout.show(p, it.getUserObject().toString())
      }
    }
    val sp = JSplitPane()
    sp.setLeftComponent(JScrollPane(tree))
    sp.setRightComponent(JScrollPane(p))
    sp.setResizeWeight(.5)
    add(sp)
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeModel(): DefaultTreeModel {
    val root = DefaultMutableTreeNode("root")
    val c1 = DefaultMutableTreeNode("1. Introduction")
    root.add(c1)
    val c2 = DefaultMutableTreeNode("2. Chapter")
    c2.add(DefaultMutableTreeNode("2.1. Section"))
    c2.add(DefaultMutableTreeNode("2.2. Section"))
    c2.add(DefaultMutableTreeNode("2.3. Section"))
    root.add(c2)
    val c3 = DefaultMutableTreeNode("3. Chapter")
    c3.add(DefaultMutableTreeNode("3.1. Section"))
    c3.add(DefaultMutableTreeNode("3.2. Section"))
    c3.add(DefaultMutableTreeNode("3.3. Section"))
    c3.add(DefaultMutableTreeNode("3.4. Section"))
    root.add(c3)
    return DefaultTreeModel(root)
  }
}

// https://ateraimemo.com/Swing/TreeRowSelection.html
class RowSelectionTree : JTree() {
  @Transient
  private var listener: TreeWillExpandListener? = null

  override fun paintComponent(g: Graphics) {
    g.setColor(getBackground())
    g.fillRect(0, 0, getWidth(), getHeight())
    val g2 = g.create() as? Graphics2D ?: return
    g2.setPaint(SELECTED_COLOR)
    getSelectionRows()
        ?.map { getRowBounds(it) }
        ?.forEach { g2.fillRect(0, it.y, getWidth(), it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      getLeadSelectionPath().also {
        val r = getRowBounds(getRowForPath(it))
        g2.setPaint(SELECTED_COLOR.darker())
        g2.drawRect(0, r.y, getWidth() - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    removeTreeWillExpandListener(listener)
    super.updateUI()
    setUI(object : BasicTreeUI() {
      override fun getPathBounds(tree: JTree?, path: TreePath?): Rectangle? {
        return if (tree != null && treeState != null) {
          getPathBounds(path, tree.getInsets(), Rectangle())
        } else null
      }

      private fun getPathBounds(path: TreePath?, insets: Insets, bounds: Rectangle): Rectangle? {
        val rect = treeState.getBounds(path, bounds)
        if (rect != null) {
          rect.width = tree.width
          rect.y += insets.top
        }
        return rect
      }
    })
    UIManager.put("Tree.repaintWholeRow", java.lang.Boolean.TRUE)
    setCellRenderer(Handler())
    isOpaque = false
    isRootVisible = false
    // https://ateraimemo.com/Swing/TreeNodeCollapseVeto.html

    listener = object : TreeWillExpandListener {
      override fun treeWillExpand(e: TreeExpansionEvent?) { // throws ExpandVetoException {
        // throw new ExpandVetoException(e, "Tree expansion cancelled");
      }

      @Throws(ExpandVetoException::class)
      override fun treeWillCollapse(e: TreeExpansionEvent?) {
        throw ExpandVetoException(e, "Tree collapse cancelled")
      }
    }
    addTreeWillExpandListener(listener)
  }

  private class Handler : DefaultTreeCellRenderer() {
    override fun getTreeCellRendererComponent(
      tree: JTree,
      value: Any?,
      selected: Boolean,
      expanded: Boolean,
      leaf: Boolean,
      row: Int,
      hasFocus: Boolean
    ): Component {
      val l =
        super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus) as JLabel
      l.background = if (selected) SELECTED_COLOR else tree.background
      l.isOpaque = true
      return l
    }
  }

  companion object {
    private val SELECTED_COLOR = Color(0x6496C8)
  }
}

class EmptyIcon : Icon {
  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    /* Empty icon */
  }

  override fun getIconWidth() = 0

  override fun getIconHeight() = 0
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
