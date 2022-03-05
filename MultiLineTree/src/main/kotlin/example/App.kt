package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreeCellRenderer
import javax.swing.tree.TreeModel

fun makeUI(): Component {
  val tree = makeTree(getDefaultTreeModel2())
  tree.cellRenderer = MultiLineCellRenderer()

  return JPanel(GridLayout(1, 2)).also {
    it.add(makeTitledPanel("Html", makeTree(getDefaultTreeModel())))
    it.add(makeTitledPanel("TextAreaRenderer", tree))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component) = JPanel(BorderLayout()).also {
  it.border = BorderFactory.createTitledBorder(title)
  it.add(JScrollPane(c))
}

private fun makeTree(model: TreeModel) = JTree(model).also {
  it.rowHeight = 0
  for (i in 0 until it.rowCount) {
    it.expandRow(i)
  }
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

private class MultiLineCellRenderer : JPanel(BorderLayout()), TreeCellRenderer {
  private var renderer = DefaultTreeCellRenderer()
  private val icon = JLabel()
  private val text = CellTextArea2()

  init {
    text.isOpaque = true
    text.font = icon.font
    text.border = BorderFactory.createEmptyBorder()
    icon.isOpaque = true
    icon.border = BorderFactory.createEmptyBorder(1, 1, 1, 2)
    icon.verticalAlignment = SwingConstants.TOP
    isOpaque = false
    border = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    add(icon, BorderLayout.WEST)
    add(text)
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
    val l = renderer.getTreeCellRendererComponent(
      tree, value, selected, expanded, leaf, row, hasFocus
    )
    if (l is JLabel) {
      text.font = l.font
      text.text = l.text
      icon.icon = l.icon
    }
    val bgColor: Color
    val fgColor: Color
    if (selected) {
      bgColor = renderer.backgroundSelectionColor
      fgColor = renderer.textSelectionColor
    } else {
      bgColor = renderer.backgroundNonSelectionColor ?: renderer.background
      fgColor = renderer.textNonSelectionColor ?: renderer.foreground
    }
    text.foreground = fgColor
    text.background = bgColor
    icon.background = bgColor
    return this
  }

  override fun updateUI() {
    super.updateUI()
    renderer = DefaultTreeCellRenderer()
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
    val lineCount = root.elementCount // = root.getElementIndex(doc.getLength());
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
