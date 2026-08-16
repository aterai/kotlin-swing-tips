package example

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.geom.Ellipse2D
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.plaf.basic.BasicSliderUI
import kotlin.math.min
import kotlin.math.roundToInt

private const val THUMB_SIZE = 20
private const val THUMB_MARGIN = 2
private const val TRACK_WIDTH = THUMB_SIZE * 2
private const val ON_TEXT = "?"
private const val OFF_TEXT = ""
private val LABELS = SwitchLabels(ON_TEXT, OFF_TEXT)

fun createUI(): Component {
  UIManager.put("ToggleSwitchSlider.onColor", Color(0x00_64_E4))
  UIManager.put("ToggleSwitchSlider.offColor", Color(0x80_80_80))
  UIManager.put("ToggleSwitchSlider.disabledColor", Color(0xB4_B4_B4))
  UIManager.put("ToggleSwitchSlider.borderColor", Color(0x64_64_64))
  UIManager.put("ToggleSwitchSlider.thumbColor", Color.WHITE)
  UIManager.put("ToggleSwitchSlider.disabledThumbColor", Color(0xEE_EE_EE))

  val disabled = createToggleSwitch(1)
  disabled.setEnabled(false)
  return JPanel(GridLayout(2, 2)).also {
    it.add(createTitledPanel("Default", JSlider(0, 1, 0)))
    it.add(createTitledPanel("ToggleSwitch: Off", createToggleSwitch(0)))
    it.add(createTitledPanel("ToggleSwitch: On", createToggleSwitch(1)))
    it.add(createTitledPanel("setEnabled(false)", disabled))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createToggleSwitch(value: Int): JSlider {
  val slider: JSlider = object : JSlider(0, 1, value) {
    override fun updateUI() {
      super.updateUI()
      setUI(SliderToggleSwitchUI(this))
      setOpaque(false)
    }
  }
  // Font is not a UIResource, so it is kept after a LookAndFeel change
  slider.setFont(slider.getFont().deriveFont(Font.BOLD, 14f))
  return slider
}

private fun createTitledPanel(title: String?, c: Component?): Component {
  val p = JPanel(GridBagLayout())
  p.setBorder(BorderFactory.createTitledBorder(title))
  p.add(c)
  return p
}

private class SliderToggleSwitchUI(
  slider: JSlider,
) : BasicSliderUI(slider) {
  // The field BasicSliderUI#slider is not assigned until installUI(...)
  private val animator = ThumbAnimator(slider)

  override fun uninstallUI(c: JComponent?) {
    animator.stop()
    super.uninstallUI(c)
  }

  override fun installDefaults(slider: JSlider?) {
    super.installDefaults(slider)
    // The track of a toggle switch fills the whole component
    focusInsets = Insets(0, 0, 0, 0)
  }

  override fun getPreferredHorizontalSize() = Dimension(
    TRACK_WIDTH,
    THUMB_SIZE,
  )

  override fun getMinimumHorizontalSize() = Dimension(
    THUMB_SIZE * 2,
    THUMB_SIZE,
  )

  override fun getThumbSize() = Dimension(THUMB_SIZE, THUMB_SIZE)

  override fun setThumbLocation(x: Int, y: Int) {
    super.setThumbLocation(x, y)
    // While dragging, the thumb follows the mouse pointer without any animation
    animator.jumpTo(x.toDouble())
    // The track color and the On/Off label depend on the thumb location,
    // so the partial repaint of the super method is not enough
    slider.repaint()
  }

  override fun calculateThumbLocation() {
    super.calculateThumbLocation()
    // BasicSliderUI#ChangeHandler skips this method while the thumb is dragged,
    // so a value change by a click, a keystroke or setValue(...) is animated here
    if (animator.isInitialized && slider.isShowing()) {
      animator.startTo(thumbRect.x.toDouble())
    } else {
      // The very first layout and a layout before the switch is shown must not animate
      animator.jumpTo(thumbRect.x.toDouble())
    }
  }

  override fun paintFocus(g: Graphics?) {
    // The focus is painted as a part of the track border in paintTrack(...)
  }

  override fun paintTrack(g: Graphics) {
    // trackRect is inset by trackBuffer(= thumbWidth / 2) on both sides
    val r = Rectangle(trackRect)
    r.grow(thumbRect.width / 2, 0)
    // A stroke is centered on the shape outline, so the track must be inset by
    // half of the line width. Otherwise, the outer half of the border sticks out
    // of the component and its edge pixels are left unpainted
    val lw = 1f // slider.hasFocus() ? 2f : 1f;
    val half = lw / 2.0
    val track = RoundRectangle2D.Double(
      r.x + half,
      r.y + half,
      (r.width - lw).toDouble(),
      (r.height - lw).toDouble(),
      r.height.toDouble(),
      r.height.toDouble(),
    )
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.setPaint(this.trackColor)
    g2.fill(track)
    g2.paint = UIManager.getColor("ToggleSwitchSlider.borderColor")
    g2.stroke = BasicStroke(lw)
    g2.draw(track)
    g2.clip(track)
    // thumbRect includes THUMB_MARGIN, so shrink it to the visible thumb size
    val left = animator.x + THUMB_MARGIN
    val right = animator.x + thumbRect.width - THUMB_MARGIN
    LABELS.paint(g2, r, left, right, this.thumbFraction)
    g2.dispose()
  }

  override fun paintThumb(g: Graphics) {
    val d = min(thumbRect.width, thumbRect.height) - THUMB_MARGIN * 2.0 - 1.0
    val thumb = Ellipse2D.Double(
      animator.x + THUMB_MARGIN + .5,
      thumbRect.y + THUMB_MARGIN + .5,
      d,
      d,
    )
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val thumbKey = if (slider.isEnabled) {
      "ToggleSwitchSlider.thumbColor"
    } else {
      "ToggleSwitchSlider.disabledThumbColor"
    }
    g2.paint = UIManager.getColor(thumbKey)
    g2.fill(thumb)
    g2.paint = UIManager.getColor("ToggleSwitchSlider.borderColor")
    g2.draw(thumb)
    g2.dispose()
  }

  override fun createTrackListener(
    slider: JSlider,
  ): TrackListener = ToggleTrackListener()

  // Extracted from an anonymous class to keep it under Checkstyle's AnonInnerLength limit
  private inner class ToggleTrackListener : TrackListener() {
    private var thumbPressed = false
    private var pressedValue = 0

    override fun mousePressed(e: MouseEvent) {
      if (!slider.isEnabled || !SwingUtilities.isLeftMouseButton(e)) {
        return
      }
      thumbPressed = thumbRect.contains(e.getX(), e.getY())
      if (thumbPressed) {
        pressedValue = slider.value
        super.mousePressed(e) // Start dragging the thumb
      } else {
        // Pressing the track toggles the value
        // instead of BasicSliderUI#scrollDueToClickInTrack(...)
        if (slider.isRequestFocusEnabled) {
          slider.requestFocus()
        }
        toggle()
      }
    }

    override fun mouseReleased(e: MouseEvent?) {
      super.mouseReleased(e)
      if (thumbPressed && pressedValue == slider.value) {
        toggle() // Clicking the thumb without dragging also toggles the value
      }
      thumbPressed = false
    }

    fun toggle() {
      val min = slider.minimum
      slider.value = if (slider.value == min) slider.maximum else min
    }
  }

  private val thumbFraction: Double
    // The ratio of the current thumb position to the whole travel: 0d = Off, 1d = On
    get() {
      val half = thumbRect.width / 2
      val minX = xPositionForValue(slider.minimum) - half
      val maxX = xPositionForValue(slider.maximum) - half
      val f = if (minX == maxX) 0.0 else (animator.x - minX) / (maxX - minX)
      return f.coerceIn(0.0, 1.0)
    }

  private val trackColor: Color?
    get() {
      val color: Color?
      if (slider.isEnabled) {
        // The track color is blended so that it changes while the thumb is moving
        val c0 = UIManager.getColor("ToggleSwitchSlider.offColor")
        val c1 = UIManager.getColor("ToggleSwitchSlider.onColor")
        val t = this.thumbFraction
        val u = 1.0 - t
        color = Color(
          (c0.red * u + c1.red * t).roundToInt(),
          (c0.green * u + c1.green * t).roundToInt(),
          (c0.blue * u + c1.blue * t).roundToInt(),
        )
      } else {
        color = UIManager.getColor("ToggleSwitchSlider.disabledColor")
      }
      return color
    }
}

// Paints the On/Off labels in the part of the track that is not covered by the thumb
private class SwitchLabels(
  private val onText: String,
  private val offText: String,
) {
  // The fraction is 0d when the switch is Off and 1d when it is On,
  // so the two labels cross-fade while the thumb is moving
  fun paint(
    g2: Graphics2D,
    track: Rectangle,
    thumbLeft: Double,
    thumbRight: Double,
    fraction: Double,
  ) {
    val y = track.y.toDouble()
    val h = track.height.toDouble()
    paintLabel(
      g2,
      onText,
      Rectangle2D.Double(track.x.toDouble(), y, thumbLeft - track.x, h),
      fraction,
    )
    paintLabel(
      g2,
      offText,
      Rectangle2D.Double(thumbRight, y, track.maxX - thumbRight, h),
      1.0 - fraction,
    )
  }
}

private fun paintLabel(
  g2: Graphics2D,
  txt: String,
  free: Rectangle2D,
  alpha: Double,
) {
  if (txt.isNotEmpty() && alpha > 0.0) {
    val fm = g2.fontMetrics
    val tx = free.x + (free.width - fm.stringWidth(txt)) / 2.0
    val ty = free.y + (free.height - fm.height) / 2.0 + fm.ascent
    g2.paint = Color(1f, 1f, 1f, alpha.toFloat())
    g2.drawString(txt, tx.toFloat(), ty.toFloat())
  }
}

// Slides the painted thumb position towards its target with an ease-out curve
private class ThumbAnimator(
  private val view: JComponent,
) {
  private val timer: Timer

  // The thumb position being painted, which lags behind the model position
  var x: Double = Double.NaN
    private set
  private var fromX = 0.0
  private var toX = 0.0
  private var startTime: Long = 0

  init {
    this.timer = Timer(DELAY) { update() }
  }

  val isInitialized: Boolean
    get() = !java.lang.Double.isNaN(this.x)

  val targetX: Double
    // The position the thumb is heading for, or the current one if it is not moving
    get() = if (timer.isRunning) toX else this.x

  // Move to the target immediately, cancelling any animation in progress
  fun jumpTo(target: Double) {
    timer.stop()
    this.x = target
  }

  fun startTo(target: Double) {
    if (this.targetX.compareTo(target) != 0) {
      fromX = this.x
      toX = target
      startTime = System.currentTimeMillis()
      timer.restart()
    }
  }

  fun stop() {
    timer.stop()
  }

  private fun update() {
    val elapsed = System.currentTimeMillis() - startTime
    if (elapsed < DURATION) {
      this.x = fromX + (toX - fromX) * easeOutCubic(elapsed / DURATION.toDouble())
    } else {
      this.x = toX
      timer.stop()
    }
    view.repaint()
  }

  private fun easeOutCubic(t: Double): Double {
    val u = 1.0 - t
    return 1.0 - u * u * u
  }

  companion object {
    private const val DELAY = 10
    private const val DURATION = 120
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
