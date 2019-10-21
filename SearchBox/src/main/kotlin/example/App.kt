package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionEvent
import java.awt.event.KeyEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

class MainPanel : JPanel(BorderLayout()) {
  init {
    val field = JTextField("asd", 10)
    val tree = JTree(makeModel())
    val findNextAction = object : AbstractAction("Find Next") {
      private val rollOverPathLists = mutableListOf<TreePath>()
      override fun actionPerformed(e: ActionEvent) {
        val selectedPath = tree.getSelectionPath()
        tree.clearSelection()
        rollOverPathLists.clear()
        TreeUtil.searchTree(tree, tree.getPathForRow(0), field.getText(), rollOverPathLists)
        if (rollOverPathLists.isNotEmpty()) {
          val nextIdx = getNextIndex(rollOverPathLists, selectedPath)
          val p = rollOverPathLists[nextIdx]
          tree.addSelectionPath(p)
          tree.scrollPathToVisible(p)
        }
      }
    }
    val button = JButton()
    button.setAction(findNextAction)
    button.setFocusable(false)
    field.getActionMap().put("find-next", findNextAction)
    field.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "find-next")
    val controls = JPanel()
    val layout = ControlPanelLayout(controls, 5, 5)
    controls.setLayout(layout)
    controls.setBorder(BorderFactory.createTitledBorder("Search down"))
    controls.add(JLabel("Find what:"), BorderLayout.WEST)
    controls.add(field)
    controls.add(button, BorderLayout.EAST)
    val showHideButton = JButton()
    showHideButton.setAction(layout.showHideAction)
    showHideButton.setFocusable(false)
    val modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()
    // Java 10: val modifiers = Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()
    val im = getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
    im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifiers), "open-search-box")
    getActionMap().put("open-search-box", layout.showHideAction)
    add(controls, BorderLayout.NORTH)
    add(JScrollPane(tree))
    add(showHideButton, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }

  private fun getNextIndex(list: List<TreePath>, selected: TreePath?): Int {
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
}

class ControlPanelLayout(private val controls: Container, hgap: Int, vgap: Int) : BorderLayout(hgap, vgap) {
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

  override fun preferredLayoutSize(target: Container): Dimension {
    // synchronized (target.getTreeLock()) {
    val ps = super.preferredLayoutSize(target)
    val controlsPreferredHeight = ps.height
    if (animator.isRunning()) {
      if (isHidden) {
        if (controls.getHeight() < controlsPreferredHeight) {
          controlsHeight += 5
        }
      } else {
        if (controls.getHeight() > 0) {
          controlsHeight -= 5
        }
      }
      if (controlsHeight <= 0) {
        controlsHeight = 0
        animator.stop()
      } else if (controlsHeight >= controlsPreferredHeight) {
        controlsHeight = controlsPreferredHeight
        animator.stop()
      }
    }
    ps.height = controlsHeight
    return ps
  }

  init {
    animator.addActionListener { controls.revalidate() }
  }
}

object TreeUtil {
  fun searchTree(tree: JTree, path: TreePath, q: String, rollOverPathLists: MutableList<TreePath>) {
    val node = path.getLastPathComponent()
    if (node is TreeNode) {
      if (node.toString().startsWith(q)) {
        rollOverPathLists.add(path)
        tree.expandPath(path.getParentPath())
      }
      if (!node.isLeaf) {
        node.children().toList()
          .forEach { searchTree(tree, path.pathByAddingChild(it), q, rollOverPathLists) }
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
