package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.geom.Ellipse2D
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Collections
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer

private val area = JTextArea()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("run")
private val cancelButton = JButton("cancel")
private val bar = JProgressBar()
private val loadingLabel = LoadingLabel()
private var worker: SwingWorker<String, String>? = null

private class AnimationTask : BackgroundTask() {
  override fun process(chunks: List<String>) {
    if (isCancelled) {
      return
    }
    if (!area.isDisplayable) {
      cancel(true)
      return
    }
    chunks.forEach { appendText(it) }
  }

  public override fun done() {
    if (!area.isDisplayable) {
      cancel(true)
      return
    }
    updateComponentDone()
    runCatching {
      val msg = if (isCancelled) "Cancelled" else get()
      appendText("$msg\n")
    }.onFailure {
      if (it is InterruptedException) {
        Thread.currentThread().interrupt()
      }
      appendText("Error: ${it.message}\n")
    }
  }
}

private fun executeWorker() {
  runButton.isEnabled = false
  cancelButton.isEnabled = true
  loadingLabel.startAnimation()
  statusPanel.isVisible = true
  bar.isIndeterminate = true
  worker = AnimationTask().also {
    it.addPropertyChangeListener(ProgressListener(bar))
    it.execute()
  }
}

private fun updateComponentDone() {
  loadingLabel.stopAnimation()
  runButton.isEnabled = true
  cancelButton.isEnabled = false
  statusPanel.isVisible = false
}

private fun appendText(str: String?) {
  area.append(str)
  area.caretPosition = area.document.length
}

fun makeUI(): Component {
  area.isEditable = false
  area.lineWrap = true
  runButton.addActionListener { executeWorker() }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
  }
  val box = Box.createHorizontalBox()
  box.add(loadingLabel)
  box.add(Box.createHorizontalGlue())
  box.add(runButton)
  box.add(Box.createHorizontalStrut(2))
  box.add(cancelButton)

  statusPanel.add(bar)
  statusPanel.isVisible = false

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(area))
    it.add(box, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

open class BackgroundTask : SwingWorker<String, String>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    Thread.sleep(2000)
    var current = 0
    val lengthOfTask = 120 // list.size();
    publish("Length Of Task: $lengthOfTask")
    publish("\n------------------------------\n")
    while (current < lengthOfTask && !isCancelled) {
      Thread.sleep(50)
      progress = 100 * current / lengthOfTask
      publish(".")
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
    val source = e.source
    if (!progressBar.isDisplayable && source is SwingWorker<*, *>) {
      source.cancel(true)
    }
    if ("progress" == e.propertyName) {
      progressBar.isIndeterminate = false
      progressBar.value = e.newValue as? Int ?: 0
    }
  }
}

private class LoadingLabel : JLabel() {
  @Transient private val icon = LoadingIcon()
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

private class LoadingIcon : Icon {
  private val list = listOf(
    Ellipse2D.Double(SX + 3 * R, SY + 0 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 5 * R, SY + 1 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 6 * R, SY + 3 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 5 * R, SY + 5 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 3 * R, SY + 6 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 1 * R, SY + 5 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 0 * R, SY + 3 * R, 2 * R, 2 * R),
    Ellipse2D.Double(SX + 1 * R, SY + 1 * R, 2 * R, 2 * R)
  )
  private var running = false
  operator fun next() {
    if (running) {
      Collections.rotate(list, 1)
    }
  }

  fun setRunning(running: Boolean) {
    this.running = running
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.paint = c?.background ?: Color.WHITE
    g2.fillRect(0, 0, iconWidth, iconHeight)
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = ELLIPSE_COLOR
    val size = list.size.toFloat()
    list.forEach {
      val alpha = if (running) (list.indexOf(it) + 1) / size else .5f
      g2.composite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha)
      g2.fill(it)
    }
    g2.dispose()
  }

  override fun getIconWidth() = WIDTH

  override fun getIconHeight() = HEIGHT

  companion object {
    private val ELLIPSE_COLOR = Color(0x80_80_80)
    private const val R = 2.0
    private const val SX = 1.0
    private const val SY = 1.0
    private const val WIDTH = (R * 8 + SX * 2).toInt()
    private const val HEIGHT = (R * 8 + SY * 2).toInt()
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
