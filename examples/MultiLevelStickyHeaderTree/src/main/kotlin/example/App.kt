package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath
import kotlin.math.max

fun createUI(): Component {
  val tree = createTree()
  tree.setRootVisible(false)
  tree.setShowsRootHandles(false)
  tree.setRowHeight(StickyHeaderTreeLayerUI.HEADER_HEIGHT)
  expandAll(tree)

  val scroll = JScrollPane(tree)
  scroll.getVerticalScrollBar().setUnitIncrement(tree.getRowHeight())

  val layerUI = StickyHeaderTreeLayerUI()
  val layer = JLayer(scroll, layerUI)
  scroll.getViewport().addChangeListener { layer.repaint() }

  return JPanel(BorderLayout()).also {
    it.add(layer)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTree(): JTree {
  val root = createTreeNode("Root")
  val catNames = arrayOf(
    "Fruits",
    "Vegetables",
    "Meat/Fish",
    "Dairy products",
    "Grains/Bread",
  )
  val subNames = arrayOf(
    arrayOf("Domestic", "Imported", "Berries"),
    arrayOf("Root vegetables", "Leaf vegetables", "Fruit vegetables"),
    arrayOf("Meat", "Seafood"),
    arrayOf("Milk", "Cheese", "Other"),
    arrayOf("Rice", "Noodles", "Bread"),
  )
  val items = arrayOf(
    arrayOf(
      arrayOf("Apple", "Orange", "Peach", "Pear", "Grape", "Persimmon"),
      arrayOf("banana", "mango", "pineapple", "kiwi", "papaya"),
      arrayOf("Strawberry", "Blueberry", "Raspberry", "Cranberry"),
    ),
    arrayOf(
      arrayOf("Carrot", "Potato", "Sweet potato", "Radish", "Burdock"),
      arrayOf("Spinach", "Komatsuna", "Lettuce", "Cabbage", "Chinese cabbage"),
      arrayOf("Tomato", "Cucumber", "Eggplant", "Bell pepper", "Zucchini"),
    ),
    arrayOf(
      arrayOf("Chicken thigh", "Pork belly", "Beef loin", "Lamb", "Mixed ground"),
      arrayOf("Salmon", "Tuna", "Octopus", "Squid", "Shrimp", "Scallop"),
    ),
    arrayOf(
      arrayOf("Milk", "Skim milk", "Processed milk"),
      arrayOf("Camembert", "Gouda", "Mozzarella", "Parmesan"),
      arrayOf("Butter", "Yogurt", "Fresh cream", "Sour cream"),
    ),
    arrayOf(
      arrayOf("KOSHIHIKARI", "AKITAKOMACHI", "HITOMEBOTE", "BrownRice"),
      arrayOf("Udon", "Soba", "Pasta", "Ramen", "Somen"),
      arrayOf("Bread", "Baguette", "Croissant", "Bagel"),
    ),
  )

  for (c in catNames.indices) {
    val catNode = createTreeNode(catNames[c])
    for (s in subNames[c].indices) {
      val subNode = createTreeNode(subNames[c][s])
      for (item in items[c][s]) {
        subNode.add(createTreeNode(item))
      }
      catNode.add(subNode)
    }
    root.add(catNode)
  }
  return JTree(root)
}

private fun createTreeNode(name: String?) = DefaultMutableTreeNode(name)

private fun expandAll(tree: JTree) {
  var row = 0
  while (row < tree.getRowCount()) {
    tree.expandRow(row)
    row++
  }
}

@Suppress("ReturnCount")
private class StickyHeaderTreeLayerUI : LayerUI<JScrollPane>() {
  private val stickyPaths = mutableListOf<TreePath>()
  private val pushOffsets = mutableListOf<Int>()
  private val nextSiblingPaths = mutableListOf<TreePath>()

  private fun updateHeader(tree: JTree) {
    stickyPaths.clear()
    pushOffsets.clear()
    nextSiblingPaths.clear()

    val viewRect = tree.getVisibleRect()
    val topY = viewRect.y
    val topRow = tree.getClosestRowForLocation(viewRect.x, topY)
    if (topRow < 0) {
      return
    }

    val topPath = tree.getPathForRow(topRow) ?: return
    collectStickyPaths(tree, topPath)
    if (!stickyPaths.isEmpty()) {
      calculatePushOffsets(tree, viewRect.y)
    }
  }

  // Collect headers to be fixed from the ancestor chain from the top to topPath.
  private fun collectStickyPaths(tree: JTree, topPath: TreePath?) {
    var cur = topPath
    val m = tree.model
    while (cur != null) {
      val node = cur.lastPathComponent
      if (cur.parentPath != null && !m.isLeaf(node) && tree.isExpanded(cur)) {
        stickyPaths.add(0, cur)
      }
      cur = cur.parentPath
    }
  }

  private fun calculatePushOffsets(tree: JTree, topY: Int) {
    var inheritedOffset = 0
    for (i in stickyPaths.indices) {
      val sticky = stickyPaths[i]
      val nextSibling = findNextExpandedSibling(tree, sticky)?.also {
        nextSiblingPaths.add(it)
      }
      val stickyBaseY = i * HEADER_HEIGHT
      val localOffset = computeOverlap(tree, nextSibling, topY, stickyBaseY)
      val finalOffset = max(localOffset, inheritedOffset)
      pushOffsets.add(finalOffset)
      inheritedOffset = finalOffset
    }
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)

    val tree = getTree(c) ?: return
    updateHeader(tree)
    if (stickyPaths.isEmpty()) {
      return
    }

    val g2 = g.create() as? Graphics2D ?: return
    val toolkit = Toolkit.getDefaultToolkit()
    val hints = toolkit.getDesktopProperty("awt.font.desktophints")
    if (hints is MutableMap<*, *>) {
      g2.addRenderingHints(hints)
    }

    val scroll = (c as? JLayer<*>)?.getView() as? JScrollPane ?: return
    val viewport = scroll.getViewport().bounds
    val r = Rectangle(viewport.x, viewport.y, viewport.width, HEADER_HEIGHT)
    val n = stickyPaths.size

    for (i in n - 1 downTo 0) {
      val path = stickyPaths[i]
      val offset = pushOffsets[i]

      val baseY = viewport.y + i * HEADER_HEIGHT
      r.y = baseY - offset
      paintStickyHeader(g2, tree, path, r, depthIndex(path))

      if (offset > 0 && nextSiblingPaths.size > i) {
        val nextSibling = nextSiblingPaths[i]
        r.y = baseY + HEADER_HEIGHT - offset
        paintStickyHeader(g2, tree, nextSibling, r, depthIndex(nextSibling))
      }
    }
    g2.dispose()
  }

  companion object {
    const val HEADER_HEIGHT = 24
    private const val DEPTH = 4
    private val BORDER_COLOR = Color(0x46_00_00_00, true)

    private fun computeOverlap(
      tree: JTree,
      nextSibling: TreePath?,
      topY: Int,
      stickyBaseY: Int,
    ): Int {
      if (nextSibling == null) {
        return 0
      }

      val nextRow = tree.getRowForPath(nextSibling)
      val nr = tree.getRowBounds(nextRow) ?: return 0
      val nextTopInView = nr.y - topY
      val overlap = stickyBaseY + HEADER_HEIGHT - nextTopInView
      return overlap.coerceIn(0, HEADER_HEIGHT)
    }

    private fun findNextExpandedSibling(tree: JTree, path: TreePath): TreePath? {
      val parentPath = path.parentPath ?: return null
      val parent = parentPath.lastPathComponent
      val current = path.lastPathComponent
      val model = tree.model
      val childCount = model.getChildCount(parent)
      var found = false
      for (i in 0..<childCount) {
        val child = model.getChild(parent, i)
        if (found) {
          val siblingPath = parentPath.pathByAddingChild(child)
          if (!model.isLeaf(child) && tree.isExpanded(siblingPath)) {
            return siblingPath
          }
        }
        if (child == current) {
          found = true
        }
      }
      return null
    }

    fun paintStickyHeader(
      g2: Graphics2D,
      tree: JTree,
      path: TreePath,
      r: Rectangle,
      depthIdx: Int,
    ) {
      val oldClip = g2.clip
      g2.clip = r
      g2.paint = Color.LIGHT_GRAY
      g2.fill(r)

      g2.color = BORDER_COLOR
      g2.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1)

      val renderer = tree.getCellRenderer()
      val node = path.lastPathComponent
      val row = tree.getRowForPath(path)
      val sel = tree.isPathSelected(path)
      val exp = tree.isExpanded(path)
      val leaf = tree.model.isLeaf(node)

      val c = renderer.getTreeCellRendererComponent(
        tree,
        node,
        sel,
        exp,
        leaf,
        row,
        false,
      )

      if (c is JLabel) {
        val label = cloneLabel(c)
        val leftChildIndent = UIManager.getInt("Tree.leftChildIndent")
        val rightChildIndent = UIManager.getInt("Tree.rightChildIndent")
        val indent = depthIdx * (leftChildIndent + rightChildIndent)
        label.setSize(r.width - indent, r.height)
        val rect = Rectangle(r.x + indent, r.y, r.width - indent, r.height)
        SwingUtilities.paintComponent(g2, label, JPanel(), rect)
        // Lower border
        g2.color = BORDER_COLOR
        g2.drawLine(r.x, r.y + r.height - 1, r.x + r.width - 1, r.y + r.height - 1)

      }
      g2.clip = oldClip
    }

    private fun cloneLabel(src: JLabel): JLabel {
      val label = JLabel(src.text)
      label.setIcon(src.icon)
      label.setIconTextGap(src.iconTextGap)
      label.setOpaque(true)
      label.setBackground(Color.LIGHT_GRAY)
      return label
    }

    private fun depthIndex(path: TreePath): Int {
      val index = path.getPathCount() - 2
      val max = DEPTH - 1
      return index.coerceIn(0, max)
    }

    private fun getTree(c: JComponent?) = c
      ?.let { it as? JLayer<*> }
      ?.view
      ?.let { it as? JScrollPane }
      ?.viewport
      ?.view
      ?.let { it as? JTree }
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
