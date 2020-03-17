package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.plaf.metal.MetalLookAndFeel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  companion object {
    private const val TAG = "<html><b>"
    private const val BOLD_METAL = "swing.boldMetal"
  }

  init {
    val check = JCheckBox(BOLD_METAL)
    check.addActionListener { e ->
      val c = e.getSource() as? JCheckBox ?: return@addActionListener
      UIManager.put(BOLD_METAL, c.isSelected())
      runCatching {
        UIManager.setLookAndFeel(MetalLookAndFeel())
      }
      SwingUtilities.updateComponentTreeUI(c.getTopLevelAncestor())
    }
    val tree = JTree()
    tree.setComponentPopupMenu(TreePopupMenu())
    val tabbedPane = JTabbedPane()
    tabbedPane.setBorder(BorderFactory.createTitledBorder("TitledBorder"))
    tabbedPane.addTab(TAG + "JTree", JScrollPane(tree))
    tabbedPane.addTab("JLabel", JLabel("JLabel"))
    tabbedPane.addTab("JTextArea", JScrollPane(JTextArea("JTextArea")))
    tabbedPane.addTab("JButton", JScrollPane(JButton("JButton")))
    tabbedPane.addChangeListener { e ->
      val t = e.getSource() as? JTabbedPane ?: return@addChangeListener
      for (i in 0 until t.getTabCount()) {
        val title = t.getTitleAt(i)
        if (i == t.getSelectedIndex()) {
          t.setTitleAt(i, TAG + title)
        } else if (title.startsWith(TAG)) {
          t.setTitleAt(i, title.substring(TAG.length))
        }
      }
    }
    add(check, BorderLayout.NORTH)
    add(tabbedPane)
    setPreferredSize(Dimension(320, 240))
  }
}

class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null
  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTree)?.also { tree ->
      path = tree.getPathForLocation(x, y)
      path?.also {
        tree.setSelectionPath(it)
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
            model.insertNodeInto(child, parent, parent.getChildCount())
            tree.scrollPathToVisible(TreePath(child.getPath()))
          }
        }
      }
    }
    add("edit").addActionListener {
      val node = path?.getLastPathComponent() as? DefaultMutableTreeNode ?: return@addActionListener
      textField.setText(node.getUserObject().toString())
      val tree = getInvoker() as? JTree ?: return@addActionListener
      val ret = JOptionPane.showConfirmDialog(
        tree, textField, "edit", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE
      )
      if (ret == JOptionPane.OK_OPTION) {
        tree.getModel().valueForPathChanged(path, textField.getText())
      }
    }
    addSeparator()
    add("remove").addActionListener {
      val node = path?.getLastPathComponent() as? DefaultMutableTreeNode ?: return@addActionListener
      if (!node.isRoot()) {
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
  UIManager.put("swing.boldMetal", false)
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
