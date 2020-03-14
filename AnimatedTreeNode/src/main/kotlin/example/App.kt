package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.image.ImageObserver
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val icon = ImageIcon(cl.getResource("example/restore_to_background_color.gif"))
  val root = DefaultMutableTreeNode("root")
  val s0 = DefaultMutableTreeNode(NodeObject("default", icon))
  val s1 = DefaultMutableTreeNode(NodeObject("setImageObserver", icon))
  root.add(s0)
  root.add(s1)

  val tree = object : JTree(DefaultTreeModel(root)) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      val r = getCellRenderer()
      setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
        val c = r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
        val l = c as? JLabel ?: return@setCellRenderer c
        val uo = (value as? DefaultMutableTreeNode)?.userObject
        if (uo is NodeObject) {
          l.text = uo.title
          l.icon = uo.icon
        } else {
          l.text = value?.toString() ?: ""
          l.icon = null
        }
        l
      }
    }
  }
  val path = TreePath(s1.path)
  icon.imageObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
    if (!tree.isShowing) {
      return@ImageObserver false
    }
    val cellRect = tree.getPathBounds(path)
    if (infoFlags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS) != 0 && cellRect != null) {
      tree.repaint(cellRect)
    }
    infoFlags and (ImageObserver.ALLBITS or ImageObserver.ABORT) == 0
  }
  tree.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private data class NodeObject(val title: String, val icon: Icon)

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
