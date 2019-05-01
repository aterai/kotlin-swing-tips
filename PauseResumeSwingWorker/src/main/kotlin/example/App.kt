package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.util.Random
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.text.BadLocationException

class MainPanel : JPanel(BorderLayout(5, 5)) {
  private val area = JTextArea()
  private val statusPanel = JPanel(BorderLayout())
  private val runButton = JButton("run")
  private val cancelButton = JButton("cancel")
  private val pauseButton = JButton("resume")
  private val bar1 = JProgressBar()
  private val bar2 = JProgressBar()
  @Transient
  private var worker: BackgroundTask? = null

  init {
    area.setEditable(false)

    runButton.addActionListener {
      runButton.setEnabled(false)
      cancelButton.setEnabled(true)
      pauseButton.setEnabled(true)
      bar1.setValue(0)
      bar2.setValue(0)
      statusPanel.add(bar1, BorderLayout.NORTH)
      statusPanel.add(bar2, BorderLayout.SOUTH)
      statusPanel.revalidate()
      val w = ProgressTask()
      worker = w
      w.execute()
    }

    pauseButton.setEnabled(false)
    pauseButton.addActionListener { e ->
      val b = e.getSource() as JButton
      worker?.also {
        if (it.isCancelled() || it.isPaused) {
          b.setText("pause")
        } else {
          b.setText("resume")
        }
        it.isPaused = it.isPaused xor true
      }?.also {
        b.setText("pause")
      }
    }

    cancelButton.setEnabled(false)
    cancelButton.addActionListener {
      worker?.takeIf { it.isDone() }?.cancel(true)
      worker = null
      pauseButton.setText("pause")
      pauseButton.setEnabled(false)
    }

    val box = createRightAlignButtonBox4(listOf(pauseButton, cancelButton, runButton), 80, 5)
    add(JScrollPane(area))
    add(box, BorderLayout.NORTH)
    add(statusPanel, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  open inner class ProgressTask : BackgroundTask() {
    protected override fun process(chunks: List<Progress>) {
      if (isCancelled()) {
        return
      }
      if (!isDisplayable()) {
        println("process: DISPOSE_ON_CLOSE")
        cancel(true)
        return
      }
      processChunks(chunks)
    }

    override fun done() {
      if (!isDisplayable()) {
        println("done: DISPOSE_ON_CLOSE")
        cancel(true)
        return
      }
      updateComponentDone()
      val message = runCatching {
        String.format("%n%s%n", if (isCancelled()) "Cancelled" else get())
      }.getOrNull() ?: String.format("%n%s%n", "Interrupted")
      appendLine(message)
    }
  }

  fun updateComponentDone() {
    runButton.requestFocusInWindow()
    runButton.setEnabled(true)
    cancelButton.setEnabled(false)
    pauseButton.setEnabled(false)
    statusPanel.removeAll()
    statusPanel.revalidate()
  }

  protected fun processChunks(chunks: List<Progress>) {
    for (s in chunks) {
      when (s.component) {
        ProgressType.TOTAL -> bar1.setValue(s.value as Int)
        ProgressType.FILE -> bar2.setValue(s.value as Int)
        ProgressType.LOG -> area.append(s.value.toString())
        ProgressType.PAUSE -> textProgress(s.value as Boolean)
        // else -> throw AssertionError("Unknown Progress")
      }
    }
  }

  fun textProgress(append: Boolean) {
    if (append) {
      area.append("*")
    } else {
      try {
        val doc = area.getDocument()
        doc.remove(doc.getLength() - 1, 1)
      } catch (ex: BadLocationException) {
        // should never happen
        val wrap = StringIndexOutOfBoundsException(ex.offsetRequested())
        wrap.initCause(ex)
        throw wrap
      }
    }
  }

  fun appendLine(str: String) {
    area.append(str)
    area.setCaretPosition(area.getDocument().getLength())
  }

  // @see https://ateraimemo.com/Swing/ButtonWidth.html
  fun createRightAlignButtonBox4(list: List<Component>, buttonWidth: Int, gap: Int): Component {
    val layout = SpringLayout()
    val p = object : JPanel(layout) {
      override fun getPreferredSize(): Dimension {
        val maxHeight = list.map { it.getPreferredSize().height }.max() ?: 0
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
      constraints.setY(y)
      constraints.setWidth(w)
      p.add(b)
      x = Spring.sum(x, Spring.minus(w))
    }
    return p
  }
}

enum class ProgressType {
  TOTAL, FILE, LOG, PAUSE
}

class Progress(val component: ProgressType, val value: Any)

open class BackgroundTask : SwingWorker<String, Progress>() {
  private val rnd = Random()
  var isPaused: Boolean = false

  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 12 // filelist.size();
    publish(Progress(ProgressType.LOG, "Length Of Task: $lengthOfTask"))
    publish(Progress(ProgressType.LOG, "\n------------------------------\n"))
    while (current < lengthOfTask && !isCancelled()) {
      publish(Progress(ProgressType.TOTAL, 100 * current / lengthOfTask))
      publish(Progress(ProgressType.LOG, "*"))
      try {
        convertFileToSomething()
      } catch (ex: InterruptedException) {
        return "Interrupted"
      }
      current++
    }
    publish(Progress(ProgressType.LOG, "\n"))
    return "Done"
  }

  @Throws(InterruptedException::class)
  private fun convertFileToSomething() {
    var blinking = false
    var current = 0
    val lengthOfTask = 10 + rnd.nextInt(50)
    while (current <= lengthOfTask && !isCancelled()) {
      if (isPaused) {
        try {
          Thread.sleep(500)
        } catch (ex: InterruptedException) {
          return
        }
        publish(Progress(ProgressType.PAUSE, blinking))
        blinking = blinking xor true
        continue
      }
      val iv = 100 * current / lengthOfTask
      Thread.sleep(20) // dummy
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
