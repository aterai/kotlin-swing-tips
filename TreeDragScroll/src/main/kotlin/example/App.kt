package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree1 = JTree()
  val tree2 = JTree()
  expandTree(tree1)
  expandTree(tree2)

  val ma = DragScrollListener()
  tree2.addMouseMotionListener(ma)
  tree2.addMouseListener(ma)

  return JPanel(GridLayout(2, 1)).also {
    it.add(makeTitledPanel("Default", tree1))
    it.add(makeTitledPanel("Drag scroll", tree2))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun expandTree(tree: JTree) {
  val root = tree.model.root as? DefaultMutableTreeNode ?: return
  root
    .preorderEnumeration()
    .toList()
    .filterIsInstance<DefaultMutableTreeNode>()
    .map { TreePath(it.path) }
    .forEach { tree.expandRow(tree.getRowForPath(it)) }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val scroll = JScrollPane(c)
  scroll.border = BorderFactory.createTitledBorder(title)
  return scroll
}

private class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      val cp = SwingUtilities.convertPoint(c, e.point, it)
      val vp = it.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, it.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      pp.location = SwingUtilities.convertPoint(c, e.point, it)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
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
