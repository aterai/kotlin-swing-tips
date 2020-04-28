package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.Arc2D
import java.awt.geom.Area
import java.awt.geom.Ellipse2D
import java.awt.geom.Point2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicProgressBarUI

private val progress1 = object : JProgressBar() {
  override fun updateUI() {
    super.updateUI()
    setUI(ProgressCircleUI())
    border = BorderFactory.createEmptyBorder(25, 25, 25, 25)
  }
}
private val progress2 = object : JProgressBar() {
  override fun updateUI() {
    super.updateUI()
    setUI(ProgressCircleUI())
    border = BorderFactory.createEmptyBorder(25, 25, 25, 25)
  }
}

fun makeUI(): Component {
  progress1.foreground = Color(0xFF_AA_AA)
  progress2.isStringPainted = true
  progress2.font = progress2.font.deriveFont(24f)

  val slider = JSlider()
  slider.putClientProperty("Slider.paintThumbArrowShape", true)
  progress1.model = slider.model

  val button = JButton("start")
  button.addActionListener { e ->
    (e.source as? JButton)?.also {
      it.isEnabled = false
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

  return JPanel(BorderLayout()).also {
    it.add(slider, BorderLayout.NORTH)
    it.add(JPanel(GridLayout(1, 2)).also { p ->
      p.add(progress1)
      p.add(progress2)
    })
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ProgressCircleUI : BasicProgressBarUI() {
  override fun getPreferredSize(c: JComponent) = super.getPreferredSize(c)?.also {
    val v = maxOf(it.width, it.height)
    it.setSize(v, v)
  }

  override fun paint(g: Graphics, c: JComponent) {
    val b = progressBar.insets // area for border
    val barRectWidth = progressBar.width - b.right - b.left
    val barRectHeight = progressBar.height - b.top - b.bottom
    // if (barRectWidth <= 0 || barRectHeight <= 0) {
    //   return
    // }

    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val degree = 360 * progressBar.percentComplete
    val sz = minOf(barRectWidth, barRectHeight).toDouble()
    val cp = Point2D.Double(b.left + barRectWidth * .5, b.top + barRectHeight * .5)
    val r = sz * .5

    val track = Area(Ellipse2D.Double(cp.x - r, cp.y - r, sz, sz))
    val sector = Area(Arc2D.Double(cp.x - r, cp.y - r, sz, sz, 90 - degree, degree, Arc2D.PIE))
    val hole = Area(Ellipse2D.Double(cp.x - r * .5, cp.y - r * .5, r, r))

    track.subtract(hole)
    sector.subtract(hole)

    // draw the track
    g2.paint = Color(0xDD_DD_DD)
    g2.fill(track)

    // draw the circular sector
    g2.paint = progressBar.foreground
    g2.fill(sector)
    g2.dispose()

    // Deal with possible text painting
    if (progressBar.isStringPainted) {
      paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b)
    }
  }
}

private open class BackgroundTask : SwingWorker<String, Unit>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(80) // dummy task
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
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
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
