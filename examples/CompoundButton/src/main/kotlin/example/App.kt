package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import javax.swing.*

fun createUI() = JPanel().also {
  val d = Dimension(64, 64)
  it.add(CompoundButton(d, ButtonLocation.NORTH))
  it.add(CompoundButton(d, ButtonLocation.SOUTH))
  it.add(CompoundButton(d, ButtonLocation.EAST))
  it.add(CompoundButton(d, ButtonLocation.WEST))
  it.add(CompoundButton(d, ButtonLocation.CENTER))
  it.add(CompoundButtonPanel(d))
  it.preferredSize = Dimension(320, 240)
}

private class CompoundButtonPanel(
  private val dim: Dimension,
) : JComponent() {
  init {
    layout = OverlayLayout(this)
    add(CompoundButton(dim, ButtonLocation.CENTER))
    add(CompoundButton(dim, ButtonLocation.NORTH))
    add(CompoundButton(dim, ButtonLocation.SOUTH))
    add(CompoundButton(dim, ButtonLocation.EAST))
    add(CompoundButton(dim, ButtonLocation.WEST))
  }

  override fun getPreferredSize() = dim

  override fun isOptimizedDrawingEnabled() = false
}

private enum class ButtonLocation(
  val startAngle: Double,
) {
  CENTER(0.0),
  NORTH(45.0),
  EAST(135.0),
  SOUTH(225.0),
  WEST(-45.0),
}

private class CompoundButton(
  private val dim: Dimension,
  private val location: ButtonLocation,
) : JButton() {
  private var shape: Shape? = null
  private val shapeSize = Dimension()

  init {
    icon = object : Icon {
      private val focusColor = Color(100, 150, 255, 200)
      private val armedColor = Color(230, 230, 230)
      private val rolloverColor = Color.ORANGE

      // The shape is in the button's coordinate space, so x and y are ignored
      override fun paintIcon(
        c: Component?,
        g: Graphics,
        x: Int,
        y: Int,
      ) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON,
        )
        if (getModel().isArmed) {
          g2.paint = armedColor
          g2.fill(getShape())
        } else if (isRolloverEnabled && getModel().isRollover) {
          paintFocusAndRollover(g2, rolloverColor)
        } else if (hasFocus()) {
          paintFocusAndRollover(g2, focusColor)
        } else {
          g2.paint = background
          g2.fill(getShape())
        }
        g2.dispose()
      }

      override fun getIconWidth() = dim.width

      override fun getIconHeight() = dim.height
    }
    isFocusPainted = false
    isContentAreaFilled = false
    background = Color(0xFA_FA_FA)
  }

  override fun getPreferredSize() = dim

  // Create the shape lazily and rebuild it only when the size changes,
  // so that contains(...) also works before the first paint
  private fun getShape(): Shape {
    val w = width
    val h = height
    val s = shape
    return if (s == null || shapeSize.width != w || shapeSize.height != h) {
      shapeSize.setSize(w, h)
      createShape(w, h).also { shape = it }
    } else {
      s
    }
  }

  private fun createShape(
    w: Int,
    h: Int,
  ): Shape {
    val ww = w * .5
    val xx = ww * .5
    val inner = Ellipse2D.Double(xx, xx, ww, ww)
    return if (ButtonLocation.CENTER == location) {
      inner
    } else {
      // The parent panel must return false from isOptimizedDrawingEnabled()
      // because the bounds of these buttons overlap each other
      val outer = Arc2D.Double(
        1.0,
        1.0,
        w - 2.0,
        h - 2.0,
        location.startAngle,
        90.0,
        Arc2D.PIE,
      )
      val area = Area(outer)
      area.subtract(Area(inner))
      area
    }
  }

  private fun paintFocusAndRollover(
    g2: Graphics2D,
    color: Color,
  ) {
    val x2 = width - 1f
    val y2 = height - 1f
    g2.paint = GradientPaint(0f, 0f, color, x2, y2, color.brighter(), true)
    g2.fill(getShape())
  }

  override fun paintBorder(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = Color.GRAY
    g2.draw(getShape())
    g2.dispose()
  }

  override fun contains(
    x: Int,
    y: Int,
  ) = getShape().contains(x.toDouble(), y.toDouble())
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
