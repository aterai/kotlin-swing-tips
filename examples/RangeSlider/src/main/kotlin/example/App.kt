package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.roundToInt

fun createUI(): Component {
  val slider = RangeSliderPanel(0, 100, 25, 75)
  return JPanel(GridBagLayout()).also {
    it.add(slider)
    it.preferredSize = Dimension(320, 240)
  }
}

/**
 * A slider UI that paints only a small triangular thumb pointing up or down.
 * The track, ticks and labels are painted by the [RangeBar] instead.
 */
private class TriangleSliderUI(
  slider: JSlider,
  private val upward: Boolean,
) : BasicSliderUI(slider) {
  override fun installDefaults(slider: JSlider) {
    super.installDefaults(slider)
    // Some LookAndFeels (e.g. Windows) reserve 2px focus insets, which would
    // shift the thumb positions away from the values painted on the RangeBar.
    focusInsets = Insets(0, 0, 0, 0)
  }

  override fun getThumbSize() = Dimension(THUMB_WIDTH, THUMB_HEIGHT)

  override fun calculateTrackBuffer() {
    if (slider.orientation == JSlider.HORIZONTAL) {
      // Share the horizontal track range with the RangeBar
      trackBuffer = RangeBar.TRACK_PADDING
    } else {
      super.calculateTrackBuffer()
    }
  }

  override fun paintTrack(g: Graphics?) {
    // nothing to paint
  }

  override fun paintFocus(g: Graphics?) {
    // nothing to paint
  }

  override fun paintThumb(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = THUMB_COLOR
    // Anchor the apex to the edge of the thumb rectangle facing the RangeBar
    val apexY = if (upward) thumbRect.minY else thumbRect.maxY
    val baseY = if (upward) apexY + TRIANGLE_HEIGHT else apexY - TRIANGLE_HEIGHT
    val triangle = Path2D.Double()
    triangle.moveTo(thumbRect.minX, baseY)
    triangle.lineTo(thumbRect.centerX, apexY)
    triangle.lineTo(thumbRect.maxX, baseY)
    triangle.closePath()
    g2.fill(triangle)
    g2.dispose()
  }

  companion object {
    private val THUMB_COLOR = Color(0x28_2C_34)
    private const val THUMB_WIDTH = 11
    private const val THUMB_HEIGHT = 10
    private const val TRIANGLE_HEIGHT = 8
  }
}

private class RangeSliderPanel(
  min: Int,
  max: Int,
  lowerValue: Int,
  upperValue: Int,
) : JPanel(BorderLayout()) {
  private val lowerSlider = createSlider(min, max, lowerValue, true)
  private val upperSlider = createSlider(min, max, upperValue, false)

  init {
    val rangeBar = RangeBar(lowerSlider, upperSlider)
    val listener = ChangeListener { e ->
      clampToOtherSlider(e.source)
      rangeBar.repaint()
    }
    lowerSlider.addChangeListener(listener)
    upperSlider.addChangeListener(listener)

    add(upperSlider, BorderLayout.NORTH)
    add(rangeBar)
    add(lowerSlider, BorderLayout.SOUTH)
  }

  // Keep lower <= upper: the slider being moved stops at the other one
  private fun clampToOtherSlider(source: Any?) {
    val lower = lowerSlider.value
    val upper = upperSlider.value
    if (lower > upper) {
      if (source == lowerSlider) {
        lowerSlider.value = upper
      } else {
        upperSlider.value = lower
      }
    }
  }

  private fun createSlider(min: Int, max: Int, value: Int, upward: Boolean) =
    object : JSlider(min, max, value) {
      override fun updateUI() {
        super.updateUI()
        setUI(TriangleSliderUI(this, upward))
        isOpaque = false
      }
    }
}

/**
 * Paints the track, the tick marks, the selected range and its values
 * between the two sliders, and lets the user drag the whole range.
 */
