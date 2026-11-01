package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeModel
import javax.swing.tree.TreePath
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

fun createUI(): Component {
  val tree = JTree()
  tree.isRootVisible = false
  tree.showsRootHandles = true
  val model = tree.model
  (model.root as? DefaultMutableTreeNode)
    ?.preorderEnumeration()
    ?.toList()
    ?.filterIsInstance<DefaultMutableTreeNode>()
    ?.filter { it.isLeaf }
    ?.forEach {
      val title = it.userObject?.toString() ?: ""
      it.userObject = WeightedItem(title, 10 + Random.nextInt(180))
    }

  val scroll = JScrollPane(tree)
  scroll.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  val treemap = TreemapPanel(model)
  treemap.background = Color.WHITE
  treemap.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

  tree.addTreeSelectionListener { e ->
    treemap.setSelectionPath(e.newLeadSelectionPath)
  }
  treemap.addMouseListener(object : MouseAdapter() {
    override fun mouseClicked(e: MouseEvent) {
      val path = treemap.getPathAt(e.point)
      if (path == null) {
        tree.clearSelection()
      } else {
        tree.selectionPath = path
        tree.scrollPathToVisible(path)
      }
    }
  })

  return JPanel(BorderLayout()).also {
    it.add(scroll, BorderLayout.WEST)
    it.add(treemap)
    it.preferredSize = Dimension(320, 240)
  }
}

private class WeightedItem(
  private val text: String,
  val weight: Int,
) {
  override fun toString() = text
}

private class TreemapPanel(
  private val model: TreeModel,
) : JPanel() {
  private val rectMap = mutableMapOf<DefaultMutableTreeNode, Rectangle2D>()
  private var selection: DefaultMutableTreeNode? = null

  fun setSelectionPath(path: TreePath?) {
    selection = path?.lastPathComponent as? DefaultMutableTreeNode
    repaint()
  }

  // Prefer the deepest node when nested rectangles overlap
  fun getPathAt(pt: Point) = rectMap.entries
    .filter { it.value.contains(pt) }
    .maxByOrNull { it.key.level }
    ?.let { TreePath(it.key.path) }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val rect = SwingUtilities.calculateInnerArea(this, null)
    val root = model.root as? DefaultMutableTreeNode
    if (root != null && !rect.isEmpty) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING,
        RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
      )
      rectMap.clear()
      drawTreemap(g2, root, rect)
      drawSelection(g2)
      g2.dispose()
    }
  }

  private fun drawTreemap(
    g2: Graphics2D,
    root: DefaultMutableTreeNode,
    rect: Rectangle,
  ) {
    val children = getChildren(root)
    val area = Rectangle2D.Double()
    area.setRect(rect)
    val rects = SquarifyUtils.squarify(getWeights(children), area)
    val maxWeight = getMaxLeafWeight(root)
    children.forEachIndexed { i, child ->
      val hue = i / children.size.toFloat()
      drawNode(g2, child, rects[i], hue, maxWeight)
    }
  }

  private fun drawNode(
    g2: Graphics2D,
    node: DefaultMutableTreeNode,
    area: Rectangle2D,
    hue: Float,
    maxWeight: Double,
  ) {
    val rect = deflate(area, GAP)
    if (rect.isEmpty) {
      return
    }
    rectMap[node] = rect
    if (node.isLeaf) {
      drawLeaf(g2, node, rect, hue, maxWeight)
    } else {
      val children = getChildren(node)
      val rects = SquarifyUtils.squarify(getWeights(children), rect)
      children.forEachIndexed { i, child ->
        drawNode(g2, child, rects[i], hue, maxWeight)
      }
    }
  }

  private fun drawLeaf(
    g2: Graphics2D,
    node: DefaultMutableTreeNode,
    rect: Rectangle2D,
    hue: Float,
    maxWeight: Double,
  ) {
    // The bigger the weight, the more saturated the color
    val sat = (.2 + .6 * getWeight(node) / maxWeight).toFloat()
    val color = Color.getHSBColor(hue, sat, .95f)
    g2.color = color
    g2.fill(rect)
    g2.color = color.darker()
    g2.draw(rect)
    g2.color = getTextColor(color)
    drawLabel(g2, node.userObject?.toString() ?: "", rect)
  }

  private fun drawSelection(g2: Graphics2D) {
    val node = selection ?: return
    val rect = rectMap[node] ?: return
    if (!node.isLeaf) {
      g2.color = Color(0x64_FF_FF_FF, true)
      g2.fill(rect)
    }
    g2.stroke = BasicStroke(2f)
    g2.color = Color.BLACK
    g2.draw(rect)
  }

  companion object {
    private const val GAP = 1.0

    private fun getTextColor(bg: Color): Color {
      // ITU-R BT.601 luma; use white text on dark backgrounds
      val luma = .299 * bg.red + .587 * bg.green + .114 * bg.blue
      return if (luma < 127.5) Color.WHITE else Color.BLACK
    }

    private fun drawLabel(g2: Graphics2D, txt: String, rect: Rectangle2D) {
      val fm = g2.fontMetrics
      val b = fm.getStringBounds(txt, g2)
      if (b.width < rect.width - 2.0 && b.height < rect.height) {
        val tx = (rect.centerX - b.width / 2.0).toFloat()
        val ty = (rect.centerY - b.height / 2.0 + fm.ascent).toFloat()
        g2.drawString(txt, tx, ty)
      }
    }

    private fun getChildren(node: DefaultMutableTreeNode) =
      node.children().toList().filterIsInstance<DefaultMutableTreeNode>()

    private fun getWeight(node: DefaultMutableTreeNode): Double =
      if (node.isLeaf) {
        (node.userObject as? WeightedItem)?.weight?.toDouble() ?: 1.0
      } else {
        getChildren(node).sumOf { getWeight(it) }
      }

    private fun getWeights(nodes: List<DefaultMutableTreeNode>) =
      nodes.map { getWeight(it) }

    private fun getMaxLeafWeight(root: DefaultMutableTreeNode) = root
      .preorderEnumeration()
      .toList()
      .filterIsInstance<DefaultMutableTreeNode>()
      .filter { it.isLeaf }
      .maxOfOrNull { getWeight(it) } ?: 1.0

    private fun deflate(r: Rectangle2D, d: Double) =
      Rectangle2D.Double(r.x + d, r.y + d, r.width - d - d, r.height - d - d)
  }
}

