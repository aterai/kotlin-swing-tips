package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

fun makeUI(): Component {
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
  (model.root as? DefaultMutableTreeNode)?.also { root ->
    root.postorderEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .map { it.userObject.toString() }
      .forEach { p.add(JLabel(it), it) }
  }

  val tree = RowSelectionTree()
  tree.model = model
  tree.rowHeight = 32
  tree.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)

  // https://ateraimemo.com/Swing/ExpandAllNodes.html
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row++)
  }
  tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
  tree.addTreeSelectionListener { e ->
    // https://ateraimemo.com/Swing/CardLayoutTabbedPane.html
    (e.newLeadSelectionPath.lastPathComponent as? DefaultMutableTreeNode)?.also {
      cardLayout.show(p, it.userObject.toString())
    }
  }
  val sp = JSplitPane()
  sp.leftComponent = JScrollPane(tree)
  sp.rightComponent = JScrollPane(p)
  sp.resizeWeight = .5
  return JPanel(BorderLayout(2, 2)).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
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

// https://ateraimemo.com/Swing/TreeRowSelection.html
private class RowSelectionTree : JTree() {
  @Transient
  private var listener: TreeWillExpandListener? = null

  override fun paintComponent(g: Graphics) {
    g.color = background
    g.fillRect(0, 0, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = SELECTED_COLOR
    selectionRows
      ?.map { getRowBounds(it) }
      ?.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath.also {
        val r = getRowBounds(getRowForPath(it))
        g2.paint = SELECTED_COLOR.darker()
        g2.drawRect(0, r.y, width - 1, r.height - 1)
      }
    }
    g2.dispose()
  }

  override fun updateUI() {
    setCellRenderer(null)
    removeTreeWillExpandListener(listener)
    super.updateUI()
    val tmp = object : BasicTreeUI() {
      override fun getPathBounds(tree: JTree?, path: TreePath?) =
        tree?.let {
          getPathBounds(path, it.insets, Rectangle())
        }

      private fun getPathBounds(path: TreePath?, insets: Insets, bounds: Rectangle) =
        treeState?.getBounds(path, bounds)?.also {
          it.width = tree.width
          it.y += insets.top
        }
    }
    setUI(tmp)
    UIManager.put("Tree.repaintWholeRow", true)
    val r = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
        it.background = if (selected) SELECTED_COLOR else tree.background
        (it as? JComponent)?.isOpaque = true
      }
    }
    isOpaque = false
    isRootVisible = false

    // https://ateraimemo.com/Swing/TreeNodeCollapseVeto.html
    listener = object : TreeWillExpandListener {
      override fun treeWillExpand(e: TreeExpansionEvent) { // throws ExpandVetoException {
        // throw ExpandVetoException(e, "Tree expansion cancelled")
      }

      @Throws(ExpandVetoException::class)
      override fun treeWillCollapse(e: TreeExpansionEvent) {
        throw ExpandVetoException(e, "Tree collapse cancelled")
      }
    }
    addTreeWillExpandListener(listener)
  }

  companion object {
    private val SELECTED_COLOR = Color(0x64_96_C8)
  }
}

private class EmptyIcon : Icon {
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
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
