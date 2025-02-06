package example

import java.awt.*
import java.awt.geom.AffineTransform
import javax.swing.*
import javax.swing.border.AbstractBorder
import javax.swing.border.Border
import javax.swing.border.TitledBorder

fun makeUI(): Component {
  val p1 = JPanel(BorderLayout())
  p1.add(JScrollPane(JTree()))
  p1.border = TitledBorder("TitledBorder 1234567890")
  val p2 = JPanel(BorderLayout())
  p2.add(JScrollPane(JTree()))
  p2.border = VerticalTitledBorder("VerticalTitledBorder 1234567890")
  val p3 = JPanel(BorderLayout())
  p3.add(JScrollPane(JTree()))
  p3.border = TitledBorder(VerticalTitledBorder("VerticalTitledBorder"), "TitledBorder")
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
    this.label.isOpaque = true
    // this.label.putClientProperty(BasicHTML.propertyKey, null)
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
      val lbl = getTitleLabel(c)
      val size = lbl.preferredSize
      val insets = makeComponentBorderInsets(border, c, Insets(0, 0, 0, 0))
      var borderX = x + edge
      val borderY = y + edge
      var borderW = width - edge - edge
      val borderH = height - edge - edge
      val labelH = size.height
      var labelW = height - insets.top - insets.bottom // TEST: - (edge * 8)
      if (labelW > size.width) {
        labelW = size.width
      }
      val left = edge + insets.left / 2 - labelH / 2
      if (left < edge) {
        borderX -= left
        borderW += left
      }
      border.paintBorder(c, g, borderX, borderY, borderW, borderH)
      val g2 = g.create() as? Graphics2D ?: return
      g2.translate(0.0, (height + labelW) / 2.0)
      g2.transform(AffineTransform.getQuadrantRotateInstance(-1))
      // or: g2.rotate(-PI / 2.0)
      lbl.setSize(labelW, labelH)
      lbl.paint(g2)
      g2.dispose()
    }
  }

  override fun getBorderInsets(
    c: Component,
    insets: Insets,
  ): Insets {
    val border = getBorder()
    val ins = makeComponentBorderInsets(border, c, insets)
    val title = getTitle()
    if (title?.isNotEmpty() == true) {
      val edge = if (border is TitledBorder) 0 else EDGE_SPACING
      val lbl = getTitleLabel(c)
      val size = lbl.preferredSize
      if (ins.left < size.height) {
        ins.left = size.height - edge
      }
      ins.top += edge + TEXT_SPACING
      ins.left += edge + TEXT_SPACING
      ins.right += edge + TEXT_SPACING
      ins.bottom += edge + TEXT_SPACING
    }
    return ins
  }

  // Copied from TitledBorder
  private fun getTitleColor(c: Component?): Color? {
    val tc = getTitleColor()
    val uic = UIManager.getColor("TitledBorder.titleColor")
    return when {
      tc != null -> tc
      uic != null -> uic
      else -> c?.foreground
    }
  }

  private fun getTitleLabel(c: Component?): JLabel {
    this.label.text = getTitle()
    this.label.font = getFont(c)
    this.label.foreground = getTitleColor(c)
    if (c != null) {
      this.label.componentOrientation = c.componentOrientation
      this.label.isEnabled = c.isEnabled
      this.label.background = c.background // ???
    }
    return this.label
  }

  companion object {
    // @see javax/swing/border/TitledBorder.java#getBorderInsets(Border border, Component c, Insets insets)
    private fun makeComponentBorderInsets(
      border: Border?,
      c: Component,
      i: Insets,
    ): Insets {
      var ins = Insets(i.top, i.left, i.bottom, i.right)
      when (border) {
        null -> ins.set(0, 0, 0, 0)
        is AbstractBorder -> ins = border.getBorderInsets(c, i)
      }
      return ins
    }
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
