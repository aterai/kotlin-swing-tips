package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.Timer
import javax.swing.event.AncestorEvent
import javax.swing.event.AncestorListener
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath

fun makeUI(): Component {
  val searchBox = JPanel(BorderLayout())
  val handler = LayoutAnimator(searchBox)
  val p = object : JPanel(handler) {
    override fun isOptimizedDrawingEnabled() = false
  }
  p.add(searchBox, BorderLayout.NORTH)

  val tree = JTree()
  tree.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val scroll = JScrollPane(tree)
  scroll.border = BorderFactory.createTitledBorder("Find... Ctrl+F")
  p.add(scroll)

  val field = object : JTextField("b", 10) {
    private var listener: AncestorListener? = null

    override fun updateUI() {
      removeAncestorListener(listener)
      super.updateUI()
      listener = FocusAncestorListener()
      addAncestorListener(listener)
    }
  }

  val findNextAction = FindNextAction(tree, field)
  val button = JButton(findNextAction)
  button.isFocusable = false
  button.toolTipText = "Find next"
  button.text = "v"

  searchBox.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  searchBox.add(field)
  searchBox.add(button, BorderLayout.EAST)
  searchBox.isVisible = false

  val animator = Timer(5, handler)
  val modifiers = Toolkit.getDefaultToolkit().menuShortcutKeyMask

  val im = p.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F, modifiers), "open-search-box")
  im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "close-search-box")
  val a1 = object : AbstractAction("Show/Hide Search Box") {
    override fun actionPerformed(e: ActionEvent) {
      if (!animator.isRunning) {
        handler.isShowing = !searchBox.isVisible
        searchBox.isVisible = true
        animator.start()
      }
    }
  }
  p.actionMap.put("open-search-box", a1)

  val a2 = object : AbstractAction("Hide Search Box") {
    override fun actionPerformed(e: ActionEvent) {
      if (!animator.isRunning) {
        handler.isShowing = false
        animator.start()
      }
    }
  }
  p.actionMap.put("close-search-box", a2)

  field.actionMap.put("find-next", findNextAction)
  val im2 = field.getInputMap(JComponent.WHEN_FOCUSED)
  im2.put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "find-next")

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.preferredSize = Dimension(320, 240)
  }
}

private class LayoutAnimator(
  private val component: JComponent,
) : BorderLayout(),
  ActionListener {
  var isShowing = true
  private var yy = 0
  private var counter = 0

  override fun actionPerformed(e: ActionEvent) {
    val height = component.preferredSize.height
    if (isShowing) {
      yy = (.5 + AnimationUtils.easeInOut(++counter / height.toDouble()) * height).toInt()
      if (yy >= height) {
        yy = height
        (e.source as? Timer)?.stop()
      }
    } else {
      yy = (.5 + AnimationUtils.easeInOut(--counter / height.toDouble()) * height).toInt()
      if (yy <= 0) {
        yy = 0
        (e.source as? Timer)?.stop()
        component.isVisible = false
      }
    }
    component.revalidate()
  }

  override fun layoutContainer(parent: Container) {
    synchronized(parent.treeLock) {
      val insets = parent.insets
      val width = parent.width
      val height = parent.height
      val top = insets.top
      val bottom = height - insets.bottom
      val left = insets.left
      val right = width - insets.right
      val nc = getLayoutComponent(parent, NORTH)
      if (nc != null) {
        val d = nc.preferredSize
        val vsw = UIManager.getInt("ScrollBar.width")
        nc.setBounds(right - d.width - vsw, yy - d.height, d.width, d.height)
      }
      val cc = getLayoutComponent(parent, CENTER)
      cc?.setBounds(left, top, right - left, bottom - top)
    }
  }
}

private class FocusAncestorListener : AncestorListener {
  override fun ancestorAdded(e: AncestorEvent) {
    e.component.requestFocusInWindow()
  }

  override fun ancestorMoved(e: AncestorEvent) {
    // not needed
  }

  override fun ancestorRemoved(e: AncestorEvent) {
    // not needed
  }
}

private class FindNextAction(
  private val tree: JTree,
  private val field: JTextField,
) : AbstractAction() {
  private val rollOverPathLists = mutableListOf<TreePath>()

  override fun actionPerformed(e: ActionEvent) {
    val selectedPath = tree.selectionPath
    tree.clearSelection()
    rollOverPathLists.clear()
    searchTree(tree, tree.getPathForRow(0), field.text, rollOverPathLists)
    if (rollOverPathLists.isEmpty()) {
      return
    }
    var nextIndex = 0
    val size = rollOverPathLists.size
    for (i in 0..<size) {
      if (rollOverPathLists[i] == selectedPath) {
        nextIndex = if (i + 1 < size) i + 1 else 0
        break
      }
    }
    val p = rollOverPathLists[nextIndex]
    tree.addSelectionPath(p)
    tree.scrollPathToVisible(p)
  }
}

private fun searchTree(
  tree: JTree,
  path: TreePath,
  q: String,
  rollOverPathLists: MutableList<TreePath>,
) {
  (path.lastPathComponent as? TreeNode)?.also { node ->
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

private object AnimationUtils {
  private const val N = 3

  fun easeInOut(t: Double): Double {
    val isFirstHalf = t < .5
    return if (isFirstHalf) {
      .5 * intPow(t * 2.0, N)
    } else {
      .5 * (intPow(t * 2.0 - 2.0, N) + 2.0)
    }
  }

  fun intPow(
    base0: Double,
    exp0: Int,
  ): Double {
    require(exp0 >= 0) { "exp0 must be a positive integer or zero" }
    var base = base0
    var exp = exp0
    var result = 1.0
    while (exp > 0) {
      if (exp and 1 != 0) {
        result *= base
      }
      base *= base
      exp = exp ushr 1
    }
    return result
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
