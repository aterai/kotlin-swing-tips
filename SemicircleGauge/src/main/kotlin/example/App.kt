package example

import java.awt.*
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

fun makeUI(): Component {
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
    val worker = object : SwingWorker<String, Void>() {
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

private class SolidGaugeUI(range: Int, extent: Double) : BasicProgressBarUI() {
  private val pallet: IntArray
  private val extent: Double

  init {
    pallet = makeGradientPallet(range)
    this.extent = extent
  }

  override fun paint(g: Graphics, c: JComponent) {
    val rect = SwingUtilities.calculateInnerArea(progressBar, null)
    if (rect.isEmpty) {
      return
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    // val extent = -150d
    val start = 90.0 + extent * .5
    val degree = extent * progressBar.percentComplete
    val or = rect.width.coerceAtMost(rect.height).toDouble()
    val cx = rect.centerX
    val cy = rect.maxY
    val sz = or * 2.0
    val ir = or * .6
    val inner = Arc2D.Double(cx - ir, cy - ir, ir * 2.0, ir * 2.0, start, -extent, Arc2D.PIE)
    val outer = Arc2D.Double(cx - or, cy - or, sz, sz, start, -extent, Arc2D.PIE)
    val sector = Arc2D.Double(cx - or, cy - or, sz, sz, start, -degree, Arc2D.PIE)
    val foreground = Area(sector)
    val background = Area(outer)
    val hole = Area(inner)
    foreground.subtract(hole)
    background.subtract(hole)
    // Draw the track
    g2.paint = Color(0xDD_DD_DD)
    g2.fill(background)
    // Draw the circular sector
    g2.paint = getColorFromPallet(pallet, progressBar.percentComplete)
    g2.fill(foreground)
    // Draw minimum, maximum
    val font = progressBar.font
    val fsz = font.size2D
    val min = (cx - or - fsz).toFloat()
    val max = (cx + or + 4.0).toFloat()
    g2.paint = progressBar.foreground
    g2.drawString(progressBar.minimum.toString(), min, cy.toFloat())
    g2.drawString(progressBar.maximum.toString(), max, cy.toFloat())
    // Deal with possible text painting
    if (progressBar.isStringPainted) {
      val h = cy.toFloat() - fsz
      val str = progressBar.value.toString()
      val vx = cx.toFloat() - g2.fontMetrics.stringWidth(str) * .5f
      g2.drawString(str, vx, h)
      val ksz = fsz * 2f / 3f
      g2.font = font.deriveFont(ksz)
      val kmh = "㎞/h"
      val tx = cx.toFloat() - g2.fontMetrics.stringWidth(kmh) * .5f
      g2.drawString(kmh, tx, h + ksz)
    }
    g2.dispose()
  }

  private fun makeGradientPallet(range: Int): IntArray {
    val image = BufferedImage(range, 1, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    val start = Point2D.Float()
    val end = Point2D.Float(range - 1f, 0f)
    val dist = floatArrayOf(0f, .8f, .9f, 1f)
    val colors = arrayOf(Color.GREEN, Color.YELLOW, Color.ORANGE, Color.RED)
    g2.paint = LinearGradientPaint(start, end, dist, colors)
    g2.fillRect(0, 0, range, 1)
    g2.dispose()

    val width = image.getWidth(null)
    val pallet = IntArray(width)
    runCatching {
      PixelGrabber(image, 0, 0, width, 1, pallet, 0, width).grabPixels()
    }.onFailure {
      it.printStackTrace()
      Toolkit.getDefaultToolkit().beep()
    }
    return pallet
  }

  private fun getColorFromPallet(pallet: IntArray, pos: Double): Color {
    require(!(pos < 0.0 || pos > 1.0)) { "Parameter outside of expected range" }
    val i = (pallet.size * pos).toInt()
    val max = pallet.size - 1
    val index = i.coerceAtLeast(0).coerceAtMost(max)
    return Color(pallet[index] and 0x00_FF_FF_FF)
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
