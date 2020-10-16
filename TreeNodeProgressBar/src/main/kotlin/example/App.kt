package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicProgressBarUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val tree = object : JTree(DefaultTreeModel(makeTreeRoot())) {
    override fun updateUI() {
      super.updateUI()
      setCellRenderer(ProgressBarRenderer())
      setRowHeight(-1)
    }
  }

  val button = JButton("start")
  button.addActionListener { e ->
    val b = e.source as JButton
    b.isEnabled = false
    val executor = Executors.newCachedThreadPool()
    object : SwingWorker<Boolean, Void?>() {
      @Throws(InterruptedException::class)
      override fun doInBackground(): Boolean {
        val model = tree.model as DefaultTreeModel
        val root = model.root as DefaultMutableTreeNode
        root.breadthFirstEnumeration().toList()
          .filterIsInstance<DefaultMutableTreeNode>()
          .filter { root != it && !model.isLeaf(it) }
          .forEach { executor.execute(makeWorker(tree, it)) }
        executor.shutdown()
        return executor.awaitTermination(1, TimeUnit.MINUTES)
      }

      private fun makeWorker(
        tree: JTree,
        node: DefaultMutableTreeNode
      ): SwingWorker<TreeNode, Int> {
        return NodeProgressWorker(tree, node)
      }

      override fun done() {
        b.isEnabled = true
      }
    }.execute()
  }

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(tree))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTreeRoot(): DefaultMutableTreeNode {
  val set1 = DefaultMutableTreeNode("Set 001")
  val set4 = DefaultMutableTreeNode("Set 004")
  set1.add(DefaultMutableTreeNode("3333333333333333"))
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(set4)
  set1.add(DefaultMutableTreeNode("222222"))
  set1.add(DefaultMutableTreeNode("222222222"))

  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("4444444444444"))
  set2.add(DefaultMutableTreeNode("5555555"))

  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("zzz"))
  set3.add(DefaultMutableTreeNode("aaa"))
  set3.add(DefaultMutableTreeNode("ccc"))
  set4.add(DefaultMutableTreeNode("22222222222"))
  set4.add(DefaultMutableTreeNode("ddd"))
  set4.add(DefaultMutableTreeNode("6666666"))
  set4.add(DefaultMutableTreeNode("eee"))

  val root = DefaultMutableTreeNode("Root")
  root.add(DefaultMutableTreeNode("9999999"))
  root.add(set3)
  root.add(DefaultMutableTreeNode("888888888"))
  root.add(set1)
  root.add(set2)
  root.add(DefaultMutableTreeNode("222222222222"))
  root.add(DefaultMutableTreeNode("777777"))
  return root
}

private class NodeProgressWorker(
  private val tree: JTree,
  private val treeNode: DefaultMutableTreeNode
) : SwingWorker<TreeNode, Int>() {
  private val lengthOfTask = 120
  private val model = tree.model as DefaultTreeModel

  @Throws(InterruptedException::class)
  override fun doInBackground(): TreeNode {
    var current = 0
    while (current <= lengthOfTask && !isCancelled) {
      doSomething()
      publish(100 * current++ / lengthOfTask)
    }
    return treeNode
  }

  @Throws(InterruptedException::class)
  private fun doSomething() {
    Thread.sleep((1..100).random().toLong())
  }

  override fun process(c: List<Int>) {
    val title = treeNode.userObject.toString()
    val i = c[c.size - 1]
    val o = ProgressObject(title, i)
    treeNode.userObject = o
    model.nodeChanged(treeNode)
  }

  override fun done() {
    try {
      val n = get()
      tree.expandPath(TreePath(model.getPathToRoot(n)))
    } catch (ex: InterruptedException) {
      Toolkit.getDefaultToolkit().beep()
      Thread.currentThread().interrupt()
    } catch (ex: ExecutionException) {
      ex.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
  }
}

private class ProgressBarRenderer : DefaultTreeCellRenderer() {
  private val progress = object : JProgressBar() {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = BAR_HEIGHT
    }

    override fun updateUI() {
      super.updateUI()
      setUI(BasicProgressBarUI())
      isStringPainted = true
      string = ""
      isOpaque = false
      border = BorderFactory.createEmptyBorder()
    }
  }
  private val renderer = object : JPanel(BorderLayout()) {
    override fun updateUI() {
      super.updateUI()
      isOpaque = false
    }
  }

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    var c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val o = (value as? DefaultMutableTreeNode)?.userObject
    if (o is ProgressObject) {
      val i = o.value
      progress.value = i

      renderer.removeAll()
      renderer.add(c)
      if (i < progress.maximum) {
        renderer.add(progress, BorderLayout.SOUTH)
      }
      c = renderer
    }
    return c
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    it.height = 18
  }

  companion object {
    const val BAR_HEIGHT = 4
  }
}

private data class ProgressObject(val title: String, val value: Int) {
  override fun toString() = title
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
