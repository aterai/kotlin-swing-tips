package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicButtonListener
import javax.swing.plaf.basic.BasicButtonUI

class RoundedCornerButtonUI : BasicButtonUI() {
  private val fc = Color(100, 150, 255)
  private val ac = Color(220, 225, 230)
  private val rc = Color.ORANGE
  private var shape: Shape? = null
  private var border: Shape? = null
  private var base: Shape? = null

  override fun installDefaults(b: AbstractButton) {
    super.installDefaults(b)
    b.isContentAreaFilled = false
    b.isBorderPainted = false
    b.isOpaque = false
    b.background = Color(245, 250, 255)
    b.border = BorderFactory.createEmptyBorder(4, 12, 4, 12)
    initShape(b)
  }

  override fun installListeners(button: AbstractButton) {
    val listener = object : BasicButtonListener(button) {
      override fun mousePressed(e: MouseEvent) {
        val b = e.component as? AbstractButton ?: return
        initShape(b)
        if (isShapeContains(e.point)) {
          super.mousePressed(e)
        }
      }

      override fun mouseEntered(e: MouseEvent) {
        if (isShapeContains(e.point)) {
          super.mouseEntered(e)
        }
      }

      override fun mouseMoved(e: MouseEvent) {
        if (isShapeContains(e.point)) {
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

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    initShape(c)

    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )

    // ContentArea
    if (c is AbstractButton) {
      val model = c.model
      if (model.isArmed) {
        g2.paint = ac
        g2.fill(shape)
      } else if (c.isRolloverEnabled && model.isRollover) {
        paintFocusAndRollover(g2, c, rc)
      } else if (c.hasFocus()) {
        paintFocusAndRollover(g2, c, fc)
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

  // private fun isShapeContains(pt: Point): Boolean {
  //   val s = shape
  //   return s is Shape && s.contains(pt)
  // }

  private fun isShapeContains(pt: Point) = shape?.contains(pt) ?: false

  private fun initShape(c: Component) {
    if (c.bounds != base) {
      base = c.bounds
      shape = RoundRectangle2D.Double(0.0, 0.0, c.width - 1.0, c.height - 1.0, ARC, ARC)
      border = RoundRectangle2D.Double(
        FOCUS_STROKE,
        FOCUS_STROKE,
        c.width - 1 - FOCUS_STROKE * 2,
        c.height - 1 - FOCUS_STROKE * 2,
        ARC,
        ARC,
      )
    }
  }

  private fun paintFocusAndRollover(
    g2: Graphics2D,
    c: Component,
    color: Color,
  ) {
    g2.paint = GradientPaint(
      0f,
      0f,
      color,
      c.width - 1f,
      c.height - 1f,
      color.brighter(),
      true,
    )
    g2.fill(shape)
    g2.paint = c.background
    g2.fill(border)
  }

  companion object {
    private const val ARC = 16.0
    private const val FOCUS_STROKE = 2.0
  }
}
