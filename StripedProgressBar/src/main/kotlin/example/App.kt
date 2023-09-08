package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

fun makeUI(): Component {
  UIManager.put("ProgressBar.cycleTime", 1000)
  UIManager.put("ProgressBar.repaintInterval", 10)

  val model = DefaultBoundedRangeModel()

  val progress1 = JProgressBar(model)
  progress1.ui = StripedProgressBarUI(dir = true, slope = true)

  val progress2 = JProgressBar(model)
  progress2.ui = StripedProgressBarUI(dir = true, slope = false)

  val progress3 = JProgressBar(model)
  progress3.ui = StripedProgressBarUI(dir = false, slope = true)

  val progress4 = JProgressBar(model)
  progress4.ui = StripedProgressBarUI(dir = false, slope = false)

  val list = listOf(
    JProgressBar(model),
    progress1,
    progress2,
    progress3,
    progress4,
  )

  val p = JPanel(GridLayout(5, 1))
  list.forEach {
    p.add(makePanel(it))
  }

  var worker: SwingWorker<String, Void>? = null
  val button = JButton("Test start")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = BackgroundTask().also { w ->
      list.forEach { bar ->
        bar.isIndeterminate = true
        w.addPropertyChangeListener(ProgressListener(bar))
      }
      w.execute()
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))

  val panel = JPanel(BorderLayout())
  panel.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable && worker != null) {
      worker?.cancel(true)
      worker = null
    }
  }
  panel.add(p)
  panel.add(box, BorderLayout.SOUTH)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  panel.preferredSize = Dimension(320, 240)
  return panel
}

private fun makePanel(cmp: Component): Component {
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  val p = JPanel(GridBagLayout())
  p.add(cmp, c)
  return p
}

private class StripedProgressBarUI(
  private val dir: Boolean,
  private val slope: Boolean
) : BasicProgressBarUI() {
  override fun getBoxLength(availableLength: Int, otherDimension: Int) = availableLength
  // (availableLength / 6.0).roundToInt()

  public override fun paintIndeterminate(
    g: Graphics,
    c: JComponent
  ) {
    val barRect = SwingUtilities.calculateInnerArea(progressBar, null)
    if (barRect.isEmpty) {
      return
    }
    // Paint the striped box.
    boxRect = getBox(boxRect)
    if (boxRect != null) {
      val w = 10
      val x = animationIndex
      val s = makeIndeterminateBox(w)
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = progressBar.foreground
      if (slope) {
        var i = boxRect.width + x
        while (i > -w) {
          val at = AffineTransform.getTranslateInstance(i.toDouble(), 0.0)
          g2.fill(at.createTransformedShape(s))
          i -= w
        }
      } else {
        var i = -x
        while (i < boxRect.width) {
          val at = AffineTransform.getTranslateInstance(i.toDouble(), 0.0)
          g2.fill(at.createTransformedShape(s))
          i += w
        }
      }
      g2.dispose()
    }
  }

  fun makeIndeterminateBox(w: Int): Shape {
    val p = GeneralPath()
    if (dir) {
      p.moveTo(boxRect.x.toFloat(), boxRect.y.toFloat())
      p.lineTo((boxRect.x + w * .5f).toDouble(), boxRect.maxY)
      p.lineTo((boxRect.x + w.toFloat()).toDouble(), boxRect.maxY)
    } else {
      p.moveTo(boxRect.x.toDouble(), boxRect.maxY)
      p.lineTo((boxRect.x + w * .5f).toDouble(), boxRect.maxY)
      p.lineTo(boxRect.x + w.toFloat(), boxRect.y.toFloat())
    }
    p.lineTo(boxRect.x + w * .5f, boxRect.y.toFloat())
    p.closePath()
    return p
  }
}

private class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    Thread.sleep(5000)
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(50)
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.propertyName
    if ("progress" == strPropertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
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
