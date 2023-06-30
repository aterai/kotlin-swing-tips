package example

import java.awt.*
import java.awt.geom.Point2D
import java.awt.image.BufferedImage
import java.awt.image.PixelGrabber
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

fun makeUI() = JPanel(BorderLayout()).also {
  val p = JPanel(GridLayout(3, 1))
  p.add(makeProgressBar())
  p.add(makeProgressBar())
  p.add(makeProgressBar())
  it.add(p, BorderLayout.NORTH)
  it.preferredSize = Dimension(320, 240)
}

private fun makeProgressBar(): Component {
  val progressBar = JProgressBar()
  progressBar.isOpaque = false
  progressBar.ui = GradientPalletProgressBarUI()

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

class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
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

class GradientPalletProgressBarUI : BasicProgressBarUI() {
  private val pallet: IntArray

  init {
    this.pallet = makeGradientPallet()
  }

  private fun makeGradientPallet(): IntArray {
    val image = BufferedImage(100, 1, BufferedImage.TYPE_INT_RGB)
    val g2 = image.createGraphics()
    val start = Point2D.Float()
    val end = Point2D.Float(99f, 0f)
    val dist = floatArrayOf(0f, .5f, 1f)
    val colors = arrayOf(Color.RED, Color.YELLOW, Color.GREEN)
    g2.paint = LinearGradientPaint(start, end, dist, colors)
    g2.fillRect(0, 0, 100, 1)
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

  private fun getColorFromPallet(pallet: IntArray, x: Float): Color {
    require(x in 0f..1f) { "Parameter outside of expected range" }
    val max = pallet.size - 1
    val index = (pallet.size * x).toInt().coerceIn(0, max)
    return Color(pallet[index] and 0x00_FF_FF_FF)
    // translucent
    // val pix = pallet[index] & 0x00_FF_FF_FF | (0x64 << 24)
    // return Color(pix, true)
  }

  override fun paintDeterminate(g: Graphics, c: JComponent) {
    val b = progressBar.insets // area for border
    val barRectWidth = progressBar.width - b.right - b.left
    val barRectHeight = progressBar.height - b.top - b.bottom
    if (barRectWidth <= 0 || barRectHeight <= 0) {
      return
    }
    // val cellLength = getCellLength()
    // val cellSpacing = getCellSpacing()
    // amount of progress to draw
    val amountFull = getAmountFull(b, barRectWidth, barRectHeight)

    // draw the cells
    when (progressBar.orientation) {
      SwingConstants.HORIZONTAL -> {
        val x = amountFull / barRectWidth.toFloat()
        g.color = getColorFromPallet(pallet, x)
        g.fillRect(b.left, b.top, amountFull, barRectHeight)
      }
      SwingConstants.VERTICAL -> {
        val y = amountFull / barRectHeight.toFloat()
        g.color = getColorFromPallet(pallet, y)
        g.fillRect(b.left, barRectHeight + b.bottom - amountFull, barRectWidth, amountFull)
      }
    }
    // Deal with possible text painting
    if (progressBar.isStringPainted) {
      paintString(g, b.left, b.top, barRectWidth, barRectHeight, amountFull, b)
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
