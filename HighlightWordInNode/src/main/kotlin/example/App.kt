package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  private val tree = JTree()
  private val field = JTextField("foo")
  private val renderer = HighlightTreeCellRenderer()

  private fun fireDocumentChangeEvent() {
    val q = field.getText()
    renderer.setQuery(q)
    val root = tree.getPathForRow(0)
    collapseAll(tree, root)
    if (q.isNotEmpty()) {
      searchTree(tree, root, q)
    }
  }

  private fun searchTree(tree: JTree, path: TreePath, q: String) {
    val node = path.getLastPathComponent() as? TreeNode ?: return
    if (node.toString().startsWith(q)) {
      tree.expandPath(path.parentPath)
    }
    if (!node.isLeaf()) {
      node.children().toList()
        .forEach { searchTree(tree, path.pathByAddingChild(it), q) }
    }
  }

  private fun collapseAll(tree: JTree, parent: TreePath) {
    val node = parent.getLastPathComponent() as? TreeNode ?: return
    if (!node.isLeaf()) {
      node.children().toList()
        .forEach { collapseAll(tree, parent.pathByAddingChild(it)) }
    }
    tree.collapsePath(parent)
  }

  init {
    field.getDocument().addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun removeUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun changedUpdate(e: DocumentEvent) {
        /* not needed */
      }
    })
    val n = JPanel(BorderLayout())
    n.add(field)
    n.setBorder(BorderFactory.createTitledBorder("Search"))
    tree.setCellRenderer(renderer)
    renderer.setQuery(field.getText())
    fireDocumentChangeEvent()
    add(n, BorderLayout.NORTH)
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class HighlightTreeCellRenderer : JTextField(), TreeCellRenderer {
  private var query: String? = null
  override fun updateUI() {
    super.updateUI()
    setOpaque(true)
    setBorder(BorderFactory.createEmptyBorder())
    setForeground(Color.BLACK)
    setBackground(Color.WHITE)
    setEditable(false)
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
    highlighter.removeAllHighlights()
    setText(txt)
    setBackground(if (selected) BACKGROUND_SELECTION_COLOR else Color.WHITE)
    val q = query ?: ""
    if (q.isNotEmpty() && txt.startsWith(q)) {
      runCatching { highlighter.addHighlight(0, q.length, HIGHLIGHT) }
    }
    return this
  }

  companion object {
    private val BACKGROUND_SELECTION_COLOR = Color(0xDCF0FF)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
