package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.text.Element
import javax.swing.text.html.HTMLDocument
import javax.swing.text.html.HTMLEditorKit
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

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
  editor.text = """
    <html>
      <body>
        <p id='main'></p>
        <p id='bottom'>id=bottom</p>
      </body>
    </html>
  """.trimIndent()

  val model = makeModel()
  (editor.document as? HTMLDocument)?.also { doc ->
    (model.root as? DefaultMutableTreeNode)?.also { root ->
      val element = doc.getElement("main")
      root.preorderEnumeration().toList()
        .filterIsInstance<DefaultMutableTreeNode>()
        .filterNot { it.isRoot }
        .map { it.userObject }
        .forEach {
          runCatching {
            val tag = "<a name='$it' href='#'>$it</a>" + "<br />".repeat(8)
            doc.insertBeforeEnd(element, tag)
          }.onFailure {
            UIManager.getLookAndFeel().provideErrorFeedback(editor)
          }
        }
    }
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
    (e.newLeadSelectionPath.lastPathComponent as? DefaultMutableTreeNode)?.also {
      val ref = it.userObject.toString()
      editor.scrollToReference(ref)
    }
  }
  val button = JButton("bottom")
  button.addActionListener { scrollToId(editor, "bottom") }
  EventQueue.invokeLater { scrollToId(editor, "main") }
  val sp = JSplitPane()
  sp.leftComponent = JScrollPane(tree)
  sp.rightComponent = JScrollPane(editor)
  sp.resizeWeight = .5

  return JPanel(BorderLayout(2, 2)).also {
    it.add(sp)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun scrollToId(editor: JEditorPane, id: String) {
  val d = editor.document as? HTMLDocument ?: return
  val element: Element = d.getElement(id)
  runCatching {
    val pos = element.startOffset
    val r = editor.modelToView(pos)
    if (r != null) {
      val vis = editor.visibleRect
      r.height = vis.height
      editor.scrollRectToVisible(r)
      editor.caretPosition = pos
    }
  }.onFailure {
    UIManager.getLookAndFeel().provideErrorFeedback(editor)
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
    setUI(object : BasicTreeUI() {
      override fun getPathBounds(tree: JTree?, path: TreePath?): Rectangle? {
        return if (tree != null && treeState != null) {
          getPathBounds(path, tree.insets, Rectangle())
        } else null
      }

      private fun getPathBounds(path: TreePath?, insets: Insets, bounds: Rectangle): Rectangle? {
        val rect = treeState.getBounds(path, bounds)
        rect?.also {
          it.width = tree.width
          it.y += insets.top
        }
        return rect
      }
    })
    UIManager.put("Tree.repaintWholeRow", true)
    val r = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      val c = r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
      c.background = if (selected) SELECTED_COLOR else tree.background
      (c as? JComponent)?.isOpaque = true
      c
    }
    isOpaque = false
    isRootVisible = false

    listener = object : TreeWillExpandListener {
      override fun treeWillExpand(e: TreeExpansionEvent) { // throws ExpandVetoException {
        // throw new ExpandVetoException(e, "Tree expansion cancelled");
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
