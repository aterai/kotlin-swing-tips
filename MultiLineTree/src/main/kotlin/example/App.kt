package example

import java.awt.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeModel

fun makeUI(): Component {
  val tree1 = JTree(getDefaultTreeModel())
  tree1.rowHeight = 0

  val tree2 = object : JTree(getDefaultTreeModel2()) {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setRowHeight(0)
      setCellRenderer(MultiLineCellRenderer())
    }
  }

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("Html", expandRow(tree1)))
    it.add(makeTitledPanel("TextAreaRenderer", expandRow(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(
  title: String,
  c: Component,
): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(JScrollPane(c))
  return p
}

private fun expandRow(tree: JTree): JTree {
  for (i in 0 until tree.rowCount) {
    tree.expandRow(i)
  }
  return tree
}

private fun getDefaultTreeModel(): TreeModel {
  val root = DefaultMutableTreeNode("JTree")
  var parent = DefaultMutableTreeNode("colors")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("<html>blue<br>&nbsp;&nbsp;blue, blue"))
  parent.add(DefaultMutableTreeNode("<html>violet<br>&ensp;&ensp;violet"))
  parent.add(DefaultMutableTreeNode("<html>red<br>&emsp;red<br>&emsp;red"))
  parent.add(DefaultMutableTreeNode("<html>yellow<br>\u3000yellow"))
  parent = DefaultMutableTreeNode("sports")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("basketball"))
  parent.add(DefaultMutableTreeNode("soccer"))
  parent.add(DefaultMutableTreeNode("football"))
  parent.add(DefaultMutableTreeNode("hockey"))
  parent = DefaultMutableTreeNode("food")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("hot dogs"))
  parent.add(DefaultMutableTreeNode("pizza"))
  parent.add(DefaultMutableTreeNode("ravioli"))
  parent.add(DefaultMutableTreeNode("bananas"))
  return DefaultTreeModel(root)
}

private fun getDefaultTreeModel2(): TreeModel {
  val root = DefaultMutableTreeNode("JTree")
  var parent = DefaultMutableTreeNode("colors")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("blue\n  blue, blue"))
  parent.add(DefaultMutableTreeNode("violet\n  violet"))
  parent.add(DefaultMutableTreeNode("red\n red\n red"))
  parent.add(DefaultMutableTreeNode("yellow\n\u3000yellow"))
  parent = DefaultMutableTreeNode("sports")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("basketball"))
  parent.add(DefaultMutableTreeNode("soccer"))
  parent.add(DefaultMutableTreeNode("football"))
  parent.add(DefaultMutableTreeNode("hockey"))
  parent = DefaultMutableTreeNode("food")
  root.add(parent)
  parent.add(DefaultMutableTreeNode("hot dogs"))
  parent.add(DefaultMutableTreeNode("pizza"))
  parent.add(DefaultMutableTreeNode("ravioli"))
  parent.add(DefaultMutableTreeNode("bananas"))
  return DefaultTreeModel(root)
}

private class MultiLineCellRenderer : TreeCellRenderer {
  private val tcr = DefaultTreeCellRenderer()
  private val icon = JLabel()
  private val text = CellTextArea2()
  private val panel = JPanel(BorderLayout())

  init {
    text.isOpaque = true
    text.font = icon.font
    text.border = BorderFactory.createEmptyBorder()
    icon.isOpaque = true
    icon.border = BorderFactory.createEmptyBorder(1, 1, 1, 2)
    icon.verticalAlignment = SwingConstants.TOP
    panel.isOpaque = false
    panel.border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    panel.add(icon, BorderLayout.WEST)
    panel.add(text)
  }

  override fun getTreeCellRendererComponent(
    tree: JTree,
    value: Any?,
    selected: Boolean,
    expanded: Boolean,
    leaf: Boolean,
    row: Int,
    hasFocus: Boolean,
  ): Component {
    val bgColor: Color
    val fgColor: Color
    if (selected) {
      bgColor = tcr.backgroundSelectionColor
      fgColor = tcr.textSelectionColor
    } else {
      bgColor = tcr.backgroundNonSelectionColor ?: tcr.background
      fgColor = tcr.textNonSelectionColor ?: tcr.foreground
    }
    text.foreground = fgColor
    text.background = bgColor
    icon.background = bgColor
    val l = tcr.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    if (l is JLabel) {
      text.font = l.font
      text.text = l.text
      icon.icon = l.icon
    }
    return panel
  }
}

private class CellTextArea2 : JTextArea() {
  override fun getPreferredSize(): Dimension {
    val d = Dimension(10, 10)
    val i = insets
    d.width = d.width.coerceAtLeast(columns * columnWidth + i.left + i.right)
    d.height = d.height.coerceAtLeast(rows * rowHeight + i.top + i.bottom)
    return d
  }

  override fun setText(str: String) {
    super.setText(str)
    val fm = getFontMetrics(font)
    val doc = document
    val root = doc.defaultRootElement
    val lineCount = root.elementCount // = root.getElementIndex(doc.getLength())
    var maxWidth = 10
    runCatching {
      for (i in 0 until lineCount) {
        val e = root.getElement(i)
        val rangeStart = e.startOffset
        val rangeEnd = e.endOffset
        val line = doc.getText(rangeStart, rangeEnd - rangeStart)
        val width = fm.stringWidth(line)
        if (maxWidth < width) {
          maxWidth = width
        }
      }
    }
    rows = lineCount
    columns = 1 + maxWidth / columnWidth
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
