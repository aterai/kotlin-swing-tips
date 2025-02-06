package example

import java.awt.*
import javax.swing.*
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val t = JTree(makeDefaultTreeModel())
  t.componentPopupMenu = TreePopupMenu()

  val model = makeDefaultTreeModel()
  val tree = JTree(model)
  tree.componentPopupMenu = TreePopupMenu()
  // model.setAsksAllowsChildren(true)

  val check = JCheckBox("setAsksAllowsChildren")
  check.addActionListener { e ->
    model.setAsksAllowsChildren((e.source as? JCheckBox)?.isSelected ?: false)
    tree.repaint()
  }

  val p = JPanel(BorderLayout())
  p.add(JScrollPane(tree))
  p.add(check, BorderLayout.SOUTH)

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("Default", JScrollPane(t)))
    it.add(makeTitledPanel("setAsksAllowsChildren", p))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private fun makeDefaultTreeModel(): DefaultTreeModel {
  val root = DefaultMutableTreeNode("Root")
  val n1 = DefaultMutableTreeNode("colors").also {
    it.add(DefaultMutableTreeNode("blue", false))
    it.add(DefaultMutableTreeNode("violet", false))
    it.add(DefaultMutableTreeNode("red", false))
    it.add(DefaultMutableTreeNode("yellow", false))
  }
  root.add(n1)
  val n2 = DefaultMutableTreeNode("sports").also {
    it.add(DefaultMutableTreeNode("basketball", false))
    it.add(DefaultMutableTreeNode("soccer", false))
    it.add(DefaultMutableTreeNode("football", false))
    it.add(DefaultMutableTreeNode("hockey", false))
  }
  root.add(n2)
  val n3 = DefaultMutableTreeNode("food").also {
    it.add(DefaultMutableTreeNode("hot dogs", false))
    it.add(DefaultMutableTreeNode("pizza", false))
    it.add(DefaultMutableTreeNode("ravioli", false))
    it.add(DefaultMutableTreeNode("bananas", false))
  }
  root.add(n3)
  root.add(DefaultMutableTreeNode("test"))
  return DefaultTreeModel(root)
}

private class TreePopupMenu : JPopupMenu() {
  private val textField = object : JTextField(24) {
    @Transient private var listener: AncestorListener? = null

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
      (invoker as? JTree)?.also { tree ->
        (tree.model as? DefaultTreeModel)?.also { model ->
          (path?.lastPathComponent as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New Folder", true)
            child.allowsChildren = true
            model.insertNodeInto(child, parent, parent.childCount)
            tree.scrollPathToVisible(TreePath(child.path))
          }
        }
      }
    }

    addNodeItem.addActionListener {
      (invoker as? JTree)?.also { tree ->
        (tree.model as? DefaultTreeModel)?.also { model ->
          (path?.lastPathComponent as? DefaultMutableTreeNode)?.also { parent ->
            val child = DefaultMutableTreeNode("New Item", false)
            // child.setAllowsChildren(false)
            model.insertNodeInto(child, parent, parent.childCount)
            tree.scrollPathToVisible(TreePath(child.path))
          }
        }
      }
    }

    add("edit").addActionListener {
      val node = path?.lastPathComponent
      val tree = invoker
      if (node is DefaultMutableTreeNode && tree is JTree) {
        textField.text = node.userObject?.toString()
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
      val node = path?.lastPathComponent
      if (node is DefaultMutableTreeNode && !node.isRoot) {
        ((invoker as? JTree)?.model as? DefaultTreeModel)?.removeNodeFromParent(node)
      }
    }
  }

  override fun show(
    c: Component?,
    x: Int,
    y: Int,
  ) {
    if (c is JTree) {
      path = c.getPathForLocation(x, y)
      path?.also { treePath ->
        (treePath.lastPathComponent as? DefaultMutableTreeNode)?.also { node ->
          val flag = node.allowsChildren
          addFolderItem.isEnabled = flag
          addNodeItem.isEnabled = flag
          super.show(c, x, y)
        }
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
