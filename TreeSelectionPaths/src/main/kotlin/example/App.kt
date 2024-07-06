package example

import java.awt.*
import javax.swing.*
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree()
  tree.addTreeWillExpandListener(object : TreeWillExpandListener {
    override fun treeWillExpand(e: TreeExpansionEvent) {
      val t = e.source as? JTree ?: return
      val anchor = t.anchorSelectionPath
      val lead = t.leadSelectionPath
      val path = e.path
      val node = path.lastPathComponent
      if (node is DefaultMutableTreeNode && t.isPathSelected(path)) {
        val paths = node
          .children()
          .toList()
          .filterIsInstance<DefaultMutableTreeNode>()
          .map { TreePath(it.path) }
          .toTypedArray()
        t.addSelectionPaths(paths)
        t.anchorSelectionPath = anchor
        t.leadSelectionPath = lead
      }
    }

    override fun treeWillCollapse(e: TreeExpansionEvent) {
      // do nothing
    }
  })
  return JPanel(GridLayout(1, 2, 5, 5)).also {
    it.add(JScrollPane(JTree()))
    it.add(JScrollPane(tree))
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
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
