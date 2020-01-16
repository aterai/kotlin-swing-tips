package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.geom.AffineTransform
import java.awt.geom.GeneralPath
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicProgressBarUI

class MainPanel : JPanel(BorderLayout()), HierarchyListener {
  @Transient
  private var worker: SwingWorker<String, Void>? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    val isDisplayableChanged = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (isDisplayableChanged && !e.component.isDisplayable && worker != null) {
      println("DISPOSE_ON_CLOSE")
      worker?.cancel(true)
      worker = null
    }
  }

  init {
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
      JProgressBar(model), progress1, progress2, progress3, progress4
    )

    val p = JPanel(GridLayout(5, 1))
    list.forEach {
      p.add(makePanel(it))
    }

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
    addHierarchyListener(this)
    add(p)
    add(box, BorderLayout.SOUTH)
    border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    preferredSize = Dimension(320, 240)
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
}

class StripedProgressBarUI(private val dir: Boolean, private val slope: Boolean) : BasicProgressBarUI() {
  override fun getBoxLength(availableLength: Int, otherDimension: Int) = availableLength
  // (availableLength / 6.0).roundToInt()

  public override fun paintIndeterminate(
    g: Graphics,
    c: JComponent
  ) {
    val b = progressBar.insets // area for border
    val barRectWidth = progressBar.width - b.right - b.left
    val barRectHeight = progressBar.height - b.top - b.bottom
    if (barRectWidth <= 0 || barRectHeight <= 0) {
      return
    }
    // Paint the striped box.
    boxRect = getBox(boxRect)
    if (boxRect != null) {
      val w = 10
      val x = animationIndex
      val p = GeneralPath()
      if (dir) {
        p.moveTo(boxRect.minX, boxRect.minY)
        p.lineTo(boxRect.minX + w * .5f, boxRect.maxY)
        p.lineTo(boxRect.minX + w.toFloat(), boxRect.maxY)
      } else {
        p.moveTo(boxRect.minX, boxRect.maxY)
        p.lineTo(boxRect.minX + w * .5f, boxRect.maxY)
        p.lineTo(boxRect.minX + w.toFloat(), boxRect.minY)
      }
      p.lineTo(boxRect.minX + w * .5f, boxRect.minY)
      p.closePath()
      val g2 = g.create() as? Graphics2D ?: return
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
      g2.paint = progressBar.foreground
      if (slope) {
        var i = boxRect.width + x
        while (i > -w) {
          g2.fill(AffineTransform.getTranslateInstance(i.toDouble(), 0.0).createTransformedShape(p))
          i -= w
        }
      } else {
        var i = -x
        while (i < boxRect.width) {
          g2.fill(AffineTransform.getTranslateInstance(i.toDouble(), 0.0).createTransformedShape(p))
          i += w
        }
      }
      g2.dispose()
    }
  }
}

class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    Thread.sleep(5000) // dummy task 1
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(50) // dummy task 2
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }
}

class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.propertyName
    if ("progress" == strPropertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
    }
  }

  init {
    progressBar.value = 0
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
