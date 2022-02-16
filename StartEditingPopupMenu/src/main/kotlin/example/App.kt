package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = JTree()
  val renderer = tree.cellRenderer as? DefaultTreeCellRenderer
  tree.cellEditor = object : DefaultTreeCellEditor(tree, renderer) {
    override fun isCellEditable(e: EventObject?) = e !is MouseEvent && super.isCellEditable(e)
  }
  tree.isEditable = true
  tree.componentPopupMenu = TreePopupMenu()
  return JScrollPane(tree).also {
    it.preferredSize = Dimension(320, 240)
  }
}

private class TreePopupMenu : JPopupMenu() {
  private var path: TreePath? = null
  private val editItem: JMenuItem
  private val editDialogItem: JMenuItem

  init {
    val field = JTextField()
    field.addAncestorListener(FocusAncestorListener())
    editItem = add("Edit")
    editItem.addActionListener {
      path?.also {
        (invoker as? JTree)?.startEditingAtPath(it)
      }
    }
    editDialogItem = add("Edit Dialog")
    editDialogItem.addActionListener {
      (path?.lastPathComponent as? DefaultMutableTreeNode)?.also { node ->
        field.text = node.userObject.toString()
        (invoker as? JTree)?.also { tree ->
          val ret = JOptionPane.showConfirmDialog(
            tree,
            field,
            "Rename",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
          )
          if (ret == JOptionPane.OK_OPTION) {
            tree.model.valueForPathChanged(path, field.text)
          }
        }
      }
    }
    add("dummy")
  }

  override fun show(c: Component, x: Int, y: Int) {
    (c as? JTree)?.also { tree ->
      val tsp = tree.selectionPaths
      path = tree.getPathForLocation(x, y)
      val isEditable = tsp != null && tsp.size == 1 && tsp[0] == path
      editItem.isEnabled = isEditable
      editDialogItem.isEnabled = isEditable
      super.show(c, x, y)
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
