package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

private const val TAG = "<html><b>"
private const val BOLD_METAL = "swing.boldMetal"

fun makeUI(): Component {
  val check = JCheckBox(BOLD_METAL)
  check.addActionListener { e ->
    (e.source as? JCheckBox)?.also {
      UIManager.put(BOLD_METAL, it.isSelected)
      runCatching {
        UIManager.setLookAndFeel(MetalLookAndFeel())
      }
      SwingUtilities.updateComponentTreeUI(it.topLevelAncestor)
    }
  }

  val tree = JTree()
  tree.componentPopupMenu = TreePopupMenu()

  val tabbedPane = JTabbedPane()
  tabbedPane.border = BorderFactory.createTitledBorder("TitledBorder")
  tabbedPane.addTab(TAG + "JTree", JScrollPane(tree))
  tabbedPane.addTab("JLabel", JLabel("JLabel"))
  tabbedPane.addTab("JTextArea", JScrollPane(JTextArea("JTextArea")))
  tabbedPane.addTab("JButton", JScrollPane(JButton("JButton")))
  tabbedPane.addChangeListener { e ->
    (e.source as? JTabbedPane)?.also {
      for (i in 0 until it.tabCount) {
        val title = it.getTitleAt(i)
        if (i == it.selectedIndex) {
          it.setTitleAt(i, TAG + title)
        } else if (title.startsWith(TAG)) {
          it.setTitleAt(i, title.substring(TAG.length))
        }
      }
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(check, BorderLayout.NORTH)
    it.add(tabbedPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null
  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTree)?.also { tree ->
      path = tree.getPathForLocation(x, y)
      path?.also {
        tree.selectionPath = it
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
    add("edit").addActionListener {
      val tree = invoker
      val node = path?.lastPathComponent
      if (tree is JTree && node is DefaultMutableTreeNode) {
        textField.text = node.userObject.toString()
        val ret = JOptionPane.showConfirmDialog(
          tree,
          textField,
          "edit",
          JOptionPane.OK_CANCEL_OPTION,
          JOptionPane.PLAIN_MESSAGE
        )
        if (ret == JOptionPane.OK_OPTION) {
          tree.model.valueForPathChanged(path, textField.text)
        }
      }
    }
    addSeparator()
    add("remove").addActionListener {
      val tree = invoker
      val node = path?.lastPathComponent
      if (tree is JTree && node is DefaultMutableTreeNode && !node.isRoot) {
        (tree.model as? DefaultTreeModel)?.removeNodeFromParent(node)
      }
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    /* not needed */
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    /* not needed */
  }
}

fun main() {
  UIManager.put("swing.boldMetal", false)
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
