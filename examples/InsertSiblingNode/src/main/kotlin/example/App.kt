package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree()
  tree.componentPopupMenu = TreePopupMenu()

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null

  init {
    add("add child node").addActionListener {
      val tree = invoker as? JTree
      val model = tree?.model
      if (model is DefaultTreeModel) {
        val self = path?.lastPathComponent as? DefaultMutableTreeNode
        val child = DefaultMutableTreeNode("New child node")
        self?.add(child)
        model.reload(self)
      }
    }
    addSeparator()
    add("insert preceding sibling node").addActionListener {
      val tree = invoker as? JTree
      val model = tree?.model
      val self = path?.lastPathComponent as? MutableTreeNode
      val parent = self?.parent as? MutableTreeNode
      val child = DefaultMutableTreeNode("New preceding sibling")
      if (model is DefaultTreeModel) {
        val index = model.getIndexOfChild(parent, self)
        parent?.insert(child, index)
        model.reload(parent)
      }
    }
    add("insert following sibling node").addActionListener {
      val tree = invoker as? JTree
      val model = tree?.model
      val self = path?.lastPathComponent as? MutableTreeNode
      val parent = self?.parent as? MutableTreeNode
      val child = DefaultMutableTreeNode("New following sibling")
      if (model is DefaultTreeModel) {
        val index = model.getIndexOfChild(parent, self)
        parent?.insert(child, index + 1)
        model.reload(parent)
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTree) {
      path = c.getPathForLocation(x, y).also {
        super.show(c, x, y)
      }
    }
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
