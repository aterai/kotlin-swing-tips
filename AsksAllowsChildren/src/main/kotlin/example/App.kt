package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class MainPanel : JPanel(GridLayout(1, 2)) {
  init {
    val t = JTree(makeDefaultTreeModel())
    t.setComponentPopupMenu(TreePopupMenu())
    add(makeTitledPanel("Default", JScrollPane(t)))

    val model = makeDefaultTreeModel()
    val tree = JTree(model)
    tree.setComponentPopupMenu(TreePopupMenu())
    // model.setAsksAllowsChildren(true);
    val check = JCheckBox("setAsksAllowsChildren")
    check.addActionListener { e ->
      model.setAsksAllowsChildren((e.getSource() as? JCheckBox)?.isSelected() ?: false)
      tree.repaint()
    }
    val p = JPanel(BorderLayout())
    p.add(JScrollPane(tree))
    p.add(check, BorderLayout.SOUTH)
    add(makeTitledPanel("setAsksAllowsChildren", p))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTitledPanel(title: String, c: Component): Component {
    val p = JPanel(BorderLayout())
    p.setBorder(BorderFactory.createTitledBorder(title))
    p.add(c)
    return p
  }

  private fun makeDefaultTreeModel(): DefaultTreeModel {
    val root = DefaultMutableTreeNode("Root")
    root.add(DefaultMutableTreeNode("colors").also {
      it.add(DefaultMutableTreeNode("blue", false))
      it.add(DefaultMutableTreeNode("violet", false))
      it.add(DefaultMutableTreeNode("red", false))
      it.add(DefaultMutableTreeNode("yellow", false))
    })
    root.add(DefaultMutableTreeNode("sports").also {
      it.add(DefaultMutableTreeNode("basketball", false))
      it.add(DefaultMutableTreeNode("soccer", false))
      it.add(DefaultMutableTreeNode("football", false))
      it.add(DefaultMutableTreeNode("hockey", false))
    })
    root.add(DefaultMutableTreeNode("food").also {
      it.add(DefaultMutableTreeNode("hot dogs", false))
      it.add(DefaultMutableTreeNode("pizza", false))
      it.add(DefaultMutableTreeNode("ravioli", false))
      it.add(DefaultMutableTreeNode("bananas", false))
    })
    root.add(DefaultMutableTreeNode("test"))
    return DefaultTreeModel(root)
  }
}

class TreePopupMenu : JPopupMenu() {
  private val textField = object : JTextField(24) {
    @Transient
    private var listener: AncestorListener? = null

    override fun updateUI() {
      removeAncestorListener(listener)
      super.updateUI()
      listener = FocusAncestorListener()
      addAncestorListener(listener)
    }
  }
  private var path: TreePath? = null
  private val addFolderItem = add("add folder")
  private val addNodeItem = add("add node")

  init {
    addFolderItem.addActionListener {
      (getInvoker() as? JTree)?.also { tree ->
        (tree.getModel() as? DefaultTreeModel)?.also { model ->
          (path?.getLastPathComponent() as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New Folder", true)
            child.setAllowsChildren(true)
            model.insertNodeInto(child, parent, parent.getChildCount())
            tree.scrollPathToVisible(TreePath(child.getPath()))
          }
        }
      }
    }

    addNodeItem.addActionListener {
      (getInvoker() as? JTree)?.also { tree ->
        (tree.getModel() as? DefaultTreeModel)?.also { model ->
          (path?.getLastPathComponent() as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New Item", false)
            // child.setAllowsChildren(false)
            model.insertNodeInto(child, parent, parent.getChildCount())
            tree.scrollPathToVisible(TreePath(child.getPath()))
          }
        }
      }
    }

    add("edit").addActionListener {
      val node = path?.getLastPathComponent()
      if (node is DefaultMutableTreeNode) {
        textField.setText(node.getUserObject()?.toString())
        val tree = getInvoker() as? JTree ?: return@addActionListener
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
      val node = path?.getLastPathComponent()
      if (node is DefaultMutableTreeNode && !node.isRoot()) {
        ((getInvoker() as? JTree)?.getModel() as? DefaultTreeModel)?.removeNodeFromParent(node)
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTree) {
      path = c.getPathForLocation(x, y)
      path?.also { treePath ->
        (treePath.getLastPathComponent() as? DefaultMutableTreeNode)?.also { node ->
          val flag = node.getAllowsChildren()
          addFolderItem.setEnabled(flag)
          addNodeItem.setEnabled(flag)
          super.show(c, x, y)
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
