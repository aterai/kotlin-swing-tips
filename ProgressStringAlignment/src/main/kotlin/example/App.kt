package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*
import javax.swing.event.ChangeListener

private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel()
  val progressBar1 = StringAlignmentProgressBar(model, SwingConstants.RIGHT)
  val progressBar2 = StringAlignmentProgressBar(model, SwingConstants.LEFT)
  progressBar2.border = BorderFactory.createTitledBorder("TitledBorder")

  val check = JCheckBox("setStringPainted")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    listOf(progressBar1, progressBar2).forEach { it.isStringPainted = b }
  }

  val button = JButton("Test")
  button.addActionListener {
    if (worker?.isDone != true) {
      worker?.cancel(true)
    }
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar1))
      it.execute()
    }
  }

  val p = JPanel()
  p.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
  p.add(progressBar1)
  p.add(progressBar2)
  p.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker?.cancel(true)
      worker = null
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(Box.createHorizontalStrut(5))
  box.add(button)
  box.add(Box.createHorizontalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class StringAlignmentProgressBar(
  model: BoundedRangeModel,
  horAlignment: Int,
) : JProgressBar(model) {
  private val label = JLabel(" ", horAlignment)

  override fun updateUI() {
    removeAll()
    super.updateUI()
    layout = BorderLayout()
    EventQueue.invokeLater {
      add(label)
      label.border = BorderFactory.createEmptyBorder(0, 4, 0, 4)
    }
  }

  override fun createChangeListener() = ChangeListener { label.text = string }
}

private open class BackgroundTask : SwingWorker<String, Void>() {
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

fun main() {
  EventQueue.invokeLater {
    runCatching {
      UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel")
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
