package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.math.BigDecimal
import javax.swing.*

fun makeUI(): Component {
  val font = Font(Font.SERIF, Font.PLAIN, 24)
  val label1 = FiveStarRatingLabel("3.5")
  label1.border = BorderFactory.createTitledBorder("3.5")
  label1.font = font

  val label2 = FiveStarRatingLabel("4.3")
  label2.border = BorderFactory.createTitledBorder("4.3")
  label2.font = font

  val label3 = FiveStarRatingLabel("5")
  label3.border = BorderFactory.createTitledBorder("5")
  label3.font = font

  return JPanel(GridLayout(0, 1)).also {
    it.add(label1)
    it.add(label2)
    it.add(label3)
    it.preferredSize = Dimension(320, 240)
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

private class FiveStarRatingLabel(rating: String) : JComponent() {
  private val ip: Int
  private val fp: Int

  init {
    val bd = BigDecimal(rating)
    ip = bd.toInt()
    fp = bd.subtract(BigDecimal(ip)).multiply(BigDecimal.TEN).toInt()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val w = width
    val h = height
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    val frc = g2.fontRenderContext
    val gv = font.createGlyphVector(frc, STAR)
    val r = gv.visualBounds
    val cx = w / 2.0 - r.centerX
    val cy = h / 2.0 - r.centerY
    val toCenterAtf = AffineTransform.getTranslateInstance(cx, cy)
    var point = 0.0
    for (i in 0 until gv.numGlyphs) {
      val gm = gv.getGlyphMetrics(i)
      if (i <= ip - 1) {
        point += gm.advance.toDouble()
      } else if (i <= ip) {
        point += gm.bounds2D.width * fp / 10.0
      }
    }
    g2.paint = Color.GREEN
    val s = toCenterAtf.createTransformedShape(gv.outline)
    g2.draw(s)
    val clip = Rectangle2D.Double(r.x, r.y, point, r.height)
    g2.clip = toCenterAtf.createTransformedShape(clip)
    g2.fill(s)
    g2.dispose()
  }

  companion object {
    private const val STAR = "ššššš"
  }
}