private class RangeBar(
  private val lowerSlider: JSlider,
  private val upperSlider: JSlider,
) : JLabel() {
  private var mouseListener: MouseAdapter? = null
  private var dragging = false
  private var hovering = false
  private var dragStartX = 0
  private var lowerAtDragStart = 0
  private var upperAtDragStart = 0

  override fun updateUI() {
    removeMouseListener(mouseListener)
    removeMouseMotionListener(mouseListener)
    super.updateUI()
    mouseListener = RangeMouseListener()
    addMouseListener(mouseListener)
    addMouseMotionListener(mouseListener)
  }

  override fun getPreferredSize() = Dimension(300, BAR_HEIGHT)

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val track = getTrackBounds()
    paintTrack(g2, track)
    paintTicks(g2, track)
    val range = getRangeBounds()
    paintRange(g2, range)
    paintValues(g2, range)
    g2.dispose()
  }

  private fun paintTrack(g2: Graphics2D, track: Rectangle) {
    val shape = RoundRectangle2D.Double(
      track.x.toDouble(),
      track.y.toDouble(),
      track.width.toDouble(),
      track.height.toDouble(),
      ARC,
      ARC,
    )
    g2.color = TRACK_COLOR
    g2.fill(shape)
    g2.color = TRACK_COLOR.darker()
    g2.draw(shape)
  }

  private fun paintTicks(g2: Graphics2D, track: Rectangle) {
    val min = lowerSlider.minimum
    val max = lowerSlider.maximum
    val minorTop = track.y + (track.height - MINOR_TICK_LENGTH) / 2
    for (value in min..max step MINOR_TICK_STEP) {
      val x = valueToX(value)
      if ((value - min) % MAJOR_TICK_STEP == 0) {
        g2.color = MAJOR_TICK_COLOR
        g2.drawLine(x, track.y, x, track.y + track.height)
      } else {
        g2.color = MINOR_TICK_COLOR
        g2.drawLine(x, minorTop, x, minorTop + MINOR_TICK_LENGTH)
      }
    }
  }

  private fun paintRange(g2: Graphics2D, range: Rectangle) {
    val shape = RoundRectangle2D.Double(
      range.x.toDouble(),
      range.y.toDouble(),
      range.width.toDouble(),
      range.height.toDouble(),
      ARC,
      ARC,
    )
    g2.color = RANGE_COLOR
    g2.fill(shape)
    g2.color = RANGE_COLOR.darker()
    g2.draw(shape)
  }

  private fun paintValues(g2: Graphics2D, range: Rectangle) {
    g2.color = foreground
    val lowerText = lowerSlider.value.toString()
    val upperText = upperSlider.value.toString()
    val fm = g2.fontMetrics
    // Center the text vertically on the bar
    val baseline = range.centerY + (fm.ascent - fm.descent) / 2.0
    val y = baseline.roundToInt()
    g2.drawString(lowerText, range.x - fm.stringWidth(lowerText) - TEXT_GAP, y)
    g2.drawString(upperText, range.x + range.width + TEXT_GAP, y)
  }

  // The same width as the slider tracks, which use TRACK_PADDING as their
  // trackBuffer (see TriangleSliderUI#calculateTrackBuffer()).
  private fun getTrackWidth() = width - TRACK_PADDING * 2

  private fun getTrackBounds(): Rectangle {
    val height = BAR_HEIGHT - 1
    val y = (getHeight() - height) / 2
    return Rectangle(TRACK_PADDING, y, getTrackWidth() - 1, height)
  }

  private fun getRangeBounds(): Rectangle {
    val track = getTrackBounds()
    val lowerX = valueToX(lowerSlider.value)
    val upperX = valueToX(upperSlider.value)
    return Rectangle(lowerX, track.y, upperX - lowerX, track.height)
  }

  // Same mapping as BasicSliderUI#xPositionForValue(int) so that the values
  // painted here line up with the slider thumbs.
  private fun valueToX(value: Int): Int {
    val min = lowerSlider.minimum
    val max = lowerSlider.maximum
    val trackWidth = getTrackWidth()
    val pixelsPerValue = trackWidth.toDouble() / (max - min)
    val x = (pixelsPerValue * (value - min)).roundToInt()
    return TRACK_PADDING + minOf(x, trackWidth - 1)
  }

  // Slide the whole range by the horizontal drag distance, keeping its width.
  private fun moveRange(dx: Int) {
    val min = lowerSlider.minimum
    val max = lowerSlider.maximum
    val valuesPerPixel = (max - min) / getTrackWidth().toDouble()
    // Stop at the ends of the track instead of ignoring the drag
    val delta = (dx * valuesPerPixel)
      .roundToInt()
      .coerceIn(min - lowerAtDragStart, max - upperAtDragStart)
    val lower = lowerAtDragStart + delta
    val upper = upperAtDragStart + delta
    // RangeSliderPanel clamps lower <= upper on every change, so when moving
    // to the right the upper value has to be raised before the lower one.
    if (lower > lowerSlider.value) {
      upperSlider.value = upper
      lowerSlider.value = lower
    } else {
      lowerSlider.value = lower
      upperSlider.value = upper
    }
  }

  private fun setRangeHovered(hovered: Boolean) {
    if (hovering != hovered) {
      hovering = hovered
      cursor = Cursor.getPredefinedCursor(
        if (hovered) Cursor.HAND_CURSOR else Cursor.DEFAULT_CURSOR,
      )
    }
  }

  private inner class RangeMouseListener : MouseAdapter() {
    override fun mouseEntered(e: MouseEvent) {
      mouseMoved(e)
    }

    override fun mouseMoved(e: MouseEvent) {
      setRangeHovered(getRangeBounds().contains(e.point))
    }

    override fun mousePressed(e: MouseEvent) {
      if (SwingUtilities.isLeftMouseButton(e) && getRangeBounds().contains(e.point)) {
        dragging = true
        setRangeHovered(true)
        dragStartX = e.x
        lowerAtDragStart = lowerSlider.value
        upperAtDragStart = upperSlider.value
      }
    }

    override fun mouseDragged(e: MouseEvent) {
      if (dragging) {
        moveRange(e.x - dragStartX)
      }
    }

    override fun mouseReleased(e: MouseEvent) {
      if (dragging && SwingUtilities.isLeftMouseButton(e)) {
        dragging = false
      }
      mouseMoved(e)
    }

    override fun mouseExited(e: MouseEvent) {
      if (!dragging) {
        setRangeHovered(false)
      }
    }
  }

  companion object {
    const val BAR_HEIGHT = 24
    const val TRACK_PADDING = 20
    private const val MAJOR_TICK_STEP = 10
    private const val MINOR_TICK_STEP = 2
    private const val MINOR_TICK_LENGTH = 8
    private const val TEXT_GAP = 2
    private const val ARC = 4.0
    private val MAJOR_TICK_COLOR = Color(0xB4_B4_B9)
    private val MINOR_TICK_COLOR = Color(0xD2_D2_D7)
    private val TRACK_COLOR = Color(0xE6_E6_EB)
    private val RANGE_COLOR = Color(0x78_00_B4_FF, true)
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
