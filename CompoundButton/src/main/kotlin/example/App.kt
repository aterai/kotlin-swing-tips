package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI() = JPanel().also {
  val d = Dimension(64, 64)
  it.add(CompoundButton(d, ButtonLocation.NORTH))
  it.add(CompoundButton(d, ButtonLocation.SOUTH))
  it.add(CompoundButton(d, ButtonLocation.EAST))
  it.add(CompoundButton(d, ButtonLocation.WEST))
  it.add(CompoundButton(d, ButtonLocation.CENTER))
  it.add(CompoundButtonPanel(d))
  it.preferredSize = Dimension(320, 240)
}

private class CompoundButtonPanel(private val dim: Dimension) : JComponent() {
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

private enum class ButtonLocation(val startAngle: Double) {
  CENTER(0.0),
  NORTH(45.0),
  EAST(135.0),
  SOUTH(225.0),
  WEST(-45.0)
}

private class CompoundButton(private val dim: Dimension, private val bl: ButtonLocation) : JButton() {
  @Transient private var shape: Shape? = null
  @Transient private var base: Shape? = null

  init {
    icon = object : Icon {
      private val fc = Color(100, 150, 255, 200)
      private val ac = Color(230, 230, 230)
      private val rc = Color.ORANGE
      override fun paintIcon(
        c: Component?,
        g: Graphics,
        x: Int,
        y: Int
      ) {
        val g2 = g.create() as? Graphics2D ?: return
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        if (getModel().isArmed) {
          g2.paint = ac
          g2.fill(shape)
        } else if (isRolloverEnabled && getModel().isRollover) {
          paintFocusAndRollover(g2, rc)
        } else if (hasFocus()) {
          paintFocusAndRollover(g2, fc)
        } else {
          g2.paint = background
          g2.fill(shape)
        }
        g2.dispose()
      }

      override fun getIconWidth() = dim.width

      override fun getIconHeight() = dim.height
    }
    isFocusPainted = false
    isContentAreaFilled = false
    background = Color(0xFA_FA_FA)
    initShape()
  }

  override fun getPreferredSize() = dim

  private fun initShape() {
    if (bounds != base) {
      base = bounds
      val ww = width * .5
      val xx = ww * .5
      val inner = Ellipse2D.Double(xx, xx, ww, ww)
      shape = if (ButtonLocation.CENTER == bl) {
        inner
      } else {
        val dw = width - 2.0
        val dh = height - 2.0
        val outer = Arc2D.Double(1.0, 1.0, dw, dh, bl.startAngle, 90.0, Arc2D.PIE)
        val area = Area(outer)
        area.subtract(Area(inner))
        area
      }
    }
  }

  private fun paintFocusAndRollover(g2: Graphics2D, color: Color) {
    g2.paint = GradientPaint(0f, 0f, color, width - 1f, height - 1f, color.brighter(), true)
    g2.fill(shape)
    g2.paint = background
  }

  override fun paintComponent(g: Graphics) {
    initShape()
    super.paintComponent(g)
  }

  override fun paintBorder(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.GRAY
    g2.draw(shape)
    g2.dispose()
  }

  override fun contains(x: Int, y: Int) = shape?.contains(x.toDouble(), y.toDouble()) ?: false
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
