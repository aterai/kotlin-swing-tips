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

private val logArea = JTextArea()

fun makeUI(): Component {
  logArea.isEditable = false
  val contextMenu = JPopupMenu()
  contextMenu.add("clear").addActionListener { logArea.text = "" }
  logArea.componentPopupMenu = contextMenu

  val tree = object : JTree() {
    override fun updateUI() {
      setCellEditor(null)
      super.updateUI()
      isEditable = true
      setCellEditor(createTreeCellEditor(this))
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

    private fun createTreeCellEditor(tree: JTree): TreeCellEditor {
      return object : DefaultTreeCellEditor(
        tree,
        tree.cellRenderer as? DefaultTreeCellRenderer,
      ) {
        override fun isCellEditable(e: EventObject?): Boolean {
          appendLog("TreeCellEditor#isCellEditable(EventObject)")
          return when (e) {
            is MouseEvent -> {
              appendLog("  MouseEvent")
              appendLog("  getPoint(): %s".format(e.point))
              appendLog("  getClickCount: %d".format(e.clickCount))
              appendLog("  isShiftDown: %s".format(e.isShiftDown))
              appendLog("  isControlDown: %s".format(e.isControlDown))
              e.clickCount >= 2 || e.isShiftDown || e.isControlDown
            }

            is KeyEvent -> {
              appendLog("  KeyEvent")
              super.isCellEditable(e)
            }

            else -> { // e == null
              appendLog("  startEditing Action(F2)")
              super.isCellEditable(e)
            }
          }
        }
      }
    }
  }

  val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
  splitPane.resizeWeight = .5
  splitPane.topComponent = JScrollPane(tree)
  splitPane.bottomComponent = JScrollPane(logArea)

  return JPanel(BorderLayout()).also {
    it.add(splitPane)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun appendLog(message: String) {
  logArea.append(message + "\n")
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
