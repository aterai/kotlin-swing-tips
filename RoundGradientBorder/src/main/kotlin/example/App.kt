package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.border.EmptyBorder

fun makeUI(): Component {
  val label0 = JLabel("JLabel + MatteBorder(Gradient Icon)")
  val fractions = floatArrayOf(0f, .25f, .5f, .75f, 1f)
  val colors = arrayOf(
    Color(0xD3_03_02),
    Color(0xFF_51_56),
    Color(0xFF_DB_4E),
    Color(0x00_FE_9B),
    Color(0x2D_D9_FE),
  )
  val icon = object : Icon {
    override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
      val g2 = g.create() as? Graphics2D ?: return
      val start = Point2D.Float(0f, 0f)
      val end = Point2D.Float(c.width.toFloat(), 0f)
      g2.paint = LinearGradientPaint(start, end, fractions, colors)
      g2.fillRect(x, y, c.width, c.height)
      g2.dispose()
    }

    override fun getIconWidth() = label0.width

    override fun getIconHeight() = label0.height
  }
  label0.border = BorderFactory.createMatteBorder(2, 2, 2, 2, icon)

  val label1 = JLabel("JLabel + RoundGradientBorder")
  label1.border = RoundGradientBorder(5, 5, 5, 5)

  val label2 = object : JLabel("JLabel(240x120) + RoundGradientBorder") {
    override fun getPreferredSize() = Dimension(240, 120)
  }
  label2.border = RoundGradientBorder(16, 16, 16, 16)

  return JPanel().also {
    it.add(label0)
    it.add(label1)
    it.add(label2)
    it.preferredSize = Dimension(320, 240)
  }
}

private class RoundGradientBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int
) : EmptyBorder(top, left, bottom, right) {
  private val fractions = floatArrayOf(0f, .25f, .5f, .75f, 1f)
  private val colors = arrayOf(
    Color(0xD3_03_02),
    Color(0xFF_51_56),
    Color(0xFF_DB_4E),
    Color(0x00_FE_9B),
    Color(0x2D_D9_FE),
  )

  override fun paintBorder(c: Component, g: Graphics, x: Int, y: Int, width: Int, height: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val start = Point2D.Float(0f, 0f)
    val end = Point2D.Float(width.toFloat(), 0f)
    g2.paint = LinearGradientPaint(start, end, fractions, colors)
    val stroke = 2f
    val arc = 12f
    val outer = RoundRectangle2D.Float(
      x.toFloat(),
      y.toFloat(),
      width - 1f,
      height - 1f,
      arc,
      arc,
    )
    val inner = RoundRectangle2D.Float(
      x + stroke,
      y + stroke,
      width - stroke - stroke - 1f,
      height - stroke - stroke - 1f,
      arc - stroke - stroke,
      arc - stroke - stroke,
    )
    val rr = Area(outer)
    rr.subtract(Area(inner))
    g2.fill(rr)
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
