package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private val tree = JTree()
private val field = JTextField("foo")
private val renderer = HighlightTreeCellRenderer()

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
  val box = JPanel(BorderLayout())
  box.add(field)
  box.border = BorderFactory.createTitledBorder("Highlight Search")

  tree.cellRenderer = renderer
  renderer.query = field.text
  fireDocumentChangeEvent()

  return JPanel(BorderLayout(5, 5)).also {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.add(box, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun fireDocumentChangeEvent() {
  val q = field.text
  renderer.query = q
  val root = tree.getPathForRow(0)
  collapseAll(tree, root)
  if (q.isNotEmpty()) {
    searchTree(tree, root, q)
  }
}

private fun searchTree(tree: JTree, path: TreePath, q: String) {
  val node = path.lastPathComponent as? TreeNode ?: return
  if (node.toString().startsWith(q)) {
    tree.expandPath(path.parentPath)
  }
  if (!node.isLeaf) {
    node.children().toList().forEach { searchTree(tree, path.pathByAddingChild(it), q) }
  }
}

private fun collapseAll(tree: JTree, parent: TreePath) {
  val node = parent.lastPathComponent as? TreeNode ?: return
  if (!node.isLeaf) {
    // Java 9: Collections.list(node.children()).stream()
    node.children().toList().forEach { parent.pathByAddingChild(it) }
  }
  tree.collapsePath(parent)
}

private class HighlightTreeCellRenderer : DefaultTreeCellRenderer() {
  var query = ""
  private var rollOver = false

  override fun updateUI() {
    setTextSelectionColor(null)
    setTextNonSelectionColor(null)
    setBackgroundSelectionColor(null)
    setBackgroundNonSelectionColor(null)
    super.updateUI()
  }

  override fun getBackgroundNonSelectionColor(): Color? =
    if (rollOver) HIGHLIGHT_ROW_BGC else super.getBackgroundNonSelectionColor()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    if (selected) {
      c.foreground = getTextSelectionColor()
    } else {
      rollOver = query.isNotEmpty() && (value?.toString() ?: "").startsWith(query)
      c.foreground = getTextNonSelectionColor()
      c.background = getBackgroundNonSelectionColor()
    }
    return c
  }

  companion object {
    private val HIGHLIGHT_ROW_BGC = Color(0xDC_F0_FF)
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
