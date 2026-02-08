package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.roundToInt

fun makeUI(): Component {
  val slider = RangeSliderPanel(0, 100, 25, 75)
  return JPanel(GridBagLayout()).also {
    it.add(slider)
    it.preferredSize = Dimension(320, 240)
  }
}

private class TriangleUI(
  b: JSlider,
  private val isUpward: Boolean,
) : BasicSliderUI(b) {
  override fun paintTrack(g: Graphics?) {
    // nothing to paint
  }

  override fun paintFocus(g: Graphics?) {
    // nothing to paint
  }

  override fun calculateTrackBuffer() {
    if (slider.getOrientation() == JSlider.HORIZONTAL) {
      trackBuffer = RangeBar.PAD
    } else {
      super.calculateTrackBuffer()
    }
  }

  override fun paintThumb(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = Color(40, 44, 52)
    val r = SwingUtilities.calculateInnerArea(slider, null)
    val h = 8
    val x = thumbRect.x
    val y = if (isUpward) r.y else r.y + r.height - h
    val xps = intArrayOf(x, x + thumbRect.width / 2, x + thumbRect.width)
    val yps = if (isUpward) intArrayOf(y + h, y, y + h) else intArrayOf(y, y + h, y)
    g2.fillPolygon(xps, yps, xps.size)
    g2.dispose()
  }
}

private class RangeSliderPanel(
  min: Int,
  max: Int,
  lowInit: Int,
  highInit: Int,
) : JPanel(BorderLayout(0, 0)) {
  private val lowerSlider = createSlider(min, max, lowInit, true)
  private val upperSlider = createSlider(min, max, highInit, false)

  init {
    val cl = ChangeListener { e ->
      if (lowerSlider.value > upperSlider.value) {
        if (e.getSource() == lowerSlider) {
          lowerSlider.setValue(upperSlider.value)
        } else {
          upperSlider.setValue(lowerSlider.value)
        }
      }
      repaint()
    }
    lowerSlider.addChangeListener(cl)
    upperSlider.addChangeListener(cl)

    add(upperSlider, BorderLayout.NORTH)
    add(RangeBar(lowerSlider, upperSlider))
    add(lowerSlider, BorderLayout.SOUTH)
  }

  private fun createSlider(min: Int, max: Int, v: Int, isUp: Boolean): JSlider {
    return object : JSlider(min, max, v) {
      override fun updateUI() {
        super.updateUI()
        setUI(TriangleUI(this, isUp))
        setOpaque(false)
        setPaintTicks(false)
        setPaintLabels(false)
      }

      override fun getPreferredSize(): Dimension {
        val d = super.getPreferredSize()
        d.height = 10
        return d
      }
    }
  }
}

private class RangeBar(
  private val low: JSlider,
  private val up: JSlider,
) : JLabel() {
  private val dragStartPt = Point(0, 0)
  private var slLow = 0
  private var slUp = 0
  private var dragListener: MouseAdapter? = null

  override fun updateUI() {
    removeMouseListener(dragListener)
    removeMouseMotionListener(dragListener)
    super.updateUI()
    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
    dragListener = object : MouseAdapter() {
      override fun mousePressed(e: MouseEvent) {
        dragStartPt.location = e.getPoint()
        slLow = low.value
        slUp = up.value
        repaint()
      }

      override fun mouseReleased(e: MouseEvent?) {
        dragStartPt.setLocation(-100, -100)
        repaint()
      }

      override fun mouseDragged(e: MouseEvent) {
        if (dragStartPt.x >= 0) {
          updateRange(e.getX() - dragStartPt.x)
        }
        repaint()
      }
    }
    addMouseListener(dragListener)
    addMouseMotionListener(dragListener)
  }

  private fun updateRange(diff: Int) {
    val trackW = low.getWidth() - PAD * 2.0
    val range = low.maximum - low.minimum
    val delta = (diff * range / trackW).roundToInt()
    val ln = slLow + delta
    val un = slUp + delta
    if (ln >= low.minimum && un <= low.maximum) {
      low.setValue(ln)
      up.setValue(un)
    }
  }

  override fun getPreferredSize(): Dimension = Dimension(300, BAR_HEIGHT)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val w = getWidth() - PAD * 2 - 1
    val cy = getHeight() / 2
    paintTrack(g2, w, cy)
    paintTicks(g2, w, cy)
    val lx = getPositionX(low)
    val ux = getPositionX(up)
    paintRangeBar(g2, cy, lx, ux)
    paintNumber(g2, cy, lx, ux)
    g2.dispose()
  }

  private fun paintNumber(g2: Graphics2D, cy: Int, lx: Int, ux: Int) {
    g2.color = UIManager.getColor("Button.foreground")
    val txtLow = low.value.toString()
    val txtUp = up.value.toString()
    val fm = g2.fontMetrics
    val gap = 2
    val ty = cy + fm.ascent / 2 - 1
    g2.drawString(txtLow, lx - fm.stringWidth(txtLow) - gap, ty)
    g2.drawString(txtUp, ux + gap, ty)
  }

  private fun paintTrack(g2: Graphics2D, w: Int, cy: Int) {
    val barH = BAR_HEIGHT - 1f
    g2.color = TRACK_BGC
    val track = RoundRectangle2D.Float(
      PAD.toFloat(),
      cy - barH / 2f,
      w.toFloat(),
      barH,
      4f,
      4f,
    )
    g2.fill(track)
    g2.color = TRACK_BGC.darker()
    g2.draw(track)
  }

  private fun paintTicks(g2: Graphics2D, w: Int, cy: Int) {
    val barH = BAR_HEIGHT - 1
    var i = 0
    while (i <= 100) {
      val tx = PAD + i * w / 100
      if (i % 10 == 0) {
        // MajorTick
        g2.color = MAJOR_TICK_COLOR
        g2.drawLine(tx, cy - barH / 2, tx, cy + barH / 2)
      } else {
        // MinorTick
        g2.color = MINOR_TICK_COLOR
        g2.drawLine(tx, cy - 4, tx, cy + 4)
      }
      i += 2
    }
  }

  private fun paintRangeBar(g2: Graphics2D, cy: Int, lx: Int, ux: Int) {
    val barH = BAR_HEIGHT - 1f
    g2.paint = RANGE_COLOR
    val bar = RoundRectangle2D.Float(
      lx.toFloat(),
      cy - barH / 2f,
      (ux - lx).toFloat(),
      barH,
      4f,
      4f,
    )
    g2.fill(bar)
    g2.color = RANGE_COLOR.darker()
    g2.draw(bar)
  }

  private fun getPositionX(slider: JSlider): Int {
    val iv = slider.value - slider.minimum
    val range = slider.maximum - slider.minimum
    val v = iv.toDouble() / range
    val r = SwingUtilities.calculateInnerArea(slider, null)
    return PAD + (v * (r.width - PAD * 2.0)).toInt()
  }

  companion object {
    const val BAR_HEIGHT = 24
    const val PAD = 20
    private val MAJOR_TICK_COLOR = Color(180, 180, 185)
    private val MINOR_TICK_COLOR = Color(210, 210, 215)
    private val TRACK_BGC = Color(230, 230, 235)
    private val RANGE_COLOR = Color(0, 180, 255, 120)
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
