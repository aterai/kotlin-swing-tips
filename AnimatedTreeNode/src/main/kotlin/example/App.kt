package example

import java.awt.*
import java.awt.image.BufferedImage
import java.awt.image.ImageObserver
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

private val tree = object : JTree() {
  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    val r = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      r.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
        if (it is JLabel) {
          val uo = (value as? DefaultMutableTreeNode)?.userObject
          if (uo is NodeObject) {
            it.text = uo.title
            it.icon = uo.icon
          } else {
            it.text = value?.toString() ?: ""
            it.icon = null
          }
        }
      }
    }
  }
}

fun makeUI(): Component {
  val icon = makeImageIcon("example/restore_to_background_color.gif")
  val root = DefaultMutableTreeNode("root")
  val s0 = DefaultMutableTreeNode(NodeObject("default", icon))
  val s1 = DefaultMutableTreeNode(NodeObject("setImageObserver", icon))
  root.add(s0)
  root.add(s1)
  tree.model = DefaultTreeModel(root)
  tree.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val path = TreePath(s1.path)
  icon.imageObserver = ImageObserver { _, infoFlags, _, _, _, _ ->
    if (tree.isShowing) {
      val rect = tree.getPathBounds(path)
      if (infoFlags and (ImageObserver.FRAMEBITS or ImageObserver.ALLBITS) != 0 && rect != null) {
        tree.repaint(rect)
      }
      infoFlags and (ImageObserver.ALLBITS or ImageObserver.ABORT) == 0
    } else {
      false
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeImageIcon(path: String): ImageIcon {
  val cl = Thread.currentThread().contextClassLoader
  return cl.getResource(path)?.let { ImageIcon(it) }
    ?: ImageIcon(makeMissingImage())
}

private fun makeMissingImage(): Image {
  val missingIcon = UIManager.getIcon("html.missingImage")
  val iw = missingIcon.iconWidth
  val ih = missingIcon.iconHeight
  val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
  val g2 = bi.createGraphics()
  missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
  g2.dispose()
  return bi
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
