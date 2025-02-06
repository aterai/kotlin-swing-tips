package example

import java.awt.*
import java.awt.event.KeyEvent
import java.awt.event.MouseEvent
import java.util.EventObject
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellEditor
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.TreeCellEditor
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

private val LOG = JTextArea()

fun makeUI(): Component {
  LOG.isEditable = false
  val popup = JPopupMenu()
  popup.add("clear").addActionListener { LOG.text = "" }
  LOG.componentPopupMenu = popup

  val tree = object : JTree() {
    override fun updateUI() {
      setCellEditor(null)
      super.updateUI()
      isEditable = true
      setCellEditor(makeTreeCellEditor(this))
    }

    override fun isPathEditable(path: TreePath): Boolean {
      appendLog("JTree#isPathEditable(TreePath)")
      appendLog("  getPathCount(): %d".format(path.pathCount))
      val node = path.lastPathComponent
      if (node is TreeNode) {
        appendLog("  isLeaf: %s".format(node.isLeaf))
      }
      if (node is DefaultMutableTreeNode) {
        appendLog("  getLevel: %d".format(node.level))
      }
      return (node as? TreeNode)?.isLeaf == true
    }

    private fun makeTreeCellEditor(tree: JTree): TreeCellEditor {
      return object : DefaultTreeCellEditor(
        tree,
        tree.cellRenderer as? DefaultTreeCellRenderer,
      ) {
        override fun isCellEditable(e: EventObject?): Boolean {
          appendLog("TreeCellEditor#isCellEditable(EventObject)")
          return if (e is MouseEvent) {
            appendLog("  MouseEvent")
            appendLog("  getPoint(): %s".format(e.point))
            appendLog("  getClickCount: %d".format(e.clickCount))
            appendLog("  isShiftDown: %s".format(e.isShiftDown))
            appendLog("  isControlDown: %s".format(e.isControlDown))
            e.clickCount >= 2 || e.isShiftDown || e.isControlDown
          } else if (e is KeyEvent) {
            appendLog("  KeyEvent")
            super.isCellEditable(e)
          } else { // e == null
            appendLog("  startEditing Action(F2)")
            super.isCellEditable(e)
          }
        }
      }
    }
  }

  val sp = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  sp.resizeWeight = .5
  sp.topComponent = JScrollPane(tree)
  sp.bottomComponent = JScrollPane(LOG)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun appendLog(str: String) {
  LOG.append(str + "\n")
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
