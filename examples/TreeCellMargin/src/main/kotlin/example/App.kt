package example

import java.awt.*
import javax.swing.*
import javax.swing.border.Border
import javax.swing.border.LineBorder
import javax.swing.plaf.basic.BasicGraphicsUtils
import javax.swing.tree.DefaultTreeCellRenderer

fun makeUI(): Component {
  val tree1 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setCellRenderer(MarginTreeCellRenderer())
    }
  }

  val tree2 = object : JTree() {
    override fun updateUI() {
      setCellRenderer(null)
      super.updateUI()
      setCellRenderer(CompoundTreeCellRenderer())
    }
  }

  return JPanel(GridLayout(1, 3)).also {
    it.add(makeTitledPanel("Default", JScrollPane(JTree())))
    it.add(makeTitledPanel("Margin", JScrollPane(tree1)))
    it.add(makeTitledPanel("Label", JScrollPane(tree2)))
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

private class MarginTreeCellRenderer : DefaultTreeCellRenderer() {
  private var drawsFocusBorder = false
  private var drawDashedFocus = false
  private var fillBackground = false
  private var treeBgsColor: Color? = null
  private var focusBgsColor: Color? = null

  override fun updateUI() {
    super.updateUI()
    drawsFocusBorder = UIManager.getBoolean("Tree.drawsFocusBorderAroundIcon")
    drawDashedFocus = UIManager.getBoolean("Tree.drawDashedFocusIndicator")
    fillBackground = UIManager.getBoolean("Tree.rendererFillBackground")
    isOpaque = fillBackground
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
    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, false)
    // this.tree = tree
    this.hasFocus = hasFocus
    this.selected = selected
    return this
  }

  override fun paint(g: Graphics) {
    if (!componentOrientation.isLeftToRight) {
      super.paint(g)
      return
    }
    val bgColor = if (selected) {
      getBackgroundSelectionColor()
    } else {
      getBackgroundNonSelectionColor() ?: background
    }
    var imageOffset = -1
    if (bgColor != null && fillBackground) {
      imageOffset = getLabelStartPosition()
      g.color = bgColor
      g.fillRect(imageOffset - MARGIN, y, width + MARGIN - imageOffset, height)
    }
    super.paint(g)
    if (hasFocus) {
      if (drawsFocusBorder) {
        imageOffset = 0
      } else if (imageOffset == -1) {
        imageOffset = getLabelStartPosition()
      }
      g.color = bgColor
      g.fillRect(imageOffset - MARGIN, y, MARGIN + 1, height)
      val r = Rectangle(imageOffset - MARGIN, y, width + MARGIN - imageOffset, height)
      paintFocusRect(g, r, bgColor)
    }
  }

  private fun paintFocusRect(
    g: Graphics,
    r: Rectangle,
    notColor: Color,
  ) {
    val bsColor = getBorderSelectionColor()
    val b = selected || !drawDashedFocus
    if (bsColor != null && b) {
      g.color = bsColor
      g.drawRect(r.x, r.y, r.width - 1, r.height - 1)
    }
    if (drawDashedFocus) {
      if (notColor != treeBgsColor) {
        treeBgsColor = notColor
        focusBgsColor = Color(notColor.rgb.inv())
      }
      g.color = focusBgsColor
      BasicGraphicsUtils.drawDashedRect(g, r.x, r.y, r.width, r.height)
    }
  }

  private fun getLabelStartPosition() = icon
    ?.takeIf { text != null }
    ?.let { it.iconWidth + 0.coerceAtLeast(iconTextGap - 1) }
    ?: 0

  companion object {
    private const val MARGIN = 2 // < 3
  }
}

private class CompoundTreeCellRenderer : DefaultTreeCellRenderer() {
  private val renderer = JPanel(BorderLayout())
  private val icon = JLabel()
  private val text = JLabel()
  private val insideBorder = BorderFactory.createEmptyBorder(1, 2, 1, 2)
  private val outsideBorder = BorderFactory.createEmptyBorder(1, 1, 1, 1)
  private val emptyBorder = BorderFactory.createCompoundBorder(
    outsideBorder,
    insideBorder,
  )
  private var focusBorder: Border? = null
  private val isSynth = ui.javaClass.name.contains("Synth")

  init {
    focusBorder = if (isSynth) {
      emptyBorder
    } else {
      val bsColor = getBorderSelectionColor()
      val b = if (UIManager.getBoolean("Tree.drawDashedFocusIndicator")) {
        DotBorder(Color(getBackgroundSelectionColor().rgb.inv()), bsColor)
      } else {
        BorderFactory.createLineBorder(bsColor)
      }
      BorderFactory.createCompoundBorder(b, insideBorder)
    }
    icon.border = BorderFactory.createEmptyBorder(0, 0, 0, 2)
    text.border = emptyBorder
    text.isOpaque = true
    renderer.isOpaque = false
    renderer.add(icon, BorderLayout.WEST)
    renderer.add(text)
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
      bgColor = getBackgroundSelectionColor()
      fgColor = getTextSelectionColor()
      text.isOpaque = !isSynth
    } else {
      bgColor = getBackgroundNonSelectionColor() ?: background
      fgColor = getTextNonSelectionColor() ?: foreground
      text.isOpaque = false
    }
    text.foreground = fgColor
    text.background = bgColor
    text.border = if (hasFocus) focusBorder else emptyBorder
    val c = super.getTreeCellRendererComponent(
      tree,
      value,
      selected,
      expanded,
      leaf,
      row,
      hasFocus,
    )
    (c as? JLabel)?.also {
      text.text = it.text
      icon.icon = it.icon
    }
    return renderer
  }
}

private class DotBorder(
  color: Color?,
  private val borderSelectionColor: Color,
) : LineBorder(color, 1) {
  override fun isBorderOpaque() = true

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    w: Int,
    h: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = borderSelectionColor
    g2.drawRect(0, 0, w - 1, h - 1)
    g2.paint = getLineColor()
    BasicGraphicsUtils.drawDashedRect(g2, 0, 0, w, h)
    g2.dispose()
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
