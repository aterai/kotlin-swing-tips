package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeModel

fun makeUI(): Component {
  val tree0 = object : JTree(DefaultTreeModel(makeTreeRoot())) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      // rowHeight = 24
      val renderer = getCellRenderer()
      setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
        renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
          (it as? JLabel)?.toolTipText = value?.toString()
        }
      }
    }
  }
  ToolTipManager.sharedInstance().registerComponent(tree0)

  val tree1 = TooltipTree(DefaultTreeModel(makeTreeRoot()))
  ToolTipManager.sharedInstance().registerComponent(tree1)

  val p = JPanel(GridLayout(2, 1))
  p.add(makeTitledPanel("Default location", tree0))
  p.add(makeTitledPanel("Draw directly above the cell", tree1))

  return JSplitPane().also {
    it.resizeWeight = .5
    it.leftComponent = p
    it.rightComponent = JLabel("dummy panel")
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTreeRoot(): DefaultMutableTreeNode {
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

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  val scroll = JScrollPane(c)
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  it.border = BorderFactory.createTitledBorder(title)
  it.add(scroll)
}

private class TooltipTree(model: TreeModel) : JTree(model) {
  private val label = object : JLabel() {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = getRowHeight()
    }
  }

  override fun updateUI() {
    setCellRenderer(null)
    super.updateUI()
    // rowHeight = 24
    val renderer = getCellRenderer()
    setCellRenderer { tree, value, selected, expanded, leaf, row, hasFocus ->
      renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus).also {
        (it as? JLabel)?.toolTipText = value?.toString()
      }
    }
  }

  override fun getToolTipLocation(e: MouseEvent): Point? {
    val p = e.point
    val i = getRowForLocation(p.x, p.y)
    val cellBounds = getRowBounds(i)
    if (i >= 0 && cellBounds?.contains(p.x, p.y) == true) {
      val tsm = getSelectionModel()
      val node = getPathForRow(i).lastPathComponent
      val hasFocus = hasFocus() && tsm.leadSelectionRow == i
      val isLeaf = model.isLeaf(node)
      val r = getCellRenderer()
      val tcr = r.getTreeCellRendererComponent(this, node, isRowSelected(i), isExpanded(i), isLeaf, i, hasFocus)
      if ((tcr as? JComponent)?.toolTipText != null) {
        val pt = cellBounds.location
        val ins = label.insets
        pt.translate(-ins.left, -ins.top)
        label.icon = RendererIcon(tcr, cellBounds)
        return pt
      }
    }
    return null
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

private class RendererIcon(private val renderer: Component, private val rect: Rectangle) : Icon {
  init {
    rect.setLocation(0, 0)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    (c as? Container)?.also {
      val g2 = g.create() as? Graphics2D ?: return
      g2.clearRect(0, 0, c.width, c.height)
      // g2.translate(x, y)
      SwingUtilities.paintComponent(g2, renderer, it, rect)
      g2.dispose()
    }
  }

  override fun getIconWidth() = renderer.preferredSize.width

  override fun getIconHeight() = renderer.preferredSize.height
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