// Squarified Treemaps: Mark Bruls, Kees Huizing, and Jarke J. van Wijk
// https://www.win.tue.nl/~vanwijk/stm.pdf
private object SquarifyUtils {
  fun squarify(weights: List<Double>, area: Rectangle2D): List<Rectangle2D> {
    val size = weights.size
    val rects = MutableList(size) { area }
    val total = weights.sum()
    if (total > 0.0 && !area.isEmpty) {
      val scale = area.width * area.height / total
      val rest = Rectangle2D.Double()
      rest.setRect(area)
      var idx = 0
      while (idx < size && !rest.isEmpty) {
        idx = layoutRow(weights, scale, idx, rest, rects)
      }
    }
    return rects
  }

  // Lay out one row, shrink the remaining area, and return the next start index
  private fun layoutRow(
    weights: List<Double>,
    scale: Double,
    idx: Int,
    rest: Rectangle2D,
    rects: MutableList<Rectangle2D>,
  ): Int {
    val horizontal = rest.width >= rest.height
    val side = if (horizontal) rest.height else rest.width
    // A row that ends where it starts would leave the remaining area
    // unchanged: consume at least one rectangle to avoid an infinite loop
    val end = max(getRowEnd(weights, scale, idx, side), idx + 1)
    val rowSum = weights.subList(idx, end).sum() * scale
    val thickness = rowSum / side
    var offset = 0.0
    for (i in idx..<end) {
      val len = weights[i] * scale / thickness
      rects[i] = if (horizontal) {
        Rectangle2D.Double(rest.x, rest.y + offset, thickness, len)
      } else {
        Rectangle2D.Double(rest.x + offset, rest.y, len, thickness)
      }
      offset += len
    }
    if (horizontal) {
      rest.setRect(rest.x + thickness, rest.y, rest.width - thickness, rest.height)
    } else {
      rest.setRect(rest.x, rest.y + thickness, rest.width, rest.height - thickness)
    }
    return end
  }

  // Grow the current row while the worst aspect ratio improves
  private fun getRowEnd(
    weights: List<Double>,
    scale: Double,
    idx: Int,
    side: Double,
  ): Int {
    var rowSum = 0.0
    var minArea = Double.MAX_VALUE
    var maxArea = 0.0
    var worst = Double.MAX_VALUE
    var end = idx
    while (end < weights.size) {
      val a = weights[end] * scale
      val w = worstAspect(rowSum + a, min(minArea, a), max(maxArea, a), side)
      if (w > worst) {
        break
      }
      worst = w
      rowSum += a
      minArea = min(minArea, a)
      maxArea = max(maxArea, a)
      end += 1
    }
    return end
  }

  private fun worstAspect(
    sum: Double,
    minArea: Double,
    maxArea: Double,
    side: Double,
  ): Double {
    val s2 = sum * sum
    val w2 = side * side
    return max(w2 * maxArea / s2, s2 / (w2 * minArea))
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
