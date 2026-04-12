package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.time.LocalTime
import java.time.ZoneId
import javax.swing.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min

private val CARD_UPPER_BG = Color(45, 45, 45)
private val CARD_LOWER_BG = Color(25, 25, 25)
private val TEXT_COLOR = Color(230, 230, 230)
private val SLIT_COLOR = Color(10, 10, 10, 220)
private val EDGE_LIGHT = Color(255, 255, 255, 60)
private val BG_WINDOW = Color(10, 10, 10)
private const val SHADOW_MAX_ALPHA = 130
private const val PNL_WIDTH = 80
private const val PNL_HEIGHT = 100
private const val SLIT_HEIGHT = 4
private const val FONT_SIZE = 64
private const val FONT_NAME = "Impact" // "Impact" or "Arial"

fun makeUI(): Component {
  val now = LocalTime.now(ZoneId.systemDefault())
  val hourPair = FlipPair(now.hour)
  val minPair = FlipPair(now.minute)
  val secPair = FlipPair(now.second)
  val p = JPanel(FlowLayout(FlowLayout.CENTER, 2, 2))
  p.setOpaque(false)
  p.add(hourPair)
  p.add(makeColonLabel(TEXT_COLOR))
  p.add(minPair)
  p.add(makeColonLabel(TEXT_COLOR))
  p.add(secPair)
  Timer(100) {
    val t = LocalTime.now(ZoneId.systemDefault())
    hourPair.setValue(t.hour)
    minPair.setValue(t.minute)
    secPair.setValue(t.second)
  }.start()
  return JPanel(GridBagLayout()).also {
    it.add(p)
    it.setBackground(BG_WINDOW)
    it.preferredSize = Dimension(320, 240)
  }
}

fun makeColonLabel(c: Color?): JLabel {
  val colon = JLabel(":")
  colon.setFont(colon.getFont().deriveFont(Font.BOLD, FONT_SIZE.toFloat()))
  colon.setForeground(c)
  return colon
}

private class FlipPair(
  private var currentVal: Int,
) : JPanel() {
  private var nextVal: Int
  private var angle = 0.0
  private var isAnimating = false
  private val animTimer = Timer(16, null)

  init {
    this.nextVal = currentVal
    animTimer.addActionListener {
      angle -= 15.0
      if (angle <= 0) {
        angle = 0.0
        isAnimating = false
        currentVal = nextVal
        animTimer.stop()
      }
      repaint()
    }
  }

  fun setValue(newValue: Int) {
    if (newValue != nextVal && !isAnimating) {
      nextVal = newValue
      angle = 180.0
      isAnimating = true
      animTimer.start()
    }
  }

  override fun getPreferredSize(): Dimension {
    val d = super.getPreferredSize()
    d.setSize(PNL_WIDTH, PNL_HEIGHT)
    return d
  }

  override fun isOpaque() = false

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setRenderingHint(
      RenderingHints.KEY_TEXT_ANTIALIASING,
      RenderingHints.VALUE_TEXT_ANTIALIAS_ON,
    )
    g2.font = Font(FONT_NAME, Font.PLAIN, FONT_SIZE)
    g2.font = g2.font.deriveFont(AffineTransform.getScaleInstance(1.0, 1.5))

    val cx = getWidth() / 2
    val cy = getHeight() / 2
    val curStr = "%02d".format(currentVal)
    val nxtStr = "%02d".format(nextVal)
    drawHalf(g2, nxtStr, cx, cy, true, CARD_UPPER_BG)
    drawHalf(g2, curStr, cx, cy, false, CARD_LOWER_BG)
    if (isAnimating) {
      val rad = Math.toRadians(angle)
      val scaleY = abs(cos(rad))
      g2.translate(cx, cy)
      g2.scale(1.0, scaleY)
      val alpha = ((1.0 - scaleY) * SHADOW_MAX_ALPHA).toInt()
      val isTop = angle > 90
      val cbg = if (isTop) CARD_LOWER_BG else CARD_UPPER_BG
      drawHalf(g2, curStr, 0, 0, isTop, cbg)
      drawShadow(g2, isTop, alpha)
    }
    drawSeparator(g2, cx, cy)
    g2.dispose()
  }

  private fun drawSeparator(g2: Graphics2D, cx: Int, cy: Int) {
    val sx = cx - PNL_WIDTH / 2
    val sy = cy - SLIT_HEIGHT / 2
    g2.color = SLIT_COLOR
    g2.fillRect(sx, sy, PNL_WIDTH, SLIT_HEIGHT)
    g2.color = EDGE_LIGHT
    g2.drawLine(sx + 5, sy, sx + PNL_WIDTH - 5, sy)
  }

  private fun drawHalf(
    g: Graphics,
    txt: String,
    cx: Int,
    cy: Int,
    isTop: Boolean,
    bg: Color,
  ) {
    val x = cx - PNL_WIDTH / 2
    val height = PNL_HEIGHT / 2 - SLIT_HEIGHT / 2
    if (isTop) {
      g.setClip(x, cy - PNL_HEIGHT / 2, PNL_WIDTH, height)
    } else {
      g.setClip(x, cy + SLIT_HEIGHT / 2, PNL_WIDTH, height)
    }
    g.color = bg
    g.fillRoundRect(x, cy - PNL_HEIGHT / 2, PNL_WIDTH, PNL_HEIGHT, 18, 18)
    g.color = TEXT_COLOR
    val fm = g.fontMetrics
    g.drawString(txt, cx - fm.stringWidth(txt) / 2, cy + fm.ascent / 2 - 12)
  }

  private fun drawShadow(g: Graphics, isTop: Boolean, alpha: Int) {
    g.color = Color(0, 0, 0, min(255, alpha))
    val h = PNL_HEIGHT / 2 - SLIT_HEIGHT / 2
    if (isTop) {
      g.fillRect(-PNL_WIDTH / 2, -PNL_HEIGHT / 2, PNL_WIDTH, h)
    } else {
      g.fillRect(-PNL_WIDTH / 2, SLIT_HEIGHT / 2, PNL_WIDTH, h)
    }
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
