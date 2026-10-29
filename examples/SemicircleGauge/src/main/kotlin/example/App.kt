package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

fun createUI(): Component {
  val progress1 = object : JProgressBar(0, 200) {
    override fun updateUI() {
      super.updateUI()
      setUI(SolidGaugeUI(maximum - minimum, 180.0))
    }
  }
  val progress2 = object : JProgressBar(0, 200) {
    override fun updateUI() {
      super.updateUI()
      setUI(SolidGaugeUI(maximum - minimum, 160.0))
    }
  }
  listOf(progress1, progress2).forEach {
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.font = it.font.deriveFont(18f)
    it.isStringPainted = true
  }
  val slider = JSlider(0, 200, 0)
  slider.putClientProperty("Slider.paintThumbArrowShape", true)
  progress1.model = slider.model

  val button = JButton("start")
  button.addActionListener {
    button.isEnabled = false
    val lengthOfTask = progress2.maximum - progress2.minimum
    val worker = object : SwingWorker<String, Unit?>() {
      @Throws(InterruptedException::class)
      override fun doInBackground(): String {
        var current = 0
        while (current <= lengthOfTask && !isCancelled) {
          Thread.sleep(10)
          progress = 100 * current / lengthOfTask
          current++
        }
        return "Done"
      }

      override fun done() {
        if (button.isDisplayable) {
          button.isEnabled = true
        }
      }
    }
    worker.addPropertyChangeListener(ProgressListener(progress2))
    worker.execute()
  }
  val p = JPanel(GridLayout(2, 1))
  p.add(progress1)
  p.add(progress2)

  return JPanel(BorderLayout()).also {
    it.add(slider, BorderLayout.NORTH)
    it.add(p)
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class SolidGaugeUI(
  range: Int,
  private val extent: Double,
) : BasicProgressBarUI() {
  private val palette = createGradientPalette(range)

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    val rect = SwingUtilities.calculateInnerArea(progressBar, null)
    if (rect.isEmpty) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    // val extent = -150d
    val outerRadius = rect.width.coerceAtMost(rect.height).toDouble()
    val innerRadius = outerRadius * .6
    val cx = rect.centerX
    val cy = rect.maxY
    // Arc2D angles are counterclockwise from 3 o'clock,
    // so the gauge starts at the upper left and sweeps clockwise (negative extent)
    val start = 90.0 + extent * .5
    val percent = progressBar.percentComplete
    val degree = extent * percent
    val background = createArc(cx, cy, outerRadius, start, -extent)
    val foreground = createArc(cx, cy, outerRadius, start, -degree)
    val hole = createArc(cx, cy, innerRadius, start, -extent)
    background.subtract(hole)
    foreground.subtract(hole)
    // Draw the track
    g2.paint = Color(0xDD_DD_DD)
    g2.fill(background)
    // Draw the circular sector
    g2.paint = getColorFromPalette(palette, percent)
    g2.fill(foreground)
    // Draw minimum, maximum
    g2.paint = progressBar.foreground
    drawMinMaxString(g2, cx, cy, outerRadius)
    // Deal with possible text painting
    if (progressBar.isStringPainted) {
      drawProgressString(g2, cx.toFloat(), cy.toFloat())
    }
    g2.dispose()
  }

  private fun createArc(
    cx: Double,
    cy: Double,
    r: Double,
    start: Double,
    extent: Double,
  ): Area {
    val d = r * 2.0
    return Area(Arc2D.Double(cx - r, cy - r, d, d, start, extent, Arc2D.PIE))
  }

  private fun drawMinMaxString(
    g2: Graphics2D,
    cx: Double,
    cy: Double,
    r: Double,
  ) {
    val min = progressBar.minimum.toString()
    val max = progressBar.maximum.toString()
    val gap = 4.0
    // Right-align the minimum label to the left end of the gauge
    val minX = cx - r - gap - g2.fontMetrics.stringWidth(min)
    val maxX = cx + r + gap
    g2.drawString(min, minX.toFloat(), cy.toFloat())
    g2.drawString(max, maxX.toFloat(), cy.toFloat())
  }

  private fun drawProgressString(
    g2: Graphics2D,
    x: Float,
    y: Float,
  ) {
    val font = progressBar.font
    val fontSize = font.size2D
    val baseline = y - fontSize
    val value = progressBar.value.toString()
    val valueX = x - g2.fontMetrics.stringWidth(value) * .5f
    g2.drawString(value, valueX, baseline)
    // Draw the unit on the next line in a font 2/3 the size of the value
    val unitSize = fontSize * 2f / 3f
    g2.font = font.deriveFont(unitSize)
    val unit = "㎞/h"
    val unitX = x - g2.fontMetrics.stringWidth(unit) * .5f
    g2.drawString(unit, unitX, baseline + unitSize)
  }

  private fun createGradientPalette(range: Int): IntArray {
    val image = BufferedImage(range, 1, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    val start = Point2D.Float()
    val end = Point2D.Float(range - 1f, 0f)
    val dist = floatArrayOf(0f, .8f, .9f, 1f)
    val colors = arrayOf(Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED)
    g2.paint = LinearGradientPaint(start, end, dist, colors)
    g2.fillRect(0, 0, range, 1)
    g2.dispose()
    // BufferedImage#getRGB(...) reads the pixels directly,
    // so PixelGrabber and its InterruptedException handling are unnecessary
    return image.getRGB(0, 0, range, 1, null, 0, range)
  }

  private fun getColorFromPalette(
    palette: IntArray,
    pos: Double,
  ): Color {
    require(pos in 0.0..1.0) { "Parameter outside of expected range" }
    // pos == 1.0 gives palette.size, so clamp it to the last index
    val index = (palette.size * pos).toInt().coerceAtMost(palette.size - 1)
    // Color(Int) ignores the alpha bits of the TYPE_INT_RGB pixel
    return Color(palette[index])
  }
}

private class ProgressListener(
  private val progressBar: JProgressBar,
) : PropertyChangeListener {
  init {
    progressBar.value = progressBar.minimum
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.propertyName
    val nv = e.newValue
    if ("progress" == strPropertyName && nv is Int) {
      progressBar.isIndeterminate = false
      val range = progressBar.maximum - progressBar.minimum
      val iv = (range * .01 * nv).toInt()
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
