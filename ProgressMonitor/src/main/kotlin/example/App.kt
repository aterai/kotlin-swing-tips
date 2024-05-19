package example

import java.awt.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*

fun makeUI(): Component {
  val area = JTextArea()
  area.isEditable = false

  val monitor = ProgressMonitor(area, "message", "note", 0, 100)

  val runButton = JButton("run")
  runButton.addActionListener {
    runButton.isEnabled = false
    monitor.setProgress(0)
    val worker = object : BackgroundTask() {
      override fun process(chunks: List<String>) {
        chunks.forEach(monitor::setNote)
      }

      override fun done() {
        runButton.isEnabled = true
        monitor.close()
        runCatching {
          val msg = if (isCancelled) "Cancelled" else get()
          area.append("$msg\n")
        }.onFailure {
          if (it is InterruptedException) {
            Thread.currentThread().interrupt()
          }
          area.append("Error: ${it.message}\n")
        }
        area.caretPosition = area.document.length
      }
    }
    worker.addPropertyChangeListener(ProgressListener(monitor))
    worker.execute()
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(runButton)

  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(area))
    it.add(box, BorderLayout.NORTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask : SwingWorker<String, String>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 120
    while (current < lengthOfTask && !isCancelled) {
      doSomething()
      current++
      progress = 100 * current / lengthOfTask
      publish("$current/$lengthOfTask")
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  protected fun doSomething() {
    Thread.sleep(50)
  }
}

private class ProgressListener(
  private val monitor: ProgressMonitor,
) : PropertyChangeListener {
  init {
    monitor.setProgress(0)
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      monitor.setProgress(e.newValue as? Int ?: 0)
      val o = e.source
      if (o is SwingWorker<*, *> && (o.isDone || monitor.isCanceled)) {
        o.cancel(true)
      }
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
