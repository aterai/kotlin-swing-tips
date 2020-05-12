package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeNode

fun makeUI(): Component {
  val tree1 = JTree()
  tree1.isEditable = true

  val tree2 = JTree()
  (tree2.cellRenderer as? DefaultTreeCellRenderer)?.also {
    tree2.cellEditor = object : DefaultTreeCellEditor(tree2, it) {
      override fun isCellEditable(e: EventObject): Boolean {
        val o = tree.lastSelectedPathComponent
        return super.isCellEditable(e) && o is TreeNode && o.isLeaf
      }
    }
  }
  tree2.isEditable = true

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("DefaultTreeCellEditor", JScrollPane(tree1)))
    it.add(makeTitledPanel("LeafTreeCellEditor", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
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
