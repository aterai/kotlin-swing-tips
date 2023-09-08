package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.LayerUI

fun makeUI(): Component {
  val informationIcon = UIManager.getIcon("OptionPane.informationIcon")
  val errorIcon = UIManager.getIcon("OptionPane.errorIcon")
  val questionIcon = UIManager.getIcon("OptionPane.questionIcon")
  val warningIcon = UIManager.getIcon("OptionPane.warningIcon")
  val information = BadgeLabel(informationIcon, BadgePosition.SOUTH_EAST, 0)
  val error = BadgeLabel(errorIcon, BadgePosition.SOUTH_EAST, 8)
  val question = BadgeLabel(questionIcon, BadgePosition.SOUTH_WEST, 64)
  val warning = BadgeLabel(warningIcon, BadgePosition.NORTH_EAST, 256)
  val information2 = BadgeLabel(informationIcon, BadgePosition.NORTH_WEST, 1024)

  val p = JPanel(GridLayout(2, 5))
  val ui = BadgeLayerUI()
  listOf(information, error, question, warning, information2).forEach {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    p.add(JLayer(it, ui))
  }

  val ui2 = BadgeIconLayerUI()
  listOf(informationIcon, errorIcon, questionIcon, warningIcon).map {
    BadgeLabel(it, BadgePosition.SOUTH_EAST, 128)
  }.forEach {
    it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
    p.add(JLayer(it, ui2))
  }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class BadgeLabel(
  image: Icon?,
  val badgePosition: BadgePosition,
  val counter: Int
) : JLabel(image)

private open class BadgeLayerUI : LayerUI<BadgeLabel>() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(g: Graphics, c: JComponent) {
    super.paint(g, c)
    val label = (c as? JLayer<*>)?.view
    if (label is BadgeLabel) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      iconRect.setBounds(0, 0, 0, 0)
      textRect.setBounds(0, 0, 0, 0)
      SwingUtilities.calculateInnerArea(label, viewRect)
      SwingUtilities.layoutCompoundLabel(
        label,
        label.getFontMetrics(label.font),
        label.text,
        label.icon,
        label.verticalAlignment,
        label.horizontalAlignment,
        label.verticalTextPosition,
        label.horizontalTextPosition,
        viewRect,
        iconRect,
        textRect,
        label.iconTextGap,
      )
      val badge = getBadgeIcon(label.counter)
      val pt = getBadgeLocation(label.badgePosition, badge)
      g2.translate(pt.x, pt.y)
      badge.paintIcon(label, g2, 0, 0)
      g2.dispose()
    }
  }

  open fun getBadgeIcon(count: Int) =
    BadgeIcon(count, Color.WHITE, Color(0xAA_FF_16_16.toInt(), true))

  protected fun getBadgeLocation(pos: BadgePosition, icon: Icon): Point {
    var x = 0
    var y = 0
    when (pos) {
      BadgePosition.NORTH_WEST -> {
        x = iconRect.x - OFFSET.x
        y = iconRect.y - OFFSET.y
      }
      BadgePosition.NORTH_EAST -> {
        x = iconRect.x + iconRect.width - icon.iconWidth + OFFSET.x
        y = iconRect.y - OFFSET.y
      }
      BadgePosition.SOUTH_WEST -> {
        x = iconRect.x - OFFSET.x
        y = iconRect.y + iconRect.height - icon.iconHeight + OFFSET.y
      }
      BadgePosition.SOUTH_EAST -> {
        x = iconRect.x + iconRect.width - icon.iconWidth + OFFSET.x
        y = iconRect.y + iconRect.height - icon.iconHeight + OFFSET.y
      }
    }
    return Point(x, y)
  }

  companion object {
    private val OFFSET = Point(6, 2)
  }
}

private class BadgeIconLayerUI : BadgeLayerUI() {
  override fun getBadgeIcon(count: Int) =
    object : BadgeIcon(count, Color.WHITE, Color(0xAA_16_16_16.toInt(), true)) {
      override val badgeShape: Shape
        get() = RoundRectangle2D.Double(
          0.0,
          0.0,
          iconWidth.toDouble(),
          iconHeight.toDouble(),
          5.0,
          5.0,
        )
    }
}

private open class BadgeIcon(
  private val value: Int,
  private val badgeFgc: Color,
  private val badgeBgc: Color
) : Icon {
  open val badgeShape: Shape
    get() = Ellipse2D.Double(0.0, 0.0, iconWidth.toDouble(), iconHeight.toDouble())

  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    if (value <= 0) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    val badge = badgeShape
    g2.paint = badgeBgc
    g2.fill(badge)
    g2.paint = badgeBgc.darker()
    g2.draw(badge)
    g2.paint = badgeFgc

    val frc = g2.fontRenderContext
    val txt = if (value > 999) "1K" else value.toString()
    val at = if (txt.length < 3) null else AffineTransform.getScaleInstance(.66, 1.0)
    val shape = TextLayout(txt, g2.font, frc).getOutline(at)
    val b = shape.bounds
    val tx = iconWidth / 2.0 - b.centerX
    val ty = iconHeight / 2.0 - b.centerY
    val toCenterAT = AffineTransform.getTranslateInstance(tx, ty)
    g2.fill(toCenterAT.createTransformedShape(shape))
    g2.dispose()
  }

  override fun getIconWidth() = 17

  override fun getIconHeight() = 17
}

private enum class BadgePosition {
  NORTH_WEST,
  NORTH_EAST,
  SOUTH_EAST,
  SOUTH_WEST
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
