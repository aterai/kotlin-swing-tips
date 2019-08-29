package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeModel

class MainPanel : JPanel(BorderLayout()) {
  init {

    val tree0 = object : JTree(DefaultTreeModel(makeTreeRoot())) {
      override fun updateUI() {
        super.updateUI()
        setCellRenderer(TooltipTreeCellRenderer())
      }
    }
    ToolTipManager.sharedInstance().registerComponent(tree0)

    val tree1 = TooltipTree(DefaultTreeModel(makeTreeRoot()))
    ToolTipManager.sharedInstance().registerComponent(tree1)

    val p = JPanel(GridLayout(2, 1))
    p.add(makeTitledPanel("Default location", tree0))
    p.add(makeTitledPanel("Draw directly above the cell", tree1))

    add(JSplitPane().also {
      it.setResizeWeight(.5)
      it.setLeftComponent(p)
      it.setRightComponent(JLabel("dummy panel"))
    })
    setPreferredSize(Dimension(320, 240))
  }

  private fun makeTreeRoot(): DefaultMutableTreeNode {
    val set4 = DefaultMutableTreeNode("Set 00000004")
    set4.add(DefaultMutableTreeNode("222222111111111111111122222"))
    set4.add(DefaultMutableTreeNode("eeeeeeeeeeeee"))
    set4.add(DefaultMutableTreeNode("bbbbbbbbbbbb"))
    set4.add(DefaultMutableTreeNode("zzzzzzz"))

    val set1 = DefaultMutableTreeNode("Set 00000001")
    set1.add(DefaultMutableTreeNode("33333333333333333333333333333333333"))
    set1.add(DefaultMutableTreeNode("111111111"))
    set1.add(DefaultMutableTreeNode("22222222222"))
    set1.add(set4)
    set1.add(DefaultMutableTreeNode("222222"))
    set1.add(DefaultMutableTreeNode("222222222"))

    val set2 = DefaultMutableTreeNode("Set 00000002")
    set2.add(DefaultMutableTreeNode("eeeeeeeeeeeee"))
    set2.add(DefaultMutableTreeNode("bbbbbbbbbbbb"))

    val set3 = DefaultMutableTreeNode("Set 00000003")
    set3.add(DefaultMutableTreeNode("zzzzzzz"))
    set3.add(DefaultMutableTreeNode("aaaaaaaaaaaa"))
    set3.add(DefaultMutableTreeNode("ccccccccc"))

    val root = DefaultMutableTreeNode("Root")
    root.add(DefaultMutableTreeNode("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"))
    root.add(set3)
    root.add(DefaultMutableTreeNode("eeeeeeeeeeeeeeeeeeeeeeeeeeeee"))
    root.add(set1)
    root.add(set2)
    root.add(DefaultMutableTreeNode("22222222222222222222222222222222222"))
    root.add(DefaultMutableTreeNode("bbbbbbbbbbbbbbbbbbbbbbbbbbbbb"))
    return root
  }

  private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
    val scroll = JScrollPane(c)
    scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    it.setBorder(BorderFactory.createTitledBorder(title))
    it.add(scroll)
  }
}

internal class TooltipTree(model: TreeModel) : JTree(model) {
  private val label: JLabel = object : JLabel() {
    override fun getPreferredSize() = super.getPreferredSize()?.also {
      it.height = getRowHeight()
    }
  }

  override fun updateUI() {
    super.updateUI()
    // setRowHeight(24)
    setCellRenderer(TooltipTreeCellRenderer())
  }

  override fun getToolTipLocation(e: MouseEvent): Point? {
    val p = e.getPoint()
    val i = getRowForLocation(p.x, p.y)
    val cellBounds = getRowBounds(i)
    if (i >= 0 && cellBounds?.contains(p.x, p.y) == true) {
      val tsm = getSelectionModel()
      val node = getPathForRow(i).getLastPathComponent()
      // println(node)
      val hasFocus = hasFocus() && tsm.getLeadSelectionRow() == i
      val isLeaf = getModel().isLeaf(node)
      val r = getCellRenderer()
      val tcr = r.getTreeCellRendererComponent(this, node, isRowSelected(i), isExpanded(i), isLeaf, i, hasFocus)
      if ((tcr as? JComponent)?.getToolTipText() != null) {
        // println(((JComponent) tcr).getToolTipText())
        val pt = cellBounds.getLocation()
        // label.setBorder(BorderFactory.createLineBorder(Color.RED))
        val ins = label.getInsets()
        pt.translate(-ins.left, -ins.top)
        label.setIcon(RendererIcon(tcr, cellBounds))
        // println(pt)
        return pt
      }
    }
    return null
  }

  override fun createToolTip(): JToolTip {
    val tip = object : JToolTip() {
      override fun getPreferredSize() = label.getPreferredSize()
    }
    // println("createToolTip")
    tip.setBorder(BorderFactory.createEmptyBorder())
    tip.setLayout(BorderLayout())
    tip.setComponent(this)
    tip.add(label)
    return tip
  }
}

internal class TooltipTreeCellRenderer : TreeCellRenderer {
  private val renderer = DefaultTreeCellRenderer()

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean
  ): Component {
    val c = renderer.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
    (c as? JLabel)?.setToolTipText(value?.toString())
    return c
  }
}

internal class RendererIcon(private val renderer: Component, private val rect: Rectangle) : Icon {
  init {
    rect.setLocation(0, 0)
  }

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    (c as? Container)?.also {
      val g2 = g.create() as? Graphics2D ?: return
      g2.clearRect(0, 0, c.getWidth(), c.getHeight())
      // g2.translate(x, y)
      SwingUtilities.paintComponent(g2, renderer, it, rect)
      g2.dispose()
    }
  }

  override fun getIconWidth() = renderer.getPreferredSize().width

  override fun getIconHeight() = renderer.getPreferredSize().height
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
