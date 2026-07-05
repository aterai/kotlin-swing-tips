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
  tree.setShowsRootHandles(true)
  tree.setRowHeight(24)
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
    val w = viewport.width
    val n = stickyPaths.size

    for (i in n - 1 downTo 0) {
      val path = stickyPaths[i]
      val offset = pushOffsets[i]

      val baseY = viewport.y + i * HEADER_HEIGHT
      val y = baseY - offset

      val depthIdx = depthIndex(path)
      paintStickyHeader(g2, tree, c, path, viewport.x, y, w, HEADER_HEIGHT, depthIdx)

      if (offset > 0 && nextSiblingPaths.size > i) {
        val nextSibling = nextSiblingPaths[i]
        val nextY = baseY + HEADER_HEIGHT - offset
        paintStickyHeader(
          g2,
          tree,
          c,
          nextSibling,
          viewport.x,
          nextY,
          w,
          HEADER_HEIGHT,
          depthIndex(nextSibling),
        )
      }
    }
    g2.dispose()
  }

  companion object {
    private const val HEADER_HEIGHT = 24
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
      layer: JComponent?,
      path: TreePath,
      x: Int,
      y: Int,
      w: Int,
      h: Int,
      depthIdx: Int,
    ) {
      val oldClip = g2.clip
      g2.setClip(x, y, w, h)
      g2.paint = Color.LIGHT_GRAY
      g2.fillRect(x, y, w, h)

      g2.color = BORDER_COLOR
      g2.drawLine(x, y + h - 1, x + w - 1, y + h - 1)

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

      val indent = depthIdx * 10

      if (c is JLabel) {
        c.setOpaque(false)
        val icon = c.icon
        var iconW = 0
        val iconX = x + 6 + indent
        if (icon != null) {
          val iconY = y + (h - icon.iconHeight) / 2
          icon.paintIcon(layer, g2, iconX, iconY)
          iconW = icon.iconWidth + 4
        }

        val text = c.text
        if (text != null && !text.isEmpty()) {
          val fm = g2.fontMetrics
          val textX = iconX + iconW
          val textY = y + (h + fm.ascent - fm.descent) / 2
          g2.color = UIManager.getColor("Tree.foreground")
          g2.drawString(text, textX, textY)
        }
      } else {
        val tmp = JPanel()
        c.setSize(w - indent, h)
        val rect = Rectangle(x + indent, y, w - indent, h)
        SwingUtilities.paintComponent(g2, c, tmp, rect)
      }
      g2.clip = oldClip
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
