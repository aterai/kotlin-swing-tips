package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeModel

fun createUI(): Component {
  val defaultTree = object : JTree(DefaultTreeModel(createTreeRoot())) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setCellRenderer(ToolTipCellRenderer(getCellRenderer()))
    }
  }
  ToolTipManager.sharedInstance().registerComponent(defaultTree)

  val aboveCellTree = AboveCellToolTipTree(DefaultTreeModel(createTreeRoot()))
  ToolTipManager.sharedInstance().registerComponent(aboveCellTree)

  val p = JPanel(GridLayout(2, 1))
  p.add(createTitledPanel("Default location", defaultTree))
  p.add(createTitledPanel("Draw directly above the cell", aboveCellTree))

  return JSplitPane(JSplitPane.HORIZONTAL_SPLIT).also {
    it.leftComponent = p
    it.rightComponent = JPanel()
    it.resizeWeight = .5
    it.dividerLocation = 160
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createTreeRoot(): DefaultMutableTreeNode {
  val set4 = DefaultMutableTreeNode("Set 00000004")
  set4.add(DefaultMutableTreeNode("222222111111111111111122222"))
  set4.add(DefaultMutableTreeNode("111111111111111"))
  set4.add(DefaultMutableTreeNode("2222222222222"))
  set4.add(DefaultMutableTreeNode("3333333"))

  val set1 = DefaultMutableTreeNode("Set 00000001")
  set1.add(DefaultMutableTreeNode("33333333333333333333333333333333333"))
  set1.add(DefaultMutableTreeNode("111111111"))
  set1.add(DefaultMutableTreeNode("22222222222"))
  set1.add(set4)
  set1.add(DefaultMutableTreeNode("222222"))
  set1.add(DefaultMutableTreeNode("222222222"))

  val set2 = DefaultMutableTreeNode("Set 00000002")
  set2.add(DefaultMutableTreeNode("5555555555"))
  set2.add(DefaultMutableTreeNode("6666666666666"))

  val set3 = DefaultMutableTreeNode("Set 00000003")
  set3.add(DefaultMutableTreeNode("7777777777"))
  set3.add(DefaultMutableTreeNode("8888888888888"))
  set3.add(DefaultMutableTreeNode("9999999"))

  val root = DefaultMutableTreeNode("Root")
  root.add(DefaultMutableTreeNode("00000000000000000000000000000"))
  root.add(set3)
  root.add(DefaultMutableTreeNode("111111111111111111111111111"))
  root.add(set1)
  root.add(set2)
  root.add(DefaultMutableTreeNode("22222222222222222222222222222222222"))
  root.add(DefaultMutableTreeNode("33333333333333333333333"))
  return root
}

private fun createTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val scroll = JScrollPane(c)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  p.add(scroll)
  return p
}

private class ToolTipCellRenderer(
  private val renderer: TreeCellRenderer,
) : TreeCellRenderer {
  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val c = renderer.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    (c as? JComponent)?.toolTipText = value?.toString()
    return c
  }
}

private class AboveCellToolTipTree(
  model: TreeModel,
) : JTree(model) {
  private val label = JLabel()

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    setCellRenderer(ToolTipCellRenderer(getCellRenderer()))
  }

  override fun getToolTipLocation(e: MouseEvent): Point? {
    // getPathForLocation(...) returns null unless the point is inside the node cell bounds
    val path = getPathForLocation(e.x, e.y) ?: return null
    val cellBounds = getPathBounds(path) ?: return null
    val row = getRowForPath(path)
    val node = path.lastPathComponent
    val hasFocus = hasFocus() && leadSelectionRow == row
    val isLeaf = model.isLeaf(node)
    val c = getCellRenderer().getTreeCellRendererComponent(
      this,
      node,
      isRowSelected(row),
      isExpanded(row),
      isLeaf,
      row,
      hasFocus,
    )
    var pt: Point? = null
    if ((c as? JComponent)?.toolTipText != null) {
      val ins = label.insets
      pt = cellBounds.location
      pt.translate(-ins.left, -ins.top)
      label.icon = RendererIcon(c, this, cellBounds.size)
    }
    return pt
  }

  override fun createToolTip(): JToolTip {
    val tip = object : JToolTip() {
      override fun getPreferredSize() = label.preferredSize
    }
    tip.border = BorderFactory.createEmptyBorder()
    tip.layout = BorderLayout()
    tip.component = this
    tip.add(label)
    return tip
  }
}

private class RendererIcon(
  private val renderer: Component,
  private val parent: Container,
  private val size: Dimension,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    // Fill with the tree background so the tooltip looks the same as the cell below it
    g2.paint = parent.background
    g2.fillRect(x, y, size.width, size.height)
    // Paint the renderer through the CellRendererPane of the tree, not of the JToolTip.
    // DefaultTreeCellRenderer discards FontUIResource and inherits the font of its parent,
    // so painting it in the JToolTip would use the JToolTip font, which differs from
    // the JTree font in some LookAndFeels (e.g. Metal) and does not fit the cell bounds.
    SwingUtilities.paintComponent(g2, renderer, parent, x, y, size.width, size.height)
    g2.dispose()
  }

  override fun getIconWidth() = size.width

  override fun getIconHeight() = size.height
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
