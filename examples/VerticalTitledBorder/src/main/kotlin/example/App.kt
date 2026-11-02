package example

import java.awt.*
import java.awt.geom.AffineTransform
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.TitledBorder

fun createUI(): Component {
  val p1 = JPanel(BorderLayout()).also {
    it.border = TitledBorder("TitledBorder 1234567890")
    it.add(JScrollPane(JTree()))
  }
  val p2 = JPanel(BorderLayout()).also {
    it.border = VerticalTitledBorder("VerticalTitledBorder 1234567890")
    it.add(JScrollPane(JTree()))
  }
  val p3 = JPanel(BorderLayout()).also {
    val border = VerticalTitledBorder("VerticalTitledBorder")
    it.border = TitledBorder(border, "TitledBorder")
    it.add(JScrollPane(JTree()))
  }
  return JPanel(GridLayout(1, 3, 5, 5)).also {
    it.add(p1)
    it.add(p2)
    it.add(p3)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class VerticalTitledBorder(
  title: String?,
) : TitledBorder(title) {
  private val label = JLabel(title)

  init {
    // Fill the label background so that the border line behind the title is hidden
    label.isOpaque = true
    // label.putClientProperty(BasicHTML.propertyKey, null)
  }

  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    val border = getBorder()
    val title = getTitle()
    if (title == null || title.isEmpty() || border == null) {
      super.paintBorder(c, g, x, y, width, height)
    } else {
      val edge = if (border is TitledBorder) 0 else EDGE_SPACING
      val titleLabel = getTitleLabel(c)
      val size = titleLabel.preferredSize
      val borderInsets = getInnerBorderInsets(border, c, Insets(0, 0, 0, 0))
      val labelH = size.height
      val labelW = (height - borderInsets.top - borderInsets.bottom).coerceAtMost(size.width)
      var borderX = x + edge
      val borderY = y + edge
      var borderW = width - edge - edge
      val borderH = height - edge - edge

      // Center the title on the left border line, the same way as
      // TitledBorder centers the title on the top border line
      var labelX = x
      val left = edge + borderInsets.left / 2 - labelH / 2
      if (left < edge) {
        borderX -= left
        borderW += left
      } else {
        labelX += left
      }
      border.paintBorder(c, g, borderX, borderY, borderW, borderH)

      val g2 = g.create() as? Graphics2D ?: return
      // The (x, y) offset is not zero when nested in another border,
      // e.g. a TitledBorder or a CompoundBorder
      g2.translate(labelX.toDouble(), y + (height + labelW) / 2.0)
      g2.transform(AffineTransform.getQuadrantRotateInstance(-1))
      // or: g2.rotate(-PI / 2.0)
      titleLabel.setSize(labelW, labelH)
      titleLabel.paint(g2)
      g2.dispose()
    }
  }

  override fun getBorderInsets(
    c: Component,
    insets: Insets,
  ): Insets {
    val border = getBorder()
    val borderInsets = getInnerBorderInsets(border, c, insets)
    val title = getTitle()
    if (title?.isNotEmpty() == true) {
      val edge = if (border is TitledBorder) 0 else EDGE_SPACING
      val size = getTitleLabel(c).preferredSize
      if (borderInsets.left < size.height) {
        borderInsets.left = size.height - edge
      }
      borderInsets.top += edge + TEXT_SPACING
      borderInsets.left += edge + TEXT_SPACING
      borderInsets.right += edge + TEXT_SPACING
      borderInsets.bottom += edge + TEXT_SPACING
    }
    return borderInsets
  }

  // @see javax/swing/border/TitledBorder.java#getTitleColor(Component)
  private fun getTitleColor(c: Component?): Color? =
    getTitleColor()
      ?: UIManager.getColor("TitledBorder.titleColor")
      ?: c?.foreground

  private fun getTitleLabel(c: Component?): JLabel {
    label.text = getTitle()
    label.font = getFont(c)
    label.foreground = getTitleColor(c)
    if (c != null) {
      label.componentOrientation = c.componentOrientation
      label.isEnabled = c.isEnabled
      label.background = c.background
    }
    return label
  }
}

// @see javax/swing/border/TitledBorder.java#getBorderInsets(Border, Component, Insets)
// Reinitialize and return the given insets, as AbstractBorder#getBorderInsets(...) does
private fun getInnerBorderInsets(
  border: Border?,
  c: Component,
  insets: Insets,
): Insets {
  var ins = insets
  when (border) {
    null -> ins.set(0, 0, 0, 0)
    is AbstractBorder -> ins = border.getBorderInsets(c, ins)
    else -> {
      val i = border.getBorderInsets(c)
      ins.set(i.top, i.left, i.bottom, i.right)
    }
  }
  return ins
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
