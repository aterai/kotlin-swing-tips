package example

import java.awt.*
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicButtonUI

class RoundedCornerButtonUI : BasicButtonUI() {
  private val cachedSize = Dimension()
  private var shape: Shape = RoundRectangle2D.Double()
  private var innerShape: Shape = RoundRectangle2D.Double()

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.isContentAreaFilled = false
    b.isBorderPainted = false
    b.isOpaque = false
    b.background = Color(245, 250, 255)
    b.border = BorderFactory.createEmptyBorder(4, 12, 4, 12)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    updateShapeIfResized(c)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )

    // ContentArea
    if (c is AbstractButton) {
      val model = c.model
      if (model.isArmed) {
        g2.paint = PRESSED_COLOR
        g2.fill(shape)
      } else if (c.isRolloverEnabled && model.isRollover) {
        paintFocusAndRollover(g2, c, ROLLOVER_COLOR)
      } else if (c.hasFocus()) {
        paintFocusAndRollover(g2, c, FOCUS_COLOR)
      } else {
        g2.paint = c.background
        g2.fill(shape)
      }
    }

    // Border
    g2.paint = c.foreground
    g2.draw(shape)
    g2.dispose()
    super.paint(g, c)
  }

  // JComponent#contains(int, int) delegates to this method, so mouse events
  // (press, rollover, etc.) outside the rounded corners are not dispatched to the button.
  override fun contains(
    c: JComponent,
    x: Int,
    y: Int,
  ): Boolean {
    updateShapeIfResized(c)
    return shape.contains(x.toDouble(), y.toDouble())
  }

  private fun updateShapeIfResized(c: Component) {
    if (cachedSize != c.size) {
      c.getSize(cachedSize)
      val w = c.width - 1.0
      val h = c.height - 1.0
      val s = FOCUS_STROKE
      shape = RoundRectangle2D.Double(0.0, 0.0, w, h, ARC, ARC)
      innerShape = RoundRectangle2D.Double(s, s, w - s * 2.0, h - s * 2.0, ARC, ARC)
    }
  }

  private fun paintFocusAndRollover(
    g2: Graphics2D,
    c: Component,
    color: Color,
  ) {
    val w = c.width - 1f
    val h = c.height - 1f
    g2.paint = GradientPaint(0f, 0f, color, w, h, color.brighter(), true)
    g2.fill(shape)
    g2.paint = c.background
    g2.fill(innerShape)
  }

  companion object {
    private const val ARC = 16.0
    private const val FOCUS_STROKE = 2.0
    private val FOCUS_COLOR = Color(100, 150, 255)
    private val PRESSED_COLOR = Color(220, 225, 230)
    private val ROLLOVER_COLOR = Color.ORANGE
  }
}
