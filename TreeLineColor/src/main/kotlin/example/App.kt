package example

import java.awt.*
import javax.swing.*
import javax.swing.plaf.basic.BasicTreeUI
import javax.swing.tree.TreePath

fun makeUI(): Component {
  UIManager.put("Tree.paintLines", true)
  UIManager.put("Tree.lineTypeDashed", true)
  UIManager.put("Tree.line", Color.GREEN)
  UIManager.put("Tree.hash", Color.RED)

  val tree0 = JTree()

  val tree1 = JTree()
  tree1.putClientProperty("JTree.lineStyle", "Horizontal")

  val tree2 = object : JTree() {
    override fun updateUI() {
      super.updateUI()
      UIManager.put("Tree.lineTypeDashed", false)
      setUI(LineStyleTreeUI())
    }
  }

  return JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("lineTypeDashed", JScrollPane(tree0)))
    it.add(makeTitledPanel("lineStyle", JScrollPane(tree1)))
    it.add(makeTitledPanel("BasicTreeUI", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class LineStyleTreeUI : BasicTreeUI() {
  private val horizontalLine = BasicStroke(2f)
  private val verticalLine = BasicStroke(5f)

  public override fun getHashColor(): Color = Color.BLUE

  override fun paintHorizontalPartOfLeg(
    g: Graphics,
    clipBounds: Rectangle,
    insets: Insets,
    bounds: Rectangle,
    path: TreePath,
    row: Int,
    isExpanded: Boolean,
    hasBeenExpanded: Boolean,
    isLeaf: Boolean,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.stroke = horizontalLine
    super.paintHorizontalPartOfLeg(
      g2,
      clipBounds,
      insets,
      bounds,
      path,
      row,
      isExpanded,
      hasBeenExpanded,
      isLeaf,
    )
    g2.dispose()
  }

  override fun paintVerticalPartOfLeg(
    g: Graphics,
    clipBounds: Rectangle,
    insets: Insets,
    path: TreePath,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.stroke = verticalLine
    super.paintVerticalPartOfLeg(g2, clipBounds, insets, path)
    g2.dispose()
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
