package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.floor
import kotlin.math.pow

private val progress1 = makeProgressBar()
private val progress2 = makeProgressBar()

fun createUI(): Component {
  progress1.foreground = Color(0xFF_AA_AA)
  progress2.isStringPainted = true
  progress2.font = progress2.font.deriveFont(24f)

  val slider = JSlider()
  slider.putClientProperty("Slider.paintThumbArrowShape", true)
  progress1.model = slider.model

  val key = "Slider.clockwise"
  progress1.putClientProperty(key, true)
  progress2.putClientProperty(key, true)
  val check = JCheckBox("Clockwise", true)
  check.addActionListener { e ->
    val b = (e.getSource() as? JCheckBox)?.isSelected == true
    for (bar in listOf(progress1, progress2)) {
      bar.putClientProperty(key, b)
      bar.repaint()
    }
  }

  val button = JButton("start")
  button.addActionListener { e ->
    (e.source as? JButton)?.also {
      it.isEnabled = false
      progress2.isIndeterminate = true
      val worker = object : BackgroundTask() {
        override fun done() {
          if (it.isDisplayable) {
            it.isEnabled = true
          }
        }
      }
      worker.addPropertyChangeListener(ProgressListener(progress2))
      worker.execute()
    }
  }

  val box = Box.createHorizontalBox()
  box.add(check)
  box.add(Box.createHorizontalGlue())
  box.add(button)

  return JPanel(BorderLayout()).also {
    it.add(slider, BorderLayout.NORTH)
    val pnl = JPanel(GridLayout(1, 2)).also { p ->
      p.add(progress1)
      p.add(progress2)
    }
    it.add(pnl)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeProgressBar() = object : JProgressBar() {
  override fun updateUI() {
    super.updateUI()
    setUI(ProgressCircleUI())
    border = BorderFactory.createEmptyBorder(25, 25, 25, 25)
  }
}

private class ProgressCircleUI : BasicProgressBarUI() {
  private val startTime = System.currentTimeMillis()

  override fun getPreferredSize(c: JComponent) = super.getPreferredSize(c)?.also {
    val v = maxOf(it.width, it.height)
    it.setSize(v, v)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    val rect = SwingUtilities.calculateInnerArea(progressBar, null)
    if (rect.isEmpty) {
      return
    }
    // Arc2D angles increase counterclockwise, so a negative extent draws clockwise
    val o = progressBar.getClientProperty("Slider.clockwise")
    val dir = if (o == true) -1 else 1
    if (progressBar.isIndeterminate) {
      // Draw the track and the stretching/contracting rotating arc
      val elapsed = System.currentTimeMillis() - startTime
      val arc = MaterialSpinner.computeArcAngles(elapsed)
      val start = START_ANGLE - dir * arc.startAngle
      val extent = -dir * arc.sweepAngle.toDouble()
      paintProgressCircle(g, rect, start, extent)
    } else {
      // Draw the track and the circular sector
      val extent = dir * 360.0 * progressBar.percentComplete
      paintProgressCircle(g, rect, START_ANGLE, extent)

      // Deal with possible text painting
      if (progressBar.isStringPainted) {
        val ins = progressBar.insets
        paintString(g, rect.x, rect.y, rect.width, rect.height, 0, ins)
      }
    }
  }

  // Repaint the whole circle on each animation frame instead of the bouncing box area
  override fun getBox(r: Rectangle?): Rectangle? = null

  private fun paintProgressCircle(
    g: Graphics,
    rect: Rectangle,
    start: Double,
    extent: Double,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    val size = minOf(rect.width, rect.height).toDouble()
    val cx = rect.centerX
    val cy = rect.centerY
    val outerRadius = size * .5
    val innerRadius = outerRadius * .5
    val ox = cx - outerRadius
    val oy = cy - outerRadius
    val innerSize = innerRadius * 2.0
    val track = Area(Ellipse2D.Double(ox, oy, size, size))
    val sector = Area(Arc2D.Double(ox, oy, size, size, start, extent, Arc2D.PIE))
    val hole = Area(
      Ellipse2D.Double(cx - innerRadius, cy - innerRadius, innerSize, innerSize),
    )
    track.subtract(hole)
    sector.subtract(hole)

    // draw the track
    g2.paint = Color(0xDD_DD_DD)
    g2.fill(track)

    // draw the circular sector
    g2.paint = progressBar.foreground
    g2.fill(sector)
    g2.dispose()
  }

  companion object {
    private const val START_ANGLE = 90.0 // 12 o'clock
  }
}

// A value object that only holds the starting angle
// and sweep angle of the arc for one frame.
private data class ArcAngles(
  val startAngle: Float,
  val sweepAngle: Float,
)

// Angle calculations for the Material style spinner (stretch/contract/rotate)
// https://ateraimemo.com/Swing/CircularSpinner.html
private object MaterialSpinner {
  private const val CYCLE_DURATION = 1332f
  private const val MAX_SWEEP = 270f
  private const val MIN_SWEEP = 15f

  // The returned angles rotate clockwise (negative values)
  fun computeArcAngles(elapsedMillis: Long): ArcAngles {
    val totalCycles = elapsedMillis / CYCLE_DURATION
    val cycleIndex = floor(totalCycles).toInt()
    val t = totalCycles - cycleIndex
    val sweepRange = MAX_SWEEP - MIN_SWEEP
    // Keep the angles small to avoid losing float precision over time
    val cycleOffset = cycleIndex * sweepRange % 360f

    val head: Float
    val tail: Float
    if (t < .5f) {
      head = cycleOffset + MIN_SWEEP + sweepRange * easeInOutCubic(t / .5f)
      tail = cycleOffset
    } else {
      head = cycleOffset + MAX_SWEEP
      tail = cycleOffset + sweepRange * easeInOutCubic((t - .5f) / .5f)
    }

    val baseSpin = -360f * (elapsedMillis % 3000L / 3000f)
    val startAngle = baseSpin - tail
    val sweepAngle = tail - head
    return ArcAngles(startAngle, sweepAngle)
  }

  private fun easeInOutCubic(x: Float) = if (x < .5f) {
    4f * x * x * x
  } else {
    1f - (-2f * x + 2f).pow(3f) / 2f
  }
}

private open class BackgroundTask : SwingWorker<String, Unit>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    // Keep the progress bar indeterminate while preparing
    Thread.sleep(3000)
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(80)
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }
}

private class ProgressListener(
  private val progressBar: JProgressBar,
) : PropertyChangeListener {
  init {
    this.progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val iv = e.newValue
    if ("progress" == e.propertyName && iv is Int) {
      progressBar.isIndeterminate = false
      progressBar.value = iv
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
    // Repaint the indeterminate spinner at about 60fps (default: 50ms)
    UIManager.put("ProgressBar.repaintInterval", 16)
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
