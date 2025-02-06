package example

import java.awt.*
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.Timer
import javax.swing.event.TreeExpansionEvent
import javax.swing.event.TreeWillExpandListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.ExpandVetoException
import javax.swing.tree.TreePath

private const val START_HEIGHT = 8
private const val END_HEIGHT = 16
private const val DELAY = 10

fun makeUI(): Component {
  val tree = object : JTree() {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(-1)
      setCellRenderer(HeightTreeCellRenderer())
    }
  }
  val model = tree.model
  (model.root as? DefaultMutableTreeNode)
    ?.breadthFirstEnumeration()
    ?.toList()
    ?.filterIsInstance<DefaultMutableTreeNode>()
    ?.forEach { it.userObject = makeUserObject(it, END_HEIGHT) }
  tree.addTreeWillExpandListener(object : TreeWillExpandListener {
    override fun treeWillExpand(e: TreeExpansionEvent) {
      val parent = e.path.lastPathComponent
      if (parent is DefaultMutableTreeNode) {
        val list = getTreeNodes(parent)
        parent.userObject = makeUserObject(parent, END_HEIGHT)
        list.forEach { it.userObject = makeUserObject(it, START_HEIGHT) }
        startExpandTimer(e, list)
      }
    }

    @Throws(ExpandVetoException::class)
    override fun treeWillCollapse(e: TreeExpansionEvent) {
      val path = e.path
      val o = path.lastPathComponent
      if (o is DefaultMutableTreeNode) {
        val list = getTreeNodes(o)
        if (list.any { (it.userObject as? SizeNode)?.height == END_HEIGHT }) {
          startCollapseTimer(e, list)
          throw ExpandVetoException(e)
        }
      }
    }
  })

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("Default", JScrollPane(JTree())))
    it.add(makeTitledPanel("Expand/Collapse Animations", JScrollPane(tree)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getTreeNodes(parent: DefaultMutableTreeNode) = parent
  .children()
  .toList()
  .filterIsInstance<DefaultMutableTreeNode>()

private fun makeUserObject(node: DefaultMutableTreeNode, height: Int): Any {
  val title = node.userObject.toString()
  return SizeNode(title, height)
}

private fun startExpandTimer(e: TreeExpansionEvent, list: List<DefaultMutableTreeNode>) {
  val tree = e.source as? JTree ?: return
  val model = tree.model
  val height = AtomicInteger(START_HEIGHT)
  Timer(DELAY) { ev ->
    val h = height.getAndIncrement()
    if (h <= END_HEIGHT) {
      list.forEach {
        val uo = makeUserObject(it, h)
        model.valueForPathChanged(TreePath(it.path), uo)
      }
    } else {
      (ev.source as? Timer)?.stop()
    }
  }.start()
}

private fun startCollapseTimer(
  e: TreeExpansionEvent,
  list: List<DefaultMutableTreeNode>,
) {
  val tree = e.source as? JTree ?: return
  val path = e.path
  val model = tree.model
  val height = AtomicInteger(END_HEIGHT)
  Timer(DELAY) { ev ->
    val h = height.getAndDecrement()
    if (h >= START_HEIGHT) {
      list.forEach { model.valueForPathChanged(TreePath(it.path), makeUserObject(it, h)) }
    } else {
      (ev.source as? Timer)?.stop()
      tree.collapsePath(path)
    }
  }.start()
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class HeightTreeCellRenderer : DefaultTreeCellRenderer() {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    val uo = (value as? DefaultMutableTreeNode)?.userObject
    if (c is JLabel && uo is SizeNode) {
      c.preferredSize = null
      c.text = uo.label
      c.preferredSize = c.preferredSize.also { it.height = uo.height }
    }
    return c
  }
}

private data class SizeNode(
  val label: String,
  val height: Int,
) {
  override fun toString() = label
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
