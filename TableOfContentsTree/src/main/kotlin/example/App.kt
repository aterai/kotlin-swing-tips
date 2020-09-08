package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

fun makeUI(): Component {
  val tree = object : JTree(makeModel()) {
    override fun getScrollableTracksViewportWidth() = true

    override fun updateUI() {
      super.updateUI()
      setCellRenderer(TableOfContentsTreeCellRenderer())
      border = BorderFactory.createTitledBorder("TreeCellRenderer")
    }
  }
  tree.isRootVisible = false

  val tree2 = TableOfContentsTree(makeModel())
  tree2.isRootVisible = false

  val sp = JSplitPane()
  sp.resizeWeight = .5
  sp.leftComponent = JScrollPane(tree)
  sp.rightComponent = JScrollPane(tree2)

  return JPanel(BorderLayout()).also {
    it.add(sp)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeModel(): DefaultTreeModel {
  val root = DefaultMutableTreeNode("root")
  val s0 = DefaultMutableTreeNode(TableOfContents("1. Introduction", 1))
  root.add(s0)
  val s1 = DefaultMutableTreeNode(TableOfContents("2. Chapter", 1))
  s1.add(DefaultMutableTreeNode(TableOfContents("2.1. Section", 2)))
  s1.add(DefaultMutableTreeNode(TableOfContents("2.2. Section", 4)))
  s1.add(DefaultMutableTreeNode(TableOfContents("2.3. Section", 8)))
  root.add(s1)
  val s2 = DefaultMutableTreeNode(TableOfContents("3. Chapter", 10))
  s2.add(DefaultMutableTreeNode(TableOfContents("ddd", 12)))
  s2.add(DefaultMutableTreeNode(TableOfContents("eee", 24)))
  s2.add(DefaultMutableTreeNode(TableOfContents("fff", 38)))
  root.add(s2)
  return DefaultTreeModel(root)
}

private data class TableOfContents(val title: String, val page: Int) {
  override fun toString() = title
}

private class TableOfContentsTreeCellRenderer : DefaultTreeCellRenderer() {
  private var pn = -1
  private val pnPt = Point()
  private var rxs = 0
  private var rxe = 0
  private var isSynth = false
  private val renderer = object : JPanel(BorderLayout()) {
    override fun paintComponent(g: Graphics) {
      super.paintComponent(g)
      if (pn >= 0) {
        val str = "%3d".format(pn)
        val metrics = g.fontMetrics
        val g2 = g.create() as? Graphics2D ?: return
        g2.paint = if (isSynth) foreground else getTextNonSelectionColor()
        g2.drawString(str, pnPt.x - x - metrics.stringWidth(str), pnPt.y)
        g2.stroke = READER
        g2.drawLine(rxs, pnPt.y, rxe - x - metrics.stringWidth("000"), pnPt.y)
        g2.dispose()
      }
    }

    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.width = Short.MAX_VALUE.toInt()
      return d
    }
  }

  override fun updateUI() {
    super.updateUI()
    isSynth = ui.javaClass.name.contains("Synth")
    if (isSynth) {
      setBackgroundSelectionColor(Color(0x0, true))
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
    val c = super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    val l = c as? JLabel ?: return c
    return ((value as? DefaultMutableTreeNode)?.userObject as? TableOfContents)
      ?.let { toc ->
        renderer.removeAll()
        renderer.add(l, BorderLayout.WEST)
        if (isSynth) {
          renderer.foreground = l.foreground
        }
        val gap = l.iconTextGap
        val d = l.preferredSize
        pnPt.setLocation(tree.width - gap, l.getBaseline(d.width, d.height))
        pn = toc.page
        rxs = d.width + gap
        rxe = tree.width - tree.insets.right - gap
        renderer.isOpaque = false
        renderer
      } ?: l.also { pn = -1 }
  }

  companion object {
    private val READER = BasicStroke(
      1f,
      BasicStroke.CAP_BUTT,
      BasicStroke.JOIN_MITER,
      1f,
      floatArrayOf(1f),
      0f
    )
  }
}

private class TableOfContentsTree(model: TreeModel?) : JTree(model) {
  private var isSynth = false
  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createTitledBorder("JTree#paintComponent(...)")
    isSynth = ui.javaClass.name.contains("Synth")
  }

  override fun paintComponent(g: Graphics) {
    g.color = background
    g.fillRect(0, 0, width, height)
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    val fm = g.fontMetrics
    val pageNumMaxWidth = fm.stringWidth("000")
    val ins = insets
    val rect = visibleRect // getVisibleRowsRect()
    for (i in 0 until rowCount) {
      val r = getRowBounds(i)
      if (rect.intersects(r)) {
        val path = getPathForRow(i)
        val tcr = getCellRenderer()
        if (isSynth && isRowSelected(i)) {
          (tcr as? DefaultTreeCellRenderer)?.also {
            g2.paint = it.textSelectionColor
          }
        } else {
          g2.paint = foreground
        }
        val node = path.lastPathComponent as? DefaultMutableTreeNode
        (node?.userObject as? TableOfContents)?.also {
          val pn = it.page.toString()
          val x = width - 1 - fm.stringWidth(pn) - ins.right
          val y = r.y + ((tcr as? Component)?.getBaseline(r.width, r.height) ?: 0)
          g2.drawString(pn, x, y)
          val gap = 5
          val x2 = width - 1 - pageNumMaxWidth - ins.right
          val s = g2.stroke
          g2.stroke = READER
          g2.drawLine(r.x + r.width + gap, y, x2 - gap, y)
          g2.stroke = s
        }
      }
    }
    g2.dispose()
  }

  companion object {
    private val READER = BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, floatArrayOf(1f), 0f)
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
