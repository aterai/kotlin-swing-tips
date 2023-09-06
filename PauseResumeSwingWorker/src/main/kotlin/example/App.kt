package example

import java.awt.*
import javax.swing.*

private const val PAUSE = "pause"
private val area = JTextArea()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("run")
private val cancelButton = JButton("cancel")
private val pauseButton = JButton(PAUSE)
private val bar1 = JProgressBar()
private val bar2 = JProgressBar()
private var worker: BackgroundTask? = null

fun makeUI(): Component {
  area.isEditable = false

  runButton.addActionListener {
    runButton.isEnabled = false
    cancelButton.isEnabled = true
    pauseButton.isEnabled = true
    bar1.value = 0
    bar2.value = 0
    statusPanel.add(bar1, BorderLayout.NORTH)
    statusPanel.add(bar2, BorderLayout.SOUTH)
    statusPanel.revalidate()
    worker = ProgressTask().also { it.execute() }
  }

  pauseButton.isEnabled = false
  pauseButton.addActionListener { e ->
    (e.source as? JButton)?.also { b ->
      b.text = worker?.let {
        it.isPaused = it.isPaused xor true
        if (it.isCancelled || it.isPaused) PAUSE else "resume"
      } ?: PAUSE
    }
  }

  cancelButton.isEnabled = false
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = null
    pauseButton.text = PAUSE
    pauseButton.isEnabled = false
  }

  val box = createRightAlignButtonBox4(pauseButton, cancelButton, runButton)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(area))
    it.add(box, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private class ProgressTask : BackgroundTask() {
  override fun process(chunks: List<Progress>) {
    if (area.isDisplayable && !isCancelled) {
      chunks.forEach { processChunks(it) }
    } else {
      cancel(true)
    }
  }

  override fun done() {
    if (!area.isDisplayable) {
      cancel(true)
      return
    }
    updateComponentDone()
    val message = runCatching {
      "%n%s%n".format(if (isCancelled) "Cancelled" else get())
    }.getOrNull() ?: "%n%s%n".format("Interrupted")
    appendLine(message)
  }
}

fun updateComponentDone() {
  runButton.requestFocusInWindow()
  runButton.isEnabled = true
  cancelButton.isEnabled = false
  pauseButton.isEnabled = false
  statusPanel.removeAll()
  statusPanel.revalidate()
}

private fun processChunks(progress: Progress) {
  when (progress.component) {
    ProgressType.TOTAL -> bar1.value = progress.value as? Int ?: 0
    ProgressType.FILE -> bar2.value = progress.value as? Int ?: 0
    ProgressType.LOG -> area.append(progress.value.toString())
    ProgressType.PAUSE -> textProgress(progress.value as? Boolean ?: false)
    // else -> throw AssertionError("Unknown Progress")
  }
}

private fun textProgress(append: Boolean) {
  if (append) {
    area.append("*")
  } else {
    runCatching {
      val doc = area.document
      doc.remove(doc.length - 1, 1)
    }
  }
}

fun appendLine(str: String) {
  area.append(str)
  area.caretPosition = area.document.length
}

// @see https://ateraimemo.com/Swing/ButtonWidth.html
private fun createRightAlignButtonBox4(vararg list: Component): Component {
  val buttonWidth = 80
  val gap = 5
  val layout = SpringLayout()
  val p = object : JPanel(layout) {
    override fun getPreferredSize(): Dimension {
      val maxHeight = list.maxOfOrNull { it.preferredSize.height } ?: 0
      return Dimension(buttonWidth * list.size + gap + gap, maxHeight + gap + gap)
    }
  }
  var x = layout.getConstraint(SpringLayout.WIDTH, p)
  val y = Spring.constant(gap)
  val g = Spring.minus(Spring.constant(gap))
  val w = Spring.constant(buttonWidth)
  for (b in list) {
    val constraints = layout.getConstraints(b)
    x = Spring.sum(x, g)
    constraints.setConstraint(SpringLayout.EAST, x)
    constraints.y = y
    constraints.width = w
    p.add(b)
    x = Spring.sum(x, Spring.minus(w))
  }
  return p
}

private enum class ProgressType {
  TOTAL,
  FILE,
  LOG,
  PAUSE
}

private data class Progress(val component: ProgressType, val value: Any)

private open class BackgroundTask : SwingWorker<String, Progress>() {
  var isPaused = false

  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 12 // fileList.size()
    publish(Progress(ProgressType.LOG, "Length Of Task: $lengthOfTask"))
    publish(Progress(ProgressType.LOG, "\n------------------------------\n"))
    while (current < lengthOfTask && !isCancelled) {
      publish(Progress(ProgressType.TOTAL, 100 * current / lengthOfTask))
      publish(Progress(ProgressType.LOG, "*"))
      convertFileToSomething()
      current++
    }
    publish(Progress(ProgressType.LOG, "\n"))
    return "Done"
  }

  @Throws(InterruptedException::class)
  private fun convertFileToSomething() {
    var blinking = false
    var current = 0
    val lengthOfTask = (10..60).random()
    while (current <= lengthOfTask && !isCancelled) {
      if (isPaused) {
        Thread.sleep(500)
        publish(Progress(ProgressType.PAUSE, blinking))
        blinking = blinking xor true
        continue
      }
      val iv = 100 * current / lengthOfTask
      Thread.sleep(20) // sample
      publish(Progress(ProgressType.FILE, iv + 1))
      current++
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
