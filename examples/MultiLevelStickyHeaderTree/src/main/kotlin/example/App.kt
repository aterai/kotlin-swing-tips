package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.LayerUI
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreePath

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

  // Y-coordinate in the viewport of the nearest "next expanded sibling" of the
  // sticky headers, or Int.MAX_VALUE if there is none. The sticky headers
  // are never drawn below it, so the JTree's own rows are visible from there.
  private var pushLimit = Int.MAX_VALUE

  private fun updateHeader(tree: JTree) {
    stickyPaths.clear()
    pushOffsets.clear()
    pushLimit = Int.MAX_VALUE

    val viewRect = tree.getVisibleRect()
    val topY = viewRect.y
    val topRow = tree.getClosestRowForLocation(viewRect.x, topY)
    if (topRow < 0) {
      return
    }

    val topPath = tree.getPathForRow(topRow) ?: return
    collectStickyPaths(tree, topPath)
    if (stickyPaths.isNotEmpty()) {
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
    // The top of the nearest "next expanded sibling" of this header and of its
    // ancestors: the next sibling of an ancestor also ends the subtree of this
    // header, so it must push this header up as well.
    var limit = Int.MAX_VALUE
    for (i in stickyPaths.indices) {
      limit = minOf(limit, getNextSiblingTop(tree, stickyPaths[i], topY))
      // The amount that the sibling encroaches on the current header area.
      val overlap = (i + 1) * HEADER_HEIGHT - limit
      pushOffsets.add(overlap.coerceIn(0, HEADER_HEIGHT))
    }
    pushLimit = limit
  }

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)

    val tree = getTree(c) ?: return
    updateHeader(tree)
    if (stickyPaths.isEmpty()) {
      return
    }

    val scroll = (c as? JLayer<*>)?.getView() as? JScrollPane ?: return
    val g2 = g.create() as? Graphics2D ?: return
    val toolkit = Toolkit.getDefaultToolkit()
    val hints = toolkit.getDesktopProperty("awt.font.desktophints")
    if (hints is Map<*, *>) {
      g2.addRenderingHints(hints)
    }

    val viewport = scroll.getViewport().bounds
    g2.clip(viewport)
    val n = stickyPaths.size

    // The innermost header is always the lowest one, even while it is pushed up.
    // The rows from the pushing sibling downwards are the JTree's own rows,
    // so the sticky header area must end at the top of that sibling.
    val bottom = minOf(n * HEADER_HEIGHT - pushOffsets[n - 1], pushLimit)

    val g3 = g2.create() as Graphics2D
    g3.clipRect(viewport.x, viewport.y, viewport.width, bottom)
    val r = Rectangle(viewport.x, viewport.y, viewport.width, HEADER_HEIGHT)
    // Draw from the bottom header up, overlaying upper headers on top (z-index)
    for (i in n - 1 downTo 0) {
      val path = stickyPaths[i]
      r.y = viewport.y + i * HEADER_HEIGHT - pushOffsets[i]
      paintStickyHeader(g3, tree, path, r, depthIndex(path))
    }
    g3.dispose()

    paintShadow(g2, viewport, viewport.y + bottom)
    g2.dispose()
  }

  companion object {
    const val HEADER_HEIGHT = 24
    private const val SHADOW_HEIGHT = 6
    private const val DEPTH = 4
    private val SHADOW_COLOR = Color(0x40_00_00_00, true)
    private val TRANSPARENT = Color(0x0, true)

    // Returns the Y-coordinate in the viewport of the "next expanded sibling"
    // of the specified TreePath, or Int.MAX_VALUE if there is none.
    private fun getNextSiblingTop(tree: JTree, path: TreePath, topY: Int) =
      findNextExpandedSibling(tree, path)
        ?.let { tree.getRowBounds(tree.getRowForPath(it)) }
        ?.let { it.y - topY }
        ?: Int.MAX_VALUE

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

    // Draw one fixed header with the same background as the JTree
    fun paintStickyHeader(
      g: Graphics2D,
      tree: JTree,
      path: TreePath,
      r: Rectangle,
      depthIdx: Int,
    ) {
      val g2 = g.create() as Graphics2D
      g2.clip(r)
      g2.paint = tree.background
      g2.fill(r)

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
        val label = cloneLabel(c, tree)
        val leftChildIndent = UIManager.getInt("Tree.leftChildIndent")
        val rightChildIndent = UIManager.getInt("Tree.rightChildIndent")
        val indent = depthIdx * (leftChildIndent + rightChildIndent)
        label.setSize(r.width - indent, r.height)
        val rect = Rectangle(r.x + indent, r.y, r.width - indent, r.height)
        SwingUtilities.paintComponent(g2, label, JPanel(), rect)
      }
      g2.dispose()
    }

    // Draw a gradient shadow below the sticky headers instead of border lines.
    private fun paintShadow(g: Graphics2D, area: Rectangle, y: Int) {
      val y0 = y.toFloat()
      val y1 = y0 + SHADOW_HEIGHT
      g.paint = GradientPaint(0f, y0, SHADOW_COLOR, 0f, y1, TRANSPARENT)
      g.fillRect(area.x, y, area.width, SHADOW_HEIGHT)
    }

    private fun cloneLabel(src: JLabel, tree: JTree): JLabel {
      val label = JLabel(src.text)
      label.setIcon(src.icon)
      label.setIconTextGap(src.iconTextGap)
      label.setOpaque(true)
      label.setBackground(tree.background)
      label.setForeground(tree.foreground)
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
