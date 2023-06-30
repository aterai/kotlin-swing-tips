package example

import java.awt.*
import java.awt.event.ActionListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.MutableTreeNode

private val root = TreeUtils.makeTreeRoot()
private val tree = JTree(DefaultTreeModel(TreeUtils.makeTreeRoot()))
private val sort1 = JRadioButton("1: bubble sort")
private val sort2 = JRadioButton("2: selection sort")
private val sort3 = JRadioButton("3: TimSort") // JDK 1.7.0
private val reset = JRadioButton("reset")

fun makeUI(): Component {
  val box = JPanel(GridLayout(2, 2))
  val listener = ActionListener { e ->
    val check = e.source
    if (check is JRadioButton) {
      if (check == reset) {
        tree.model = DefaultTreeModel(root)
      } else {
        TreeUtils.COMPARE_COUNTER.set(0)
        TreeUtils.SWAP_COUNTER.set(0)
        // val r = TreeUtils.deepCopyTree(root, root.clone() as DefaultMutableTreeNode)
        val r = TreeUtils.deepCopyTree(root, DefaultMutableTreeNode(root.userObject))
        when (check) {
          sort1 -> TreeUtils.sortTree1(r)
          sort2 -> TreeUtils.sortTree2(r)
          else -> TreeUtils.sortTree3(r)
        }
        swapCounter(check)
        tree.model = DefaultTreeModel(r)
      }
      TreeUtils.expandAll(tree)
    }
  }
  val bg = ButtonGroup()
  listOf(reset, sort1, sort2, sort3).forEach {
    box.add(it)
    bg.add(it)
    it.addActionListener(listener)
  }

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.SOUTH)

    val p = JPanel(BorderLayout())
    p.border = BorderFactory.createTitledBorder("Sort JTree")
    p.add(JScrollPane(tree))
    it.add(p)
    TreeUtils.expandAll(tree)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun swapCounter(radio: JRadioButton) {
  val title = radio.text
  if (TreeUtils.SWAP_COUNTER.get() == 0) {
    val cc = TreeUtils.COMPARE_COUNTER.get()
    radio.toolTipText = "%-24s - compare: %3d, swap: ---%n".format(title, cc)
  } else {
    val cc = TreeUtils.COMPARE_COUNTER.get()
    val sc = TreeUtils.SWAP_COUNTER.get()
    radio.toolTipText = "%-24s - compare: %3d, swap: %3d%n".format(title, cc, sc)
  }
}

private object TreeUtils {
  val COMPARE_COUNTER = AtomicInteger()
  val SWAP_COUNTER = AtomicInteger()

  // private val tnc = Comparator.comparing(Function<DefaultMutableTreeNode, Boolean> {
  //   it.isLeaf()
  // })
  // private val tnc = Comparator.comparing<DefaultMutableTreeNode, Boolean> { it.isLeaf() }
  //     .thenComparing { n -> n.getUserObject().toString() }
  private val tnc = compareBy<DefaultMutableTreeNode> { it.isLeaf }
    .thenBy { it.userObject.toString() }

  fun sortTree1(root: DefaultMutableTreeNode) {
    val n = root.childCount
    for (i in 0 until n - 1) {
      for (j in n - 1 downTo i + 1) {
        val curNode = root.getChildAt(j) as? DefaultMutableTreeNode
        val prevNode = root.getChildAt(j - 1) as? DefaultMutableTreeNode
        if (prevNode != null && !prevNode.isLeaf) {
          sortTree1(prevNode)
        }
        if (tnc.compare(prevNode, curNode) > 0) {
          SWAP_COUNTER.getAndIncrement()
          root.insert(curNode, j - 1)
          root.insert(prevNode, j)
        }
      }
    }
  }

  private fun sort2(parent: DefaultMutableTreeNode) {
    val n = parent.childCount
    for (i in 0 until n - 1) {
      var min = i
      for (j in i + 1 until n) {
        val a = parent.getChildAt(min) as? DefaultMutableTreeNode
        val b = parent.getChildAt(j) as? DefaultMutableTreeNode
        if (tnc.compare(a, b) > 0) {
          min = j
        }
      }
      if (i != min) {
        SWAP_COUNTER.getAndIncrement()
        val a = parent.getChildAt(i) as? MutableTreeNode
        val b = parent.getChildAt(min) as? MutableTreeNode
        parent.insert(b, i)
        parent.insert(a, min)
      }
    }
  }

  fun sortTree2(parent: DefaultMutableTreeNode) {
    parent.preorderEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filterNot { it.isLeaf }
      .forEach { sort2(it) }
  }

  private fun sort3(parent: DefaultMutableTreeNode) {
    val children = parent.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .sortedWith(tnc)
    // .sortedWith(compareBy(DefaultMutableTreeNode::isLeaf, { it.getUserObject().toString() }))
    parent.removeAllChildren()
    children.forEach { parent.add(it) }
  }

  fun sortTree3(parent: DefaultMutableTreeNode) {
    parent.preorderEnumeration().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filterNot { it.isLeaf }
      .forEach { sort3(it) }
  }

  fun deepCopyTree(
    src: DefaultMutableTreeNode,
    tgt: DefaultMutableTreeNode
  ): DefaultMutableTreeNode {
    src.children().toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .forEach {
        val clone = DefaultMutableTreeNode(it.userObject)
        tgt.add(clone)
        if (!it.isLeaf) {
          deepCopyTree(it, clone)
        }
      }
    return tgt
  }

  fun makeTreeRoot(): DefaultMutableTreeNode {
    val set1 = DefaultMutableTreeNode("Set 001")
    val set4 = DefaultMutableTreeNode("Set 004")
    set1.add(DefaultMutableTreeNode("3333333333333333"))
    set1.add(DefaultMutableTreeNode("111111111"))
    set1.add(DefaultMutableTreeNode("22222222222"))
    set1.add(set4)
    set1.add(DefaultMutableTreeNode("222222"))
    set1.add(DefaultMutableTreeNode("222222222"))

    val set2 = DefaultMutableTreeNode("Set 002")
    set2.add(DefaultMutableTreeNode("eee eee eee ee"))
    set2.add(DefaultMutableTreeNode("bbb bbb bbb"))

    val set3 = DefaultMutableTreeNode("Set 003")
    set3.add(DefaultMutableTreeNode("zzz zzz"))
    set3.add(DefaultMutableTreeNode("aaa aaa aaa aaa"))
    set3.add(DefaultMutableTreeNode("ccc ccc ccc"))

    set4.add(DefaultMutableTreeNode("22222222222"))
    set4.add(DefaultMutableTreeNode("eee eee eee ee ee"))
    set4.add(DefaultMutableTreeNode("bbb bbb bbb bbb"))
    set4.add(DefaultMutableTreeNode("zzz zzz"))

    val root = DefaultMutableTreeNode("Root")
    root.add(DefaultMutableTreeNode("xxx xxx xxx xx xx"))
    root.add(set3)
    root.add(DefaultMutableTreeNode("eee eee eee ee ee"))
    root.add(set1)
    root.add(set2)
    root.add(DefaultMutableTreeNode("222222222222"))
    root.add(DefaultMutableTreeNode("bbb bbb bbb bbb"))
    return root
  }

  fun expandAll(tree: JTree) {
    var row = 0
    while (row < tree.rowCount) {
      tree.expandRow(row++)
    }
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
