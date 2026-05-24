package example

import java.awt.*
import javax.swing.*

private const val TXT_PAUSE = "pause"
private const val TXT_RESUME = "resume"
private val area = JTextArea()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("run")
private val cancelButton = JButton("cancel")
private val pauseButton = JButton(TXT_PAUSE)
private val bar1 = JProgressBar()
private val bar2 = JProgressBar()
private var worker: BackgroundTask? = null

fun createUI(): Component {
  area.isEditable = false

  runButton.addActionListener {
    updateButtonsAndStatusPanel(true)
    worker = ProgressTask().also {
      it.execute()
    }
  }

  pauseButton.isEnabled = false
  pauseButton.addActionListener { e ->
    (e.source as? JButton)?.also { b ->
      b.text = worker?.let {
        it.toggle()
        if (it.isCancelled || it.isPaused) TXT_PAUSE else TXT_RESUME
      } ?: TXT_PAUSE
    }
  }

  cancelButton.isEnabled = false
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = null
    pauseButton.text = TXT_PAUSE
    pauseButton.isEnabled = false
  }
  val buttons = listOf<Component>(pauseButton, cancelButton, runButton)
  val box = createRightAlignBox(buttons, 80, 5)
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
      chunks.forEach { updateProgress(it) }
    } else {
      cancel(true)
    }
  }

  override fun done() {
    if (area.isDisplayable) {
      updateButtonsAndStatusPanel(false)
      appendLine("%n%s%n".format(doneMessage()))
    }
  }
}

private fun updateProgress(progress: Progress) {
  progress.type.update(progress.value)
}

fun updateTotalProgress(value: Int) {
  bar1.setValue(value)
}

fun updateFileProgress(value: Int) {
  bar2.setValue(value)
}

fun appendLog(value: Any) {
  area.append(value.toString())
}

fun updatePauseMarker(append: Boolean) {
  if (append) {
    area.append("*")
  } else {
    runCatching {
      val doc = area.document
      doc.remove(doc.length - 1, 1)
    }
  }
}

private fun updateButtonsAndStatusPanel(running: Boolean) {
  runButton.setEnabled(!running)
  cancelButton.setEnabled(running)
  pauseButton.setEnabled(running)
  if (running) {
    bar1.setValue(0)
    bar2.setValue(0)
    statusPanel.add(bar1, BorderLayout.NORTH)
    statusPanel.add(bar2, BorderLayout.SOUTH)
  } else {
    runButton.requestFocusInWindow()
    statusPanel.removeAll()
  }
  statusPanel.revalidate()
}

fun appendLine(str: String) {
  area.append(str)
  area.caretPosition = area.document.length
}


fun createRightAlignBox(list: List<Component>, width: Int, gap: Int): Component {
  val layout = SpringLayout()
  val p = object : JPanel(layout) {
    override fun getPreferredSize(): Dimension {
      val maxHeight = list.maxOfOrNull { it.preferredSize.height } ?: 0
      return Dimension(width * list.size + gap * 2, maxHeight + gap * 2)
    }
  }
  var x = layout.getConstraint(SpringLayout.WIDTH, p)
  val y = Spring.constant(gap)
  val g = Spring.minus(Spring.constant(gap))
  val w = Spring.constant(width)
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
  TOTAL {
    override fun update(value: Any) {
      updateTotalProgress((value as Int))
    }
  },
  FILE {
    override fun update(value: Any) {
      updateFileProgress(value as Int)
    }
  },
  LOG {
    override fun update(value: Any) {
      appendLog(value)
    }
  },
  PAUSE {
    override fun update(value: Any) {
      updatePauseMarker(value as Boolean)
    }
  };

  abstract fun update(value: Any)
}

private class Progress(val type: ProgressType, val value: Any)

private open class BackgroundTask : SwingWorker<String, Progress>() {
  var isPaused = false

  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 12 // fileList.size()
    publish(Progress(ProgressType.LOG, "Length Of Task: $lengthOfTask"))
    publish(Progress(ProgressType.LOG, "\n------------------------------\n"))
    while (current < lengthOfTask && !isCancelled) {
      convertFileToSomething(100 * current / lengthOfTask)
      current++
    }
    publish(Progress(ProgressType.LOG, "\n"))
    return "Done"
  }

  @Throws(InterruptedException::class)
  private fun convertFileToSomething(progress: Int) {
    var blinking = false
    var current = 0
    val lengthOfTask = (10..60).random()
    publish(Progress(ProgressType.TOTAL, progress))
    publish(Progress(ProgressType.LOG, "*"))
    while (current <= lengthOfTask && !isCancelled) {
      if (isPaused) {
        pause(blinking)
        blinking = !blinking
        continue
      }
      doSomething(100 * current / lengthOfTask)
      current++
    }
  }

  fun toggle() {
    this.isPaused = !this.isPaused
  }

  @Throws(InterruptedException::class)
  private fun pause(blinking: Boolean) {
    Thread.sleep(500)
    publish(Progress(ProgressType.PAUSE, blinking))
  }

  @Throws(InterruptedException::class)
  private fun doSomething(progress: Int) {
    Thread.sleep(20)
    publish(Progress(ProgressType.FILE, progress + 1))
  }

  fun doneMessage() = runCatching {
      if (isCancelled) "Cancelled" else get()
    }.onFailure {
      if (it is InterruptedException) {
        Thread.currentThread().interrupt()
      }
    }.getOrNull() ?: "Error"
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
