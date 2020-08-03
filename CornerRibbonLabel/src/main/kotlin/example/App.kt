package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.FontRenderContext
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.sqrt

fun makeUI(): Component {
  val informationIcon = UIManager.getIcon("OptionPane.informationIcon")
  val errorIcon = UIManager.getIcon("OptionPane.errorIcon")
  val questionIcon = UIManager.getIcon("OptionPane.questionIcon")
  val warningIcon = UIManager.getIcon("OptionPane.warningIcon")

  val error = BadgeLabel(errorIcon)
  val error1 = BadgeLabel(errorIcon, "beta")
  // error1.text = "default"
  val question = BadgeLabel(questionIcon, "RC1")
  val warning = BadgeLabel(warningIcon, "beta")
  val information = BadgeLabel(informationIcon, "alpha")
  val information2 = BadgeLabel(informationIcon, "RC2")

  val p = JPanel(FlowLayout(FlowLayout.CENTER, 50, 50))
  listOf(error, error1, question, warning, information, information2).forEach { p.add(it) }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class BadgeLabel : JLabel {
  private val ribbonColor = Color(0xAA_FF_64_00.toInt(), true)
  private val ribbonText: String?

  constructor(image: Icon) : super(image) {
    ribbonText = null
  }

  constructor(image: Icon, ribbonText: String?) : super(image) {
    this.ribbonText = ribbonText
  }

  override fun updateUI() {
    super.updateUI()
    border = BorderFactory.createEmptyBorder(8, 8, 8, 8)
    verticalAlignment = SwingConstants.CENTER
    verticalTextPosition = SwingConstants.BOTTOM
    horizontalAlignment = SwingConstants.CENTER
    horizontalTextPosition = SwingConstants.CENTER
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = Color.WHITE
    g2.fill(getShape())
    super.paintComponent(g)

    if (ribbonText != null) {
      val d = size
      val fontSize = 10.0
      val cx = (d.width - fontSize) / 2.0
      val theta = Math.toRadians(45.0)

      val font = g2.font.deriveFont(fontSize.toFloat())
      g2.font = font
      val frc = FontRenderContext(null, true, true)

      val ribbon = Rectangle2D.Double(cx, -fontSize, d.width.toDouble(), fontSize)
      val at = AffineTransform.getRotateInstance(theta, cx, 0.0)
      g2.paint = ribbonColor
      g2.fill(at.createTransformedShape(ribbon))

      val tl = TextLayout(ribbonText, font, frc)
      g2.paint = Color.WHITE
      val r = tl.getOutline(null).bounds2D
      val dx = cx + (d.width - cx) / sqrt(2.0) - r.width / 2.0
      val dy = fontSize / 2.0 + r.y
      val tx = AffineTransform.getTranslateInstance(dx, dy)
      val s = tl.getOutline(tx)
      g2.fill(at.createTransformedShape(s))
    }
    g2.dispose()
  }

  override fun isOpaque() = false

  private fun getShape(): Shape {
    val d = size
    val r = d.width / 2.0
    return RoundRectangle2D.Double(0.0, 0.0, d.width - 1.0, d.height - 1.0, r, r)
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
