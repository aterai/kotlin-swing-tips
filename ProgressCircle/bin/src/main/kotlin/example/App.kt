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

class MainPanel : JPanel(BorderLayout()) {
  protected val progress1: JProgressBar = object : JProgressBar() {
    override fun updateUI() {
      super.updateUI()
      setUI(ProgressCircleUI())
      setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25))
    }
  }
  protected val progress2: JProgressBar = object : JProgressBar() {
    override fun updateUI() {
      super.updateUI()
      setUI(ProgressCircleUI())
      setBorder(BorderFactory.createEmptyBorder(25, 25, 25, 25))
    }
  }

  init {
    progress1.setForeground(Color(0xFF_AA_AA))
    progress2.setStringPainted(true)
    progress2.setFont(progress2.getFont().deriveFont(24f))

    val slider = JSlider()
    slider.putClientProperty("Slider.paintThumbArrowShape", true)
    progress1.setModel(slider.getModel())

    val button = JButton("start")
    button.addActionListener { e ->
      val b = e.getSource() as JButton
      b.setEnabled(false)
      val worker = object : BackgroundTask() {
        override fun done() {
          if (b.isDisplayable()) {
            b.setEnabled(true)
          }
        }
      }
      worker.addPropertyChangeListener(ProgressListener(progress2))
      worker.execute()
    }

    add(slider, BorderLayout.NORTH)
    add(JPanel(GridLayout(1, 2)).also {
      it.add(progress1)
      it.add(progress2)
    })
    add(button, BorderLayout.SOUTH)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ProgressCircleUI : BasicProgressBarUI() {
  override fun getPreferredSize(c: JComponent) = super.getPreferredSize(c).also {
    val v = Math.max(it.width, it.height)
    it.setSize(v, v)
  }

  override fun paint(g: Graphics, c: JComponent) {
    val b = progressBar.getInsets() // area for border
    val barRectWidth = progressBar.getWidth() - b.right - b.left
    val barRectHeight = progressBar.getHeight() - b.top - b.bottom
    // if (barRectWidth <= 0 || barRectHeight <= 0) {
    //   return
    // }

    val g2 = g as Graphics2D
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

    val degree = 360 * progressBar.getPercentComplete()
    val sz = Math.min(barRectWidth, barRectHeight).toDouble()
    val cp = Point2D.Double(b.left + barRectWidth * .5, b.top + barRectHeight * .5)
    val r = sz * .5

    val track = Area(Ellipse2D.Double(cp.x - r, cp.y - r, sz, sz))
    val sector = Area(Arc2D.Double(cp.x - r, cp.y - r, sz, sz, 90 - degree, degree, Arc2D.PIE))
    val hole = Area(Ellipse2D.Double(cp.x - r * .5, cp.y - r * .5, r, r))

    track.subtract(hole)
    sector.subtract(hole)

    // draw the track
    g2.setPaint(Color(0xDD_DD_DD))
    g2.fill(track)

    // draw the circular sector
    // AffineTransform at = AffineTransform.getScaleInstance(-1.0, 1.0);
    // at.translate(-(barRectWidth + b.left * 2), 0);
    // AffineTransform at = AffineTransform.getRotateInstance(Math.toRadians(degree), cp.x, cp.y);
    // g2.fill(at.createTransformedShape(area));
    g2.setPaint(progressBar.getForeground())
    g2.fill(sector)
    // g2.dispose()

    // Deal with possible text painting
    if (progressBar.isStringPainted()) {
      paintString(g, b.left, b.top, barRectWidth, barRectHeight, 0, b)
    }
  }
}

open class BackgroundTask : SwingWorker<String, Void>() {
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled()) {
      try { // dummy task
        Thread.sleep(80)
      } catch (ex: InterruptedException) {
        return "Interrupted"
      }

      setProgress(100 * current / lengthOfTask)
      current++
    }
    return "Done"
  }
}

internal class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    this.progressBar.setValue(0)
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.getPropertyName()
    if ("progress" == strPropertyName) {
      progressBar.setIndeterminate(false)
      val progress = e.getNewValue() as Int
      progressBar.setValue(progress)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
