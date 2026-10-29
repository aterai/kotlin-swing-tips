package example

import java.awt.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

fun createUI() = JPanel(BorderLayout()).also {
  val p = JPanel(GridLayout(3, 1))
  p.add(makeProgressBar())
  p.add(makeProgressBar())
  p.add(makeProgressBar())
  it.add(p, BorderLayout.NORTH)
  it.preferredSize = Dimension(320, 240)
}

private fun makeProgressBar(): Component {
  val progressBar = object : JProgressBar() {
    override fun updateUI() {
      super.updateUI()
      setUI(GradientPaletteProgressBarUI())
      isOpaque = false
    }
  }

  val button = JButton("Start")
  button.addActionListener {
    button.isEnabled = false
    val worker = object : BackgroundTask() {
      override fun done() {
        button.takeIf { it.isDisplayable }?.isEnabled = true
      }
    }
    worker.addPropertyChangeListener(ProgressListener(progressBar))
    worker.execute()
  }

  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createEmptyBorder(32, 8, 0, 8)
  val c = GridBagConstraints()

  c.insets = Insets(0, 0, 0, 4)
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(progressBar, c)

  c.weightx = 0.0
  p.add(button, c)
  return p
}

private open class BackgroundTask : SwingWorker<Unit, Unit>() {
  @Throws(InterruptedException::class)
  override fun doInBackground() {
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(50)
      progress = 100 * current / lengthOfTask
      current++
    }
  }
}

class ProgressListener(
  private val progressBar: JProgressBar,
) : PropertyChangeListener {
  init {
    this.progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val nv = e.newValue
    if ("progress" == e.propertyName && nv is Int) {
      progressBar.isIndeterminate = false
      progressBar.value = nv
    }
  }
}

class GradientPaletteProgressBarUI : BasicProgressBarUI() {
  private val palette = makeGradientPalette()

  private fun makeGradientPalette(): IntArray {
    val width = 100
    val image = BufferedImage(width, 1, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    val start = Point2D.Float()
    val end = Point2D.Float(width - 1f, 0f)
    val dist = floatArrayOf(0f, .5f, 1f)
    val colors = arrayOf(Color.RED, Color.YELLOW, Color.GREEN)
    g2.paint = LinearGradientPaint(start, end, dist, colors)
    g2.fillRect(0, 0, width, 1)
    g2.dispose()
    // BufferedImage#getRGB(...) reads the pixels directly,
    // so PixelGrabber and its InterruptedException handling are unnecessary
    return image.getRGB(0, 0, width, 1, null, 0, width)
  }

  private fun getColorFromPalette(
    palette: IntArray,
    pos: Float,
  ): Color {
    require(pos in 0f..1f) { "Parameter outside of expected range" }
    // pos == 1f gives palette.size, so clamp it to the last index
    val index = minOf((palette.size * pos).toInt(), palette.size - 1)
    // Color(Int) ignores the alpha bits of the TYPE_INT_RGB pixel
    return Color(palette[index])
    // translucent
    // val pix = palette[index] and 0x00_FF_FF_FF or (0x64 shl 24)
    // return Color(pix, true)
  }

  override fun paintDeterminate(
    g: Graphics,
    c: JComponent,
  ) {
    val r = SwingUtilities.calculateInnerArea(progressBar, null)
    if (r.isEmpty) {
      return
    }
    val b = progressBar.insets // area for border
    // amount of progress to draw
    val amountFull = getAmountFull(b, r.width, r.height)
    when (progressBar.orientation) {
      SwingConstants.HORIZONTAL -> {
        g.color = getColorFromPalette(palette, amountFull / r.width.toFloat())
        // fill from the right edge in a right-to-left orientation,
        // same as BasicProgressBarUI
        val ltr = progressBar.componentOrientation.isLeftToRight
        val x = if (ltr) r.x else r.x + r.width - amountFull
        g.fillRect(x, r.y, amountFull, r.height)
      }

      SwingConstants.VERTICAL -> {
        g.color = getColorFromPalette(palette, amountFull / r.height.toFloat())
        g.fillRect(r.x, r.y + r.height - amountFull, r.width, amountFull)
      }
    }
    // Deal with possible text painting
    if (progressBar.isStringPainted) {
      paintString(g, r.x, r.y, r.width, r.height, amountFull, b)
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
