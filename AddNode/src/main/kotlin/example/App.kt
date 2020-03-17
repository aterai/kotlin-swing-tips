package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  init {
    val tree = JTree()
    tree.setComponentPopupMenu(TreePopupMenu())
    add(JScrollPane(tree))
    setPreferredSize(Dimension(320, 240))
  }
}

class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null

  override fun show(c: Component?, x: Int, y: Int) {
    (c as? JTree)?.also { tree ->
      path = tree.getPathForLocation(x, y)
      path?.also { treePath ->
        c.setSelectionPath(treePath)
        super.show(c, x, y)
      }
    }
  }

  init {
    val textField: JTextField = object : JTextField(24) {
      @Transient
      private var listener: AncestorListener? = null

      override fun updateUI() {
        removeAncestorListener(listener)
        super.updateUI()
        listener = FocusAncestorListener()
        addAncestorListener(listener)
      }
    }
    add("add").addActionListener {
      (getInvoker() as? JTree)?.also { tree ->
        (tree.getModel() as? DefaultTreeModel)?.also { model ->
          (path?.getLastPathComponent() as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New node")
            model.insertNodeInto(child, parent, parent.childCount)
            tree.scrollPathToVisible(TreePath(child.path))
          }
        }
      }
    }
    add("add & reload").addActionListener {
      (getInvoker() as? JTree)?.also { tree ->
        (tree.getModel() as? DefaultTreeModel)?.also { model ->
          (path?.getLastPathComponent() as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New node")
            parent.add(child)
            model.reload(parent)
            tree.scrollPathToVisible(TreePath(child.getPath()))
          }
        }
      }
    }
    add("edit").addActionListener {
      val node = path?.getLastPathComponent()
      if (node !is DefaultMutableTreeNode) {
        return@addActionListener
      }
      textField.setText(node.getUserObject().toString())
      (getInvoker() as? JTree)?.also { tree ->
        val ret = JOptionPane.showConfirmDialog(
          tree, textField, "edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
        )
        if (ret == JOptionPane.OK_OPTION) {
          tree.getModel().valueForPathChanged(path, textField.getText())
        }
      }
    }
    addSeparator()
    add("remove").addActionListener {
      val node = path?.getLastPathComponent() as? DefaultMutableTreeNode
      if (node != null && !node.isRoot()) {
        (getInvoker() as? JTree)?.also { tree ->
          (tree.getModel() as? DefaultTreeModel)?.removeNodeFromParent(node)
        }
      }
    }
  }
}

class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.getComponent().requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
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
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
