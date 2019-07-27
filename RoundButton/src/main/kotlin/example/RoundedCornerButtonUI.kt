package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicButtonListener
import javax.swing.plaf.basic.BasicButtonUI

class RoundedCornerButtonUI : BasicButtonUI() {
  protected val fc = Color(100, 150, 255)
  protected val ac = Color(220, 225, 230)
  protected val rc = Color.ORANGE
  private var shape: Shape? = null
  private var border: Shape? = null
  private var base: Shape? = null

  protected override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.setContentAreaFilled(false)
    b.setBorderPainted(false)
    b.setOpaque(false)
    b.setBackground(Color(245, 250, 255))
    b.setBorder(BorderFactory.createEmptyBorder(4, 12, 4, 12))
    initShape(b)
  }

  protected override fun installListeners(button: AbstractButton) {
    val listener = object : BasicButtonListener(button) {
      override fun mousePressed(e: MouseEvent) {
        val b = e.getComponent() as? AbstractButton ?: return@mousePressed
        initShape(b)
        if (isShapeContains(e.getPoint())) {
          super.mousePressed(e)
        }
      }

      override fun mouseEntered(e: MouseEvent) {
        if (isShapeContains(e.getPoint())) {
          super.mouseEntered(e)
        }
      }

      override fun mouseMoved(e: MouseEvent) {
        if (isShapeContains(e.getPoint())) {
          super.mouseEntered(e)
        } else {
          super.mouseExited(e)
        }
      }
    }
    // if (listener != null)
    button.addMouseListener(listener)
    button.addMouseMotionListener(listener)
    button.addFocusListener(listener)
    button.addPropertyChangeListener(listener)
    button.addChangeListener(listener)
  }

  override fun paint(g: Graphics, c: JComponent) {
    initShape(c)

    val g2 = g.create() as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    // ContentArea
    if (c is AbstractButton) {
      val model = c.getModel()
      if (model.isArmed()) {
        g2.setPaint(ac)
        g2.fill(shape)
      } else if (c.isRolloverEnabled() && model.isRollover()) {
        paintFocusAndRollover(g2, c, rc)
      } else if (c.hasFocus()) {
        paintFocusAndRollover(g2, c, fc)
      } else {
        g2.setPaint(c.getBackground())
        g2.fill(shape)
      }
    }

    // Border
    g2.setPaint(c.getForeground())
    g2.draw(shape)
    g2.dispose()
    super.paint(g, c)
  }

  // protected fun isShapeContains(pt: Point): Boolean {
  //   val s = shape
  //   return s is Shape && s.contains(pt.getX(), pt.getY())
  // }
  protected fun isShapeContains(pt: Point) = shape?.contains(pt.getX(), pt.getY()) ?: false

  protected fun initShape(c: Component) {
    if (c.getBounds() != base) {
      base = c.getBounds()
      shape = RoundRectangle2D.Double(0.0, 0.0, c.getWidth() - 1.0, c.getHeight() - 1.0, ARC_WIDTH, ARC_HEIGHT)
      border = RoundRectangle2D.Double(
          FOCUS_STROKE, FOCUS_STROKE,
          c.getWidth() - 1 - FOCUS_STROKE * 2, c.getHeight() - 1 - FOCUS_STROKE * 2,
          ARC_WIDTH, ARC_HEIGHT)
    }
  }

  protected fun paintFocusAndRollover(g2: Graphics2D, c: Component, color: Color) {
    g2.setPaint(GradientPaint(0f, 0f, color, c.getWidth() - 1f, c.getHeight() - 1f, color.brighter(), true))
    g2.fill(shape)
    g2.setPaint(c.getBackground())
    g2.fill(border)
  }

  companion object {
    private const val ARC_WIDTH = 16.0
    private const val ARC_HEIGHT = 16.0
    private const val FOCUS_STROKE = 2.0
  }
}
