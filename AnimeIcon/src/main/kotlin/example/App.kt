package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.geom.AffineTransform
import java.awt.geom.Ellipse2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

private val area = JTextArea()
private val bar = JProgressBar()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("run")
private val cancelButton = JButton("cancel")
private val loadingLabel = LoadingLabel()

private var worker: BackgroundTask? = null

private fun executeWorker() {
  runButton.isEnabled = false
  cancelButton.isEnabled = true
  loadingLabel.startAnimation()
  statusPanel.removeAll()
  statusPanel.add(bar)
  statusPanel.revalidate()
  bar.isIndeterminate = true
  val w = object : BackgroundTask() {
    override fun process(chunks: List<String?>) {
      if (isCancelled) {
        return
      }
      if (!runButton.isDisplayable) {
        cancel(true)
        return
      }
      chunks.forEach { appendLine(it) }
    }

    override fun done() {
      if (!runButton.isDisplayable) {
        cancel(true)
        return
      }
      loadingLabel.stopAnimation()
      runButton.isEnabled = true
      cancelButton.isEnabled = false
      statusPanel.remove(bar)
      statusPanel.revalidate()
      appendLine("\n")
      runCatching {
        appendLine(if (isCancelled) "Cancelled" else get())
      }.onFailure {
        if (it is InterruptedException) {
          Thread.currentThread().interrupt()
        }
        appendLine("Interrupted")
      }
      appendLine("\n\n")
    }
  }
  w.addPropertyChangeListener(ProgressListener(bar))
  w.execute()
  worker = w
}

private fun appendLine(str: String?) {
  area.append(str)
  area.caretPosition = area.document.length
}

fun makeUI(): Component {
  area.isEditable = false
  area.lineWrap = true
  runButton.addActionListener { executeWorker() }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = null
  }
  val box = Box.createHorizontalBox()
  box.add(loadingLabel)
  box.add(Box.createHorizontalGlue())
  box.add(runButton)
  box.add(cancelButton)
  return JPanel(BorderLayout()).also {
    it.add(box, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.add(JScrollPane(area))
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask : SwingWorker<String, String?>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    Thread.sleep(1000)
    var current = 0
    val lengthOfTask = 120 // list.size()
    publish("Length Of Task: $lengthOfTask")
    publish("\n------------------------------\n")
    while (current < lengthOfTask && !isCancelled) {
      publish(".")
      progress = 100 * current / lengthOfTask
      Thread.sleep(50)
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
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
    }
  }
}

private class LoadingLabel : JLabel() {
  private val icon = AnimeIcon()
  private val animator = Timer(100) {
    icon.next()
    repaint()
  }

  init {
    setIcon(icon)
    addHierarchyListener { e ->
      val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
      if (b && !e.component.isDisplayable) {
        animator.stop()
      }
    }
  }

  fun startAnimation() {
    icon.setRunning(true)
    animator.start()
  }

  fun stopAnimation() {
    icon.setRunning(false)
    animator.stop()
  }
}

private class AnimeIcon : Icon {
  private val list = mutableListOf<Shape>()
  private val dim: Dimension
  private var running = false
  private var rotate = 45.0

  init {
    val r = 4.0
    val s = Ellipse2D.Double(0.0, 0.0, 2.0 * r, 2.0 * r)
    for (i in 0..7) {
      val at = AffineTransform.getRotateInstance(i * 2 * Math.PI / 8)
      at.concatenate(AffineTransform.getTranslateInstance(r, r))
      list.add(at.createTransformedShape(s))
    }
    val d = (r * 2 * (1 + 3)).toInt()
    dim = Dimension(d, d)
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = c?.background ?: Color.WHITE
    g2.fillRect(x, y, iconWidth, iconHeight)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = ELLIPSE_COLOR
    val xx = x + dim.width / 2.0
    val yy = y + dim.height / 2.0
    val at = AffineTransform.getRotateInstance(Math.toRadians(rotate), xx, yy)
    at.concatenate(AffineTransform.getTranslateInstance(xx, yy))
    val size = list.size
    for (i in 0 until size) {
      val alpha = if (running) (i + 1f) / size else .5f
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
      g2.fill(at.createTransformedShape(list[i]))
    }
    g2.dispose()
  }

  override fun getIconWidth() = dim.width

  override fun getIconHeight() = dim.height

  operator fun next() {
    if (running) {
      rotate = (rotate + 45.0) % 360.0 // 45 = 360 / 8
    }
  }

  fun setRunning(running: Boolean) {
    this.running = running
  }

  companion object {
    private val ELLIPSE_COLOR = Color(0xE6_B3_B3)
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
