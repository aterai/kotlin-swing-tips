package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.Timer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val field = JTextField("asd", 10)
  val tree = JTree(makeModel())
  val findNextAction = object : AbstractAction("Find Next") {
    private val rollOverPathLists = mutableListOf<TreePath>()

    override fun actionPerformed(e: ActionEvent) {
      val selectedPath = tree.selectionPath
      tree.clearSelection()
      rollOverPathLists.clear()
      TreeUtils.searchTree(tree, tree.getPathForRow(0), field.text, rollOverPathLists)
      if (rollOverPathLists.isNotEmpty()) {
        val nextIdx = getNextIndex(rollOverPathLists, selectedPath)
        val p = rollOverPathLists[nextIdx]
        tree.addSelectionPath(p)
        tree.scrollPathToVisible(p)
      }
    }
  }
  val button = JButton()
  button.action = findNextAction
  button.isFocusable = false
  field.actionMap.put("find-next", findNextAction)
  val imap = field.getInputMap(JComponent.WHEN_FOCUSED)
  imap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "find-next")

  val controls = JPanel()
  val layout = ControlPanelLayout(controls, 5, 5)
  controls.layout = layout
  controls.border = BorderFactory.createTitledBorder("Search down")
  controls.add(JLabel("Find what:"), BorderLayout.WEST)
  controls.add(field)
  controls.add(button, BorderLayout.EAST)
  val showHideButton = JButton()
  showHideButton.action = layout.showHideAction
  showHideButton.isFocusable = false

  return JPanel(BorderLayout()).also {
    val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask
    // Java 10: val modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
    val im = it.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifiers), "open-search-box")
    it.actionMap.put("open-search-box", layout.showHideAction)
    it.add(controls, BorderLayout.NORTH)
    it.add(JScrollPane(tree))
    it.add(showHideButton, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun getNextIndex(
  list: List<TreePath>,
  selected: TreePath?,
): Int {
  for ((i, tp) in list.withIndex()) {
    if (tp == selected) {
      return if (i + 1 < list.size) i + 1 else 0
    }
  }
  return 0
}

private fun makeModel(): DefaultTreeModel {
  val set1 = DefaultMutableTreeNode("Set 001")
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(DefaultMutableTreeNode("33333"))
  val set2 = DefaultMutableTreeNode("Set 002")
  set2.add(DefaultMutableTreeNode("asd fas df as"))
  set2.add(DefaultMutableTreeNode("as df"))
  val set3 = DefaultMutableTreeNode("Set 003")
  set3.add(DefaultMutableTreeNode("asd fas dfa sdf"))
  set3.add(DefaultMutableTreeNode("qwe rqw er"))
  set3.add(DefaultMutableTreeNode("zvx cvz xcv zxz xcv zx cv"))
  val root = DefaultMutableTreeNode("Root")
  root.add(set1)
  root.add(set2)
  set2.add(set3)
  return DefaultTreeModel(root)
}

private class ControlPanelLayout(
  private val controls: Container,
  horizontalGap: Int,
  verticalGap: Int,
) : BorderLayout(horizontalGap, verticalGap) {
  private var isHidden = true
  private val animator = Timer(5, null)
  private var controlsHeight = 0
  val showHideAction = object : AbstractAction("Show/Hide Search Box") {
    override fun actionPerformed(e: ActionEvent) {
      if (!animator.isRunning) {
        isHidden = controls.height == 0
        animator.start()
      }
    }
  }

  init {
    animator.addActionListener { controls.revalidate() }
  }

  override fun preferredLayoutSize(target: Container): Dimension {
    // synchronized (target.getTreeLock()) {
    val ps = super.preferredLayoutSize(target)
    val defaultHeight = ps.height
    if (animator.isRunning) {
      if (isHidden) {
        if (controls.height < defaultHeight) {
          controlsHeight += 5
        }
      } else {
        if (controls.height > 0) {
          controlsHeight -= 5
        }
      }
      if (controlsHeight <= 0) {
        controlsHeight = 0
        animator.stop()
      } else if (controlsHeight >= defaultHeight) {
        controlsHeight = defaultHeight
        animator.stop()
      }
    }
    ps.height = controlsHeight
    return ps
  }
}

private object TreeUtils {
  fun searchTree(
    tree: JTree,
    path: TreePath,
    q: String,
    rollOverPathLists: MutableList<TreePath>,
  ) {
    val node = path.lastPathComponent
    if (node is TreeNode) {
      if (node.toString().startsWith(q)) {
        rollOverPathLists.add(path)
        tree.expandPath(path.parentPath)
      }
      // if (!node.isLeaf) {
      for (c in node.children()) {
        searchTree(tree, path.pathByAddingChild(c), q, rollOverPathLists)
      }
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
