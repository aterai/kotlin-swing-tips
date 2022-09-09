package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.text.html.HTML
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

private const val HTML_TEXT = """
  <html>
    <body>
      <h1>Scrollspy</h1>
      <p id='main'></p>
      <p id='bottom'>id=bottom</p>
    </body>
  </html>
"""
private val editor = JEditorPane()

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
  val htmlEditorKit = HTMLEditorKit()
  editor.isEditable = false
  editor.editorKit = htmlEditorKit
  editor.text = HTML_TEXT
  val doc = editor.document as? HTMLDocument
  val element = doc?.getElement("main")
  val model = makeModel()
  (model.root as? DefaultMutableTreeNode)?.preorderEnumeration()?.toList()
    ?.filterIsInstance<DefaultMutableTreeNode>()
    ?.filterNot { it.isRoot }
    ?.map { it.userObject }
    ?.forEach {
      runCatching {
        val tag = "<a name='$it' href='#'>$it</a>" + "<br />".repeat(8)
        doc?.insertBeforeEnd(element, tag)
      }.onFailure {
        UIManager.getLookAndFeel().provideErrorFeedback(editor)
      }
    }

  val tree = makeTree()
  tree.model = model
  tree.addTreeSelectionListener { e ->
    val n = e.newLeadSelectionPath.lastPathComponent
    if (tree.isEnabled && n is DefaultMutableTreeNode) {
      editor.scrollToReference(n.userObject.toString())
    }
  }

  // scroll to top of page
  EventQueue.invokeLater { editor.scrollRectToVisible(editor.bounds) }

  val scroll = JScrollPane(editor)
  scroll.verticalScrollBar.model.addChangeListener {
    val itr = doc?.getIterator(HTML.Tag.A)
    while (itr?.isValid == true) {
      val r = runCatching {
        editor.modelToView(itr.startOffset)
      }.getOrNull()
      if (r != null && editor.visibleRect.contains(r.location)) {
        searchTreeNode(tree, itr.attributes.getAttribute(HTML.Attribute.NAME))
        break
      }
      itr.next()
    }
  }

  return JSplitPane().also {
    it.leftComponent = JScrollPane(tree)
    it.rightComponent = scroll
    it.resizeWeight = .5
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeTree(): JTree {
  val tree = RowSelectionTree()
  tree.rowHeight = 32
  tree.border = BorderFactory.createEmptyBorder(2, 2, 2, 2)
  tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
  expandAllNodes(tree)
  return tree
}

// https://ateraimemo.com/Swing/ExpandAllNodes.html
private fun expandAllNodes(tree: JTree) {
  var row = 0
  while (row < tree.rowCount) {
    tree.expandRow(row++)
  }
}

private fun searchTreeNode(tree: JTree, name: Any) {
  val model = tree.model
  val root = model.root as? DefaultMutableTreeNode ?: return
  root.preorderEnumeration().toList()
    .filterIsInstance<DefaultMutableTreeNode>()
    .firstOrNull { name == it.userObject.toString() }
    ?.also {
      tree.isEnabled = false
      val path = TreePath(it.path)
      tree.selectionPath = path
      tree.scrollPathToVisible(path)
      tree.isEnabled = true
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

private class RowSelectionTree : JTree() {
  @Transient private var listener: TreeWillExpandListener? = null

  override fun paintComponent(g: Graphics) {
    val sr = selectionRows
    if (sr == null) {
      super.paintComponent(g)
      return
    }
    g.color = background
    g.fillRect(0, 0, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = SELECTED_COLOR
    sr.map { getRowBounds(it) }.forEach { g2.fillRect(0, it.y, width, it.height) }
    super.paintComponent(g)
    if (hasFocus()) {
      leadSelectionPath?.also {
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

    listener = object : TreeWillExpandListener {
      override fun treeWillExpand(e: TreeExpansionEvent) { // throws ExpandVetoException {
        // throw ExpandVetoException(e, "Tree expansion cancelled");
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
