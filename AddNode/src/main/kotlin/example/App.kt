package example

import java.awt.*
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree()
  tree.componentPopupMenu = TreePopupMenu()
  return JScrollPane(tree).also {
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null

  init {
    val textField = object : JTextField(24) {
      @Transient private var listener: AncestorListener? = null

      override fun updateUI() {
        removeAncestorListener(listener)
        super.updateUI()
        listener = FocusAncestorListener()
        addAncestorListener(listener)
      }
    }
    add("add").addActionListener {
      (invoker as? JTree)?.also { tree ->
        (tree.model as? DefaultTreeModel)?.also { model ->
          (path?.lastPathComponent as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New node")
            model.insertNodeInto(child, parent, parent.childCount)
            tree.scrollPathToVisible(TreePath(child.path))
          }
        }
      }
    }
    add("add & reload").addActionListener {
      (invoker as? JTree)?.also { tree ->
        (tree.model as? DefaultTreeModel)?.also { model ->
          (path?.lastPathComponent as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New node")
            parent.add(child)
            model.reload(parent)
            tree.scrollPathToVisible(TreePath(child.path))
          }
        }
      }
    }
    add("edit").addActionListener {
      (path?.lastPathComponent as? DefaultMutableTreeNode)?.also {
        textField.text = it.userObject.toString()
      }
      (invoker as? JTree)?.also { tree ->
        val ret = JOptionPane.showConfirmDialog(
          tree,
          textField,
          "edit",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE,
        )
        if (ret == JOptionPane.OK_OPTION) {
          tree.model.valueForPathChanged(path, textField.text)
        }
      }
    }
    addSeparator()
    add("remove").addActionListener {
      val node = path?.lastPathComponent as? DefaultMutableTreeNode
      if (node != null && !node.isRoot) {
        (invoker as? JTree)?.also { tree ->
          (tree.model as? DefaultTreeModel)?.removeNodeFromParent(node)
        }
      }
    }
  }

  override fun show(c: Component?, x: Int, y: Int) {
    (c as? JTree)?.also { tree ->
      path = tree.getPathForLocation(x, y)
      path?.also { treePath ->
        c.selectionPath = treePath
        super.show(c, x, y)
      }
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    // not needed
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    // not needed
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
