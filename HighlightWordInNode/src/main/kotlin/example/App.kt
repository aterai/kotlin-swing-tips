package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private val field = JTextField("foo")
private val tree = object : JTree() {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setCellRenderer(HighlightTreeCellRenderer())
    EventQueue.invokeLater { fireDocumentChangeEvent() }
  }
}

fun makeUI(): Component {
  val dl = object : DocumentListener {
    override fun insertUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun removeUpdate(e: DocumentEvent) {
      fireDocumentChangeEvent()
    }

    override fun changedUpdate(e: DocumentEvent) {
      // not needed
    }
  }
  field.document.addDocumentListener(dl)
  val n = JPanel(BorderLayout())
  n.add(field)
  n.border = BorderFactory.createTitledBorder("Search")

  return JPanel(BorderLayout()).also {
    it.add(n, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun fireDocumentChangeEvent() {
  val q = field.text
  (tree.cellRenderer as? HighlightTreeCellRenderer)?.also {
    it.setQuery(q)
    val root = tree.getPathForRow(0)
    collapseAll(tree, root)
    if (q.isNotEmpty()) {
      searchTree(tree, root, q)
    }
  }
}

private fun searchTree(tree: JTree, path: TreePath, q: String) {
  val node = path.lastPathComponent as? TreeNode ?: return
  if (node.toString().startsWith(q)) {
    tree.expandPath(path.parentPath)
  }
  if (!node.isLeaf) {
    node.children().toList()
      .forEach { searchTree(tree, path.pathByAddingChild(it), q) }
  }
}

private fun collapseAll(tree: JTree, parent: TreePath) {
  val node = parent.lastPathComponent as? TreeNode ?: return
  if (!node.isLeaf) {
    node.children().toList()
      .forEach { collapseAll(tree, parent.pathByAddingChild(it)) }
  }
  tree.collapsePath(parent)
}

private class HighlightTreeCellRenderer : TreeCellRenderer {
  private var query: String? = null
  private val renderer = object : JTextField() {
    override fun updateUI() {
      super.updateUI()
      isOpaque = true
      border = BorderFactory.createEmptyBorder()
      foreground = Color.BLACK
      background = Color.WHITE
      isEditable = false
    }
  }

  fun setQuery(query: String?) {
    this.query = query
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
    val txt = value?.toString() ?: ""
    renderer.highlighter.removeAllHighlights()
    renderer.text = txt
    renderer.background = if (selected) BACKGROUND_SELECTION_COLOR else Color.WHITE
    val q = query ?: ""
    if (q.isNotEmpty() && txt.startsWith(q)) {
      runCatching { renderer.highlighter.addHighlight(0, q.length, HIGHLIGHT) }
    }
    return renderer
  }

  companion object {
    private val BACKGROUND_SELECTION_COLOR = Color(0xDC_F0_FF)
    private val HIGHLIGHT = DefaultHighlightPainter(Color.YELLOW)
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
