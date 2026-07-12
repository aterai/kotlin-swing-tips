package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.plaf.basic.BasicProgressBarUI
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

fun createUI(): Component {
  val progress1 = object : JProgressBar() {
    override fun updateUI() {
      super.updateUI()
      setUI(ProgressCircleUI())
      putClientProperty("Slider.clockwise", true)
    }

    override fun getString(): String? {
      val progressUI = getUI()
      return if (progressUI is AbstractEaseOutProgressBarUI) {
        (progressUI.animatedFraction * 100.0).roundToInt().toString() + "%"
      } else {
        super.getString()
      }
    }
  }
  progress1.setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25))
  progress1.setFont(progress1.getFont().deriveFont(24f))
  progress1.setStringPainted(true)

  val progress2 = object : JProgressBar() {
    override fun updateUI() {
      super.updateUI()
      setUI(EaseOutProgressBarUI())
    }
  }

  val slider = JSlider(JSlider.HORIZONTAL, 0, 100, 0)
  slider.model.also {
    progress1.setModel(it)
    progress2.setModel(it)
  }

  val p = JPanel()
  p.add(progress1)

  return JPanel(BorderLayout()).also {
    it.add(slider, BorderLayout.NORTH)
    it.add(p)
    it.add(progress2, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private abstract class AbstractEaseOutProgressBarUI : BasicProgressBarUI() {
  var animatedFraction = 0.0
    private set
  private var animator: SwingWorker<Void, Double>? = null
  private var changeHandler: ChangeListener? = null

  override fun installUI(c: JComponent) {
    super.installUI(c)
    animatedFraction = progressBar.getPercentComplete()
  }

  override fun installListeners() {
    super.installListeners()
    changeHandler = ChangeListener {
      startAnimation(progressBar.getPercentComplete())
    }
    progressBar.addChangeListener(changeHandler)
  }

  override fun uninstallListeners() {
    progressBar.removeChangeListener(changeHandler)
    cancelAnimation()
    super.uninstallListeners()
  }

  private fun cancelAnimation() {
    animator
      ?.takeUnless { it.isDone }
      ?.also { it.cancel(true) }
  }

  private fun startAnimation(target: Double) {
    cancelAnimation()
    val from = animatedFraction
    if (abs(target - from) < EPSILON) {
      return
    }
    animator = AnimationWorker(from, target).also {
      it.execute()
    }
  }

  private inner class AnimationWorker(
    private val from: Double,
    private val target: Double,
  ) : SwingWorker<Void, Double>() {
    @Throws(InterruptedException::class)
    override fun doInBackground(): Void? {
      val startTime = System.currentTimeMillis()
      while (!isCancelled) {
        val time = System.currentTimeMillis() - startTime
        val t = min(COMPLETE, time / DURATION_MS.toDouble())
        publish(from + (target - from) * easeOutCubic(t))
        if (t >= COMPLETE) {
          break
        }
        sleep()
      }
      return null
    }

    @Throws(InterruptedException::class)
    fun sleep() {
      Thread.sleep(FRAME_INTERVAL_MS.toLong())
    }

    override fun process(chunks: MutableList<Double>) {
      // It is enough to reflect only the latest value
      animatedFraction = chunks[chunks.size - 1]
      progressBar.repaint()
    }

    override fun done() {
      if (!isCancelled) {
        animatedFraction = target
        progressBar.repaint()
      }
    }
  }

  companion object {
    protected const val COMPLETE: Double = 1.0
    private const val DURATION_MS = 400
    private const val FRAME_INTERVAL_MS = 1000 / 60
    private const val EPSILON = 1e-6

    private fun easeOutCubic(t: Double): Double {
      val u = COMPLETE - t
      return COMPLETE - u * u * u
    }
  }
}

private class ProgressCircleUI : AbstractEaseOutProgressBarUI() {
  override fun getPreferredSize(c: JComponent?): Dimension {
    val d = super.getPreferredSize(c)
    val v = max(d.width, d.height)
    d.setSize(v, v)
    return d
  }

  override fun paint(g: Graphics, c: JComponent?) {
    val rect = SwingUtilities.calculateInnerArea(progressBar, null)
    if (!rect.isEmpty) {
      // Draw the track and the circular sector
      paintProgressCircle(g, rect)

      // Deal with possible text painting
      if (progressBar.isStringPainted) {
        val ins = progressBar.getInsets()
        paintString(g, rect.x, rect.y, rect.width, rect.height, 0, ins)
      }
    }
  }

  private fun paintProgressCircle(g: Graphics, rect: Rectangle) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )

    val o = progressBar.getClientProperty("Slider.clockwise")
    val dir = if (o == true) -1 else 1
    val sz = min(rect.width, rect.height).toDouble()
    val cx = rect.centerX
    val cy = rect.centerY
    val or = sz * .5
    val ir = or * .5 // .8;
    val start = 90.0

    // Draw using apparent proportions during animation instead of actual values
    val degree = dir * 360.0 * this.animatedFraction
    val inner = Ellipse2D.Double(cx - ir, cy - ir, ir * 2.0, ir * 2.0)
    val outer = Ellipse2D.Double(cx - or, cy - or, sz, sz)
    val sector = Arc2D.Double(cx - or, cy - or, sz, sz, start, degree, Arc2D.PIE)

    val foreground = Area(sector)
    val background = Area(outer)
    val hole = Area(inner)

    foreground.subtract(hole)
    background.subtract(hole)

    // Draw the track
    g2.paint = Color(0xDD_DD_DD)
    g2.fill(background)

    g2.paint = progressBar.getForeground()
    g2.fill(foreground)
    g2.dispose()
  }
}

private class EaseOutProgressBarUI : AbstractEaseOutProgressBarUI() {
  override fun getAmountFull(b: Insets?, width: Int, height: Int): Int {
    var amountFull = 0
    val model = progressBar.getModel()
    if ((model.maximum - model.minimum) != 0) {
      val percentComplete = this.animatedFraction
      amountFull = if (progressBar.getOrientation() == JProgressBar.HORIZONTAL) {
        (width * percentComplete).roundToInt()
      } else {
        (height * percentComplete).roundToInt()
      }
    }
    return amountFull
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
