package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout(5, 5)) {
  private val tree = JTree()
  private val field = JTextField("foo")
  private val renderer = HighlightTreeCellRenderer()

  init {
    field.getDocument().addDocumentListener(object : DocumentListener {
      override fun insertUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun removeUpdate(e: DocumentEvent) {
        fireDocumentChangeEvent()
      }

      override fun changedUpdate(e: DocumentEvent) { /* not needed */ }
    })
    val box = JPanel(BorderLayout())
    box.add(field)
    box.setBorder(BorderFactory.createTitledBorder("Highlight Search"))

    tree.setCellRenderer(renderer)
    renderer.query = field.getText()
    fireDocumentChangeEvent()

    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    add(box, BorderLayout.NORTH)
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }

  protected fun fireDocumentChangeEvent() {
    val q = field.getText()
    renderer.query = q
    val root = tree.getPathForRow(0)
    collapseAll(tree, root)
    if (!q.isEmpty()) {
      searchTree(tree, root, q)
    }
  }

  private fun searchTree(tree: JTree, path: TreePath, q: String) {
    val node = path.getLastPathComponent() as? TreeNode ?: return
    if (node.toString().startsWith(q)) {
      tree.expandPath(path.getParentPath())
    }
    if (!node.isLeaf()) {
      node.children().toList().forEach { searchTree(tree, path.pathByAddingChild(it), q) }
    }
  }

  private fun collapseAll(tree: JTree, parent: TreePath) {
    val node = parent.getLastPathComponent() as? TreeNode ?: return
    if (!node.isLeaf()) {
      // Java 9: Collections.list(node.children()).stream()
      node.children().toList().forEach { parent.pathByAddingChild(it) }
    }
    tree.collapsePath(parent)
  }
}

internal class HighlightTreeCellRenderer : DefaultTreeCellRenderer() {
  var query = ""
  private var rollOver = false

  override fun updateUI() {
    setTextSelectionColor(null)
    setTextNonSelectionColor(null)
    setBackgroundSelectionColor(null)
    setBackgroundNonSelectionColor(null)
    super.updateUI()
  }

  override fun getBackgroundNonSelectionColor() =
      if (rollOver) ROLLOVER_ROW_COLOR else super.getBackgroundNonSelectionColor()

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
      c.setForeground(getTextSelectionColor())
    } else {
      rollOver = !query.isEmpty() && (value?.toString() ?: "").startsWith(query)
      c.setForeground(getTextNonSelectionColor())
      c.setBackground(getBackgroundNonSelectionColor())
    }
    return c
  }

  companion object {
    private val ROLLOVER_ROW_COLOR = Color(0xDC_F0_FF)
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
