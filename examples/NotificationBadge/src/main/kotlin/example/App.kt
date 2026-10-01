package example

import java.awt.*
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.LayerUI

fun createUI(): Component {
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
  listOf(informationIcon, errorIcon, questionIcon, warningIcon)
    .map { BadgeLabel(it, BadgePosition.SOUTH_EAST, 128) }
    .forEach {
      it.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
      p.add(JLayer(it, ui2))
    }
  p.preferredSize = Dimension(320, 240)
  return p
}

private class BadgeLabel(
  image: Icon?,
  val badgePosition: BadgePosition,
  val count: Int,
) : JLabel(image)

private open class BadgeLayerUI : LayerUI<BadgeLabel>() {
  private val viewRect = Rectangle()
  private val iconRect = Rectangle()
  private val textRect = Rectangle()

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val label = (c as? JLayer<*>)?.view
    if (label is BadgeLabel) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
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
      val badge = getBadgeIcon(label.count)
      val pt = label.badgePosition.getLocation(iconRect, badge, OFFSET)
      badge.paintIcon(label, g2, pt.x, pt.y)
      g2.dispose()
    }
  }

  open fun getBadgeIcon(count: Int) =
    BadgeIcon(count, Color.WHITE, BADGE_BACKGROUND)

  companion object {
    private val OFFSET = Point(6, 2)
    private val BADGE_BACKGROUND = Color(0xAA_FF_16_16.toInt(), true)
  }
}

private class BadgeIconLayerUI : BadgeLayerUI() {
  override fun getBadgeIcon(count: Int) =
    object : BadgeIcon(count, Color.WHITE, BADGE_BACKGROUND) {
      override val badgeShape: Shape
        get() = RoundRectangle2D.Double(
          0.0,
          0.0,
          iconWidth - 1.0,
          iconHeight - 1.0,
          5.0,
          5.0,
        )
    }

  companion object {
    private val BADGE_BACKGROUND = Color(0xAA_16_16_16.toInt(), true)
  }
}

private open class BadgeIcon(
  private val value: Int,
  private val foreground: Color,
  private val background: Color,
) : Icon {
  // Subtract 1px so that the outline stroke stays within the icon bounds
  open val badgeShape: Shape
    get() = Ellipse2D.Double(0.0, 0.0, iconWidth - 1.0, iconHeight - 1.0)

  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create()
    if (value > 0 && g2 is Graphics2D) {
      g2.translate(x, y)
      val badge = badgeShape
      g2.paint = background
      g2.fill(badge)
      g2.paint = background.darker()
      g2.draw(badge)
      g2.paint = foreground
      val frc = g2.fontRenderContext
      val txt = if (value < 1_000) value.toString() else "${minOf(value / 1_000, 99)}K"
      val at = if (txt.length < 3) {
        null
      } else {
        AffineTransform.getScaleInstance(.66, 1.0)
      }
      val shape = TextLayout(txt, g2.font, frc).getOutline(at)
      val b = shape.bounds2D
      val r = badge.bounds2D
      val tx = r.centerX - b.centerX
      val ty = r.centerY - b.centerY
      val toCenterAt = AffineTransform.getTranslateInstance(tx, ty)
      g2.fill(toCenterAt.createTransformedShape(shape))
    }
    g2.dispose()
  }

  override fun getIconWidth() = SIZE

  override fun getIconHeight() = SIZE

  companion object {
    private const val SIZE = 17
  }
}

private enum class BadgePosition {
  NORTH_WEST {
    override fun getLocation(
      iconRect: Rectangle,
      icon: Icon,
      offset: Point,
    ) = Point(
      iconRect.x - offset.x,
      iconRect.y - offset.y,
    )
  },
  NORTH_EAST {
    override fun getLocation(
      iconRect: Rectangle,
      icon: Icon,
      offset: Point,
    ) = Point(
      iconRect.x + iconRect.width - icon.iconWidth + offset.x,
      iconRect.y - offset.y,
    )
  },
  SOUTH_EAST {
    override fun getLocation(
      iconRect: Rectangle,
      icon: Icon,
      offset: Point,
    ) = Point(
      iconRect.x + iconRect.width - icon.iconWidth + offset.x,
      iconRect.y + iconRect.height - icon.iconHeight + offset.y,
    )
  },
  SOUTH_WEST {
    override fun getLocation(
      iconRect: Rectangle,
      icon: Icon,
      offset: Point,
    ) = Point(
      iconRect.x - offset.x,
      iconRect.y + iconRect.height - icon.iconHeight + offset.y,
    )
  }, ;

  abstract fun getLocation(iconRect: Rectangle, icon: Icon, offset: Point): Point
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
