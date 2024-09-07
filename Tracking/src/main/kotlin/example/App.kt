package example

import java.awt.*
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import javax.swing.*

fun makeUI(): Component {
  val text = "1234567890"
  val l1 = JLabel("$text TRACKING_TIGHT (-.04f)")
  l1.font = l1.font.deriveFont(
    hashMapOf(TextAttribute.TRACKING to TextAttribute.TRACKING_TIGHT),
  )

  val l2 = JLabel("$text TRACKING_LOOSE (.04f)")
  l2.font = l2.font.deriveFont(
    hashMapOf(TextAttribute.TRACKING to TextAttribute.TRACKING_LOOSE),
  )

  val p0 = JPanel(GridLayout(0, 1, 5, 5))
  p0.border = BorderFactory.createTitledBorder("TextAttribute.TRACKING")
  listOf(JLabel("$text Default"), l1, l2).forEach { p0.add(it) }

  val c1 = Color(0xAA_FF_32_32.toInt(), true)
  val c2 = Color(0xAA_64_FF_64.toInt(), true)
  val c3 = Color(0xAA_32_32_FF.toInt(), true)

  val l3 = JLabel(BadgeIcon(128, Color.WHITE, c1))
  val l4 = JLabel(BadgeIcon(256, Color.BLACK, c2))
  val l5 = JLabel(BadgeIcon(1024, Color.WHITE, c3))
  val p1 = JPanel()
  p1.border = BorderFactory.createTitledBorder("Tracking: -0.1")
  listOf(l3, l4, l5).forEach { p1.add(it) }

  val l6 = JLabel(BadgeIcon2(128, Color.WHITE, c1))
  val l7 = JLabel(BadgeIcon2(256, Color.BLACK, c2))
  val l8 = JLabel(BadgeIcon2(1024, Color.WHITE, c3))
  val p2 = JPanel()
  p2.border = BorderFactory.createTitledBorder("Scaled along the X axis direction: 0.95")
  listOf(l6, l7, l8).forEach { p2.add(it) }

  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.add(p0)
  box.add(Box.createVerticalStrut(10))
  box.add(p2)
  box.add(Box.createVerticalStrut(10))
  box.add(p1)

  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BadgeIcon(
  val value: Int,
  val badgeFgc: Color,
  val badgeBgc: Color,
) : Icon {
  val text get() = if (value > 999) "1K+" else value.toString()

  val badgeShape get() = Ellipse2D.Double(
    0.0,
    0.0,
    iconWidth.toDouble(),
    iconHeight.toDouble(),
  )

  open fun getTextShape(g2: Graphics2D): Shape {
    val txt = text
    val attr = hashMapOf(TextAttribute.TRACKING to -.1f)
    val font = if (txt.length < 3) g2.font else g2.font.deriveFont(attr)
    val frc = g2.fontRenderContext
    return TextLayout(txt, font, frc).getOutline(null)
  }

  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    if (value <= 0) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    g2.translate(x, y)

    val badge = badgeShape
    g2.paint = badgeBgc
    g2.fill(badge)

    g2.paint = badgeFgc
    val shape = getTextShape(g2)
    val b = shape.bounds
    val tx = iconWidth / 2.0 - b.centerX
    val ty = iconHeight / 2.0 - b.centerY
    val toCenterAt = AffineTransform.getTranslateInstance(tx, ty)
    g2.fill(toCenterAt.createTransformedShape(shape))
    g2.dispose()
  }

  override fun getIconWidth() = 24

  override fun getIconHeight() = 24
}

private class BadgeIcon2(
  value: Int,
  fgc: Color,
  bgc: Color,
) : BadgeIcon(value, fgc, bgc) {
  override fun getTextShape(g2: Graphics2D): Shape {
    val txt = text
    val at = if (txt.length < 3) null else AffineTransform.getScaleInstance(.95, 1.0)
    val font = g2.font.deriveFont(at)
    val frc = g2.fontRenderContext
    return TextLayout(txt, font, frc).getOutline(null)
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
