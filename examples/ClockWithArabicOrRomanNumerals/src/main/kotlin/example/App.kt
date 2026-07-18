package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.font.FontRenderContext
import java.awt.font.TextAttribute
import java.awt.font.TextLayout
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.awt.geom.Line2D
import java.awt.geom.Point2D
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*
import kotlin.math.max
import kotlin.math.min


fun createUI(): Component {
  val clock = AnalogClock()
  val attr = mapOf<TextAttribute, Any>(
    Pair(TextAttribute.TRACKING, -.08f),
  )
  clock.font = clock.font.deriveFont(20f).deriveFont(attr)

  val check = JCheckBox("roman", true)
  check.addActionListener { e ->
    clock.isRomanNumerals = (e.source as? JCheckBox)?.isSelected == true
    clock.repaint()
  }

  return JPanel(BorderLayout()).also {
    it.add(clock)
    it.add(check, BorderLayout.SOUTH)
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
      defaultCloseOperation = WindowConstants.DISPOSE_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}

private class AnalogClock : JPanel() {
  private val fontRatio = .2f
  private val arabicNumerals = arrayOf(
    "12",
    "1",
    "2",
    "3",
    "4",
    "5",
    "6",
    "7",
    "8",
    "9",
    "10",
    "11",
  )
  private val romanNumerals = arrayOf(
    "XII",
    "I",
    "II",
    "III",
    "IIII",
    "V",
    "VI",
    "VII",
    "VIII",
    "IX",
    "X",
    "XI",
  )
  private var secondRot = 0.0
  private var minuteRot = 0.0
  private var hourRot = 0.0
  private val timer = Timer(200) {
    val time = LocalTime.now(ZoneId.systemDefault())
    secondRot = time.second * Math.PI / 30.0
    minuteRot = time.minute * Math.PI / 30.0 + secondRot / 60.0
    hourRot = time.hour * Math.PI / 6.0 + minuteRot / 12.0
    repaint()
  }
  private var listener: HierarchyListener? = null
  var isRomanNumerals = true

  override fun updateUI() {
    removeHierarchyListener(listener)
    super.updateUI()
    listener = HierarchyListener { e ->
      if (e.changeFlags and HierarchyEvent.SHOWING_CHANGED.toLong() != 0L) {
        if (e.component.isShowing) {
          timer.start()
        } else {
          timer.stop()
        }
      }
    }
    addHierarchyListener(listener)
  }

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    initRenderingHints(g2)
    val rect = SwingUtilities.calculateInnerArea(this, null)
    paintBackground(g2, rect)
    val radius = min(rect.width, rect.height) / 2.0 - 10.0
    g2.translate(rect.centerX, rect.centerY)

    paintHourMarkers(g2, radius)
    paintClockNumbers(g2, radius)
    paintHourHand(g2, radius, hourRot)
    paintMinuteHand(g2, radius, minuteRot)
    paintSecondHand(g2, radius, secondRot)

    g2.dispose()
  }

  private fun paintClockNumbers(g2: Graphics2D, radius: Double) {
    val hourMarkerLen = radius / 6.0 - 10.0
    val at = AffineTransform.getRotateInstance(0.0)
    g2.color = Color.WHITE
    val dynamicFontSize = max((radius * fontRatio).toFloat(), 20f)
    val font = g2.font.deriveFont(dynamicFontSize)
    val frc = g2.fontRenderContext
    if (isRomanNumerals) {
      val si = AffineTransform.getScaleInstance(1.0, 2.0)
      for (txt in romanNumerals) {
        val s = getTextLayout(txt, font, frc).getOutline(si)
        val r = s.bounds2D
        val tx = r.centerX
        val ty = radius - hourMarkerLen - r.height + r.centerY * fontRatio
        val toCenter = AffineTransform.getTranslateInstance(-tx, -ty)
        g2.fill(at.createTransformedShape(toCenter.createTransformedShape(s)))
        at.rotate(Math.PI / 6.0)
      }
    } else {
      val ptSrc = Point2D.Double()
      for (txt in arabicNumerals) {
        val s = getTextLayout(txt, font, frc).getOutline(null)
        val r = s.bounds2D
        val ty = radius - hourMarkerLen - r.height - r.centerY * fontRatio
        ptSrc.setLocation(0.0, -ty)
        val pt = at.transform(ptSrc, null)
        val dx = pt.x - r.centerX
        val dy = pt.y - r.centerY
        val transform = AffineTransform.getTranslateInstance(dx, dy)
        g2.fill(transform.createTransformedShape(s))
        at.rotate(Math.PI / 6.0)
      }
    }
  }

  private fun initRenderingHints(g2: Graphics2D) {
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_STROKE_CONTROL,
      RenderingHints.VALUE_STROKE_PURE,
    )
  }

  private fun paintBackground(g2: Graphics2D, rect: Rectangle) {
    g2.color = Color.DARK_GRAY
    g2.fill(rect)
  }

  private fun paintHourMarkers(g2: Graphics2D, radius: Double) {
    val hourMarkerLen = radius / 6.0 - 10.0
    val hourMarker = Line2D.Double(0.0, hourMarkerLen - radius, 0.0, -radius)
    val minuteMarker = Line2D.Double(0.0, hourMarkerLen / 2.0 - radius, 0.0, -radius)
    val at = AffineTransform.getRotateInstance(0.0)
    g2.stroke = BasicStroke(2f)
    g2.color = Color.WHITE
    for (i in 0..59) {
      if (i % 5 == 0) {
        g2.draw(at.createTransformedShape(hourMarker))
      } else {
        g2.draw(at.createTransformedShape(minuteMarker))
      }
      at.rotate(Math.PI / 30.0)
    }
  }

  private fun getTextLayout(
    txt: String,
    font: Font,
    frc: FontRenderContext,
  ) = TextLayout(txt, font, frc)

  private fun paintHourHand(g2: Graphics2D, radius: Double, hourRot: Double) {
    val hourHandLen = radius / 2.0
    val hourHand = Line2D.Double(0.0, 0.0, 0.0, -hourHandLen)
    paintHand(g2, hourHand, 8f, Color.LIGHT_GRAY, hourRot)
  }

  private fun paintMinuteHand(g2: Graphics2D, radius: Double, minuteRot: Double) {
    val minuteHandLen = 5.0 * radius / 6.0
    val minuteHand = Line2D.Double(0.0, 0.0, 0.0, -minuteHandLen)
    paintHand(g2, minuteHand, 4f, Color.WHITE, minuteRot)
  }

  private fun paintSecondHand(g2: Graphics2D, radius: Double, secondRot: Double) {
    val r = radius / 6.0
    val secondHandLen = radius - r
    val secondHand = Line2D.Double(0.0, r, 0.0, -secondHandLen)
    paintHand(g2, secondHand, 1f, Color.RED, secondRot)
    g2.fill(Ellipse2D.Double(-r / 4.0, -r / 4.0, r / 2.0, r / 2.0))
  }

  private fun paintHand(
    g2: Graphics2D,
    hand: Shape,
    strokeWidth: Float,
    color: Color,
    rot: Double,
  ) {
    g2.stroke = BasicStroke(strokeWidth)
    g2.paint = color
    g2.draw(AffineTransform.getRotateInstance(rot).createTransformedShape(hand))
  }
}
