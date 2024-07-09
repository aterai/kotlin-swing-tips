package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.ChangeListener
import javax.swing.plaf.LayerUI

private var worker: SwingWorker<String, Unit?>? = null

fun makeUI(): Component {
  val m = DefaultBoundedRangeModel()
  val progressBar = JProgressBar(m)
  progressBar.orientation = SwingConstants.VERTICAL

  val progressBar0 = JProgressBar(m)
  progressBar0.orientation = SwingConstants.VERTICAL
  progressBar0.isStringPainted = false
  progressBar0.isStringPainted = true

  val button = JButton("Test")
  button.addActionListener {
    if (worker?.isDone != true) {
      worker?.cancel(true)
    }
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar))
      it.execute()
    }
  }

  val p = JPanel()
  p.add(progressBar)
  p.add(Box.createHorizontalStrut(5))
  p.add(progressBar0)
  p.add(Box.createHorizontalStrut(5))
  p.add(makeProgressBar1(m))
  p.add(Box.createHorizontalStrut(5))
  p.add(makeProgressBar2(m))

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))
  box.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  box.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker?.cancel(true)
      worker = null
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(JProgressBar(m), BorderLayout.NORTH)
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeProgressBar1(model: BoundedRangeModel): Component {
  val progressBar = TextLabelProgressBar(model)
  progressBar.orientation = SwingConstants.VERTICAL
  progressBar.isStringPainted = false
  return progressBar
}

private fun makeProgressBar2(model: BoundedRangeModel): Component {
  val label = JLabel("000/100")
  label.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
  val progressBar = object : JProgressBar(model) {
    override fun getPreferredSize() = super.getPreferredSize().also {
      val i = label.insets
      it.width = label.preferredSize.width + i.left + i.right
    }
  }
  progressBar.orientation = SwingConstants.VERTICAL
  progressBar.isStringPainted = false
  return JLayer(progressBar, ProgressBarLayerUI(label))
}

private open class BackgroundTask : SwingWorker<String, Unit?>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 100
    var total = 0
    while (current <= lengthOfTask && !isCancelled) {
      total += doSomething()
      progress = 100 * current++ / lengthOfTask
    }
    return "Done(${total}ms)"
  }

  @Throws(InterruptedException::class)
  protected fun doSomething(): Int {
    val iv = (1..50).random()
    Thread.sleep(iv.toLong())
    return iv
  }
}

private class ProgressListener(
  private val progressBar: JProgressBar,
) : PropertyChangeListener {
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

private class TextLabelProgressBar(
  model: BoundedRangeModel,
) : JProgressBar(model) {
  private val label = JLabel("000/100", CENTER)

  override fun updateUI() {
    removeAll()
    super.updateUI()
    layout = BorderLayout()
    EventQueue.invokeLater {
      SwingUtilities.updateComponentTreeUI(label)
      add(label)
      label.border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
    }
  }

  override fun createChangeListener() = ChangeListener {
    label.text = "%03d/100".format((100 * percentComplete).toInt())
  }

  override fun getPreferredSize() = super.getPreferredSize()?.also {
    val i = label.insets
    it.width = label.preferredSize.width + i.left + i.right
  }
}

private class ProgressBarLayerUI(
  private val label: JLabel,
) : LayerUI<JProgressBar>() {
  private val rubberStamp = JPanel()

  override fun updateUI(l: JLayer<out JProgressBar>?) {
    super.updateUI(l)
    SwingUtilities.updateComponentTreeUI(label)
  }

  override fun paint(
    g: Graphics,
    c: JComponent,
  ) {
    super.paint(g, c)
    val progress = (c as? JLayer<*>)?.view
    if (progress is JProgressBar) {
      label.text = "%03d/100".format((100 * progress.percentComplete).toInt())
      val d = label.preferredSize
      val x = (c.width - d.width) / 2
      val y = (c.height - d.height) / 2
      SwingUtilities.paintComponent(g, label, rubberStamp, x, y, d.width, d.height)
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
