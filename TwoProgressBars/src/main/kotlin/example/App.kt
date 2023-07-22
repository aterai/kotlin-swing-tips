package example

import java.awt.*
import javax.swing.*

private val logger = JTextArea()
private val statusPanel = JPanel(BorderLayout())
private val runButton = JButton("run")
private val cancelButton = JButton("cancel")
private val bar1 = JProgressBar()
private val bar2 = JProgressBar()
private var worker: SwingWorker<String, Progress>? = null

fun makeUI(): Component {
  logger.isEditable = false
  runButton.addActionListener {
    initStatusPanel(true)
    executeWorker()
  }
  cancelButton.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = null
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(runButton)
  box.add(Box.createHorizontalStrut(2))
  box.add(cancelButton)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(logger))
    it.add(box, BorderLayout.NORTH)
    it.add(statusPanel, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

fun executeWorker() {
  if (worker == null) {
    worker = ProgressTask()
  }
  worker?.execute()
}

fun appendLine(str: String) {
  logger.append(str)
  logger.caretPosition = logger.document.length
}

fun initStatusPanel(start: Boolean) {
  if (start) {
    runButton.isEnabled = false
    cancelButton.isEnabled = true
    bar1.value = 0
    bar2.value = 0
    statusPanel.add(bar1, BorderLayout.NORTH)
    statusPanel.add(bar2, BorderLayout.SOUTH)
  } else {
    runButton.isEnabled = true
    cancelButton.isEnabled = false
    statusPanel.removeAll()
  }
  statusPanel.revalidate()
}

private class ProgressTask : BackgroundTask() {
  override fun process(chunks: List<Progress>) {
    if (logger.isDisplayable && !isCancelled) {
      chunks.forEach {
        when (it.componentType) {
          ComponentType.TOTAL -> bar1.value = it.value as? Int ?: 0
          ComponentType.FILE -> bar2.value = it.value as? Int ?: 0
          ComponentType.LOG -> logger.append(it.value.toString())
        }
      }
    } else {
      cancel(true)
    }
  }

  override fun done() {
    if (!logger.isDisplayable) {
      cancel(true)
      return
    }
    initStatusPanel(false)
    runCatching {
      appendLine(if (isCancelled) "\nCancelled\n" else get())
    }.onFailure {
      appendLine("\n${it.message}\n")
    }
  }
}

private enum class ComponentType {
  TOTAL, FILE, LOG
}

private data class Progress(val componentType: ComponentType, val value: Any)

private open class BackgroundTask : SwingWorker<String, Progress>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val numOfFiles = 12 // fileList.size()
    publish(Progress(ComponentType.LOG, "Total number of files: $numOfFiles"))
    publish(Progress(ComponentType.LOG, "\n------------------------------\n"))
    while (current < numOfFiles && !isCancelled) {
      convertFileToSomething(100 * current++ / numOfFiles)
    }
    publish(Progress(ComponentType.LOG, "\n"))
    return "Done"
  }

  @Throws(InterruptedException::class)
  fun convertFileToSomething(iv: Int) {
    var current = 0
    val lengthOfFile = (10..60).random()
    publish(Progress(ComponentType.LOG, "*"))
    publish(Progress(ComponentType.TOTAL, iv))
    while (current <= lengthOfFile && !isCancelled) {
      doSomething(100 * current / lengthOfFile)
      current++
    }
  }

  @Throws(InterruptedException::class)
  fun doSomething(iv: Int) {
    publish(Progress(ComponentType.FILE, iv + 1))
    Thread.sleep(20)
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
