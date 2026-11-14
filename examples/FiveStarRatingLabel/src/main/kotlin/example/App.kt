package example

import java.awt.*
import java.awt.font.GlyphVector
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.math.BigDecimal
import javax.swing.*

fun createUI(): Component {
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

private class FiveStarRatingLabel(
  rating: String,
) : JComponent() {
  private val intPart: Int
  private val fracPart: Int

  init {
    val bd = BigDecimal(rating)
    intPart = bd.toInt()
    // the first decimal place of the rating as an integer: "3.5" -> 5
    fracPart = bd.subtract(BigDecimal(intPart)).movePointRight(1).toInt()
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val w = width
    val h = height
    g.color = Color.WHITE
    g.fillRect(0, 0, w, h)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val frc = g2.fontRenderContext
    val gv = font.createGlyphVector(frc, STAR)
    val r = gv.visualBounds
    val cx = w / 2.0 - r.centerX
    val cy = h / 2.0 - r.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(cx, cy)
    val fillWidth = getFillWidth(gv)
    g2.paint = Color.GREEN
    val s = toCenterAt.createTransformedShape(gv.outline)
    g2.draw(s)
    val clip = Rectangle2D.Double(r.x, r.y, fillWidth, r.height)
    g2.clip = toCenterAt.createTransformedShape(clip)
    g2.fill(s)
    g2.dispose()
  }

  private fun getFillWidth(gv: GlyphVector): Double {
    // the glyph position equals the sum of the advances of the preceding glyphs,
    // and getGlyphPosition(numGlyphs) returns the end position
    var fillWidth = gv.getGlyphPosition(intPart).x
    if (intPart < gv.numGlyphs) {
      val gm = gv.getGlyphMetrics(intPart)
      fillWidth += gm.bounds2D.width * fracPart / 10.0
    }
    return fillWidth
  }

  companion object {
    private const val STAR = "★★★★★"
  }
}
