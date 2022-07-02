package example

import java.awt.* // ktlint-disable no-wildcard-imports
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.CompoundBorder
import javax.swing.border.LineBorder
import javax.swing.plaf.basic.BasicGraphicsUtils
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
  UIManager.put("Tree.closedIcon", ColorIcon(Color.RED))
  UIManager.put("Tree.openIcon", ColorIcon(Color.GREEN))
  val tree1 = JTree()
  tree1.rowHeight = 0

  val tree2 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setCellRenderer(CompoundTreeCellRenderer())
      setRowHeight(0)
    }
  }

  return JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("Default", JScrollPane(tree1)))
    it.add(makeTitledPanel("Label", JScrollPane(tree2)))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, c: Component): Component {
  val p = JPanel(BorderLayout())
  p.border = BorderFactory.createTitledBorder(title)
  p.add(c)
  return p
}

private class CompoundTreeCellRenderer : DefaultTreeCellRenderer() {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel()
  private val text = JLabel()
  private val emptyBorder: CompoundBorder
  private val compoundFocusBorder: CompoundBorder

  init {
    val insideBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2)
    val outsideBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1)
    emptyBorder = BorderFactory.createCompoundBorder(outsideBorder, insideBorder)

    val bsColor = getBorderSelectionColor()
    val focusBgsColor = Color(getBackgroundSelectionColor().rgb.inv())
    compoundFocusBorder = BorderFactory.createCompoundBorder(
      DotBorder(focusBgsColor, bsColor),
      insideBorder
    )

    icon.border = BorderFactory.createEmptyBorder(0, 0, 0, 2)
    text.border = emptyBorder
    text.isOpaque = true
    renderer.isOpaque = false
    renderer.add(icon, BorderLayout.WEST)

    val wrap = JPanel(GridBagLayout())
    wrap.isOpaque = false
    wrap.add(text)
    renderer.add(wrap)
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
    val bgColor: Color
    val fgColor: Color
    if (selected) {
      bgColor = getBackgroundSelectionColor()
      fgColor = getTextSelectionColor()
    } else {
      bgColor = getBackgroundNonSelectionColor() ?: background
      fgColor = getTextNonSelectionColor() ?: foreground
    }
    text.foreground = fgColor
    text.background = bgColor
    text.border = if (hasFocus) compoundFocusBorder else emptyBorder
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus
    )
    (c as? JLabel)?.also {
      text.text = it.text
      icon.icon = it.icon
    }
    return renderer
  }
}

private class DotBorder(
  color: Color,
  private val borderSelectionColor: Color
) : LineBorder(color, 1) {
  override fun isBorderOpaque() = true

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, w: Int, h: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = borderSelectionColor
    g2.drawRect(0, 0, w - 1, h - 1)
    g2.paint = getLineColor()
    BasicGraphicsUtils.drawDashedRect(g2, 0, 0, w, h)
    g2.dispose()
  }
}

private class ColorIcon(private val color: Color) : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = color
    g2.fillOval(1, 1, iconWidth - 2, iconHeight - 2)
    g2.dispose()
  }

  override fun getIconWidth() = 24

  override fun getIconHeight() = 24
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
