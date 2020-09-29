package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports

private fun executeWorker(
  monitor: ProgressMonitor,
  lengthOfTask: Int,
  button: JButton,
  area: JTextArea
) {
  val worker = object : BackgroundTask(lengthOfTask) {
    override fun process(chunks: List<String>) {
      if (!area.isDisplayable) {
        println("process: DISPOSE_ON_CLOSE")
        cancel(true)
        return
      }
      for (message in chunks) {
        monitor.note = message
      }
    }

    public override fun done() {
      if (!area.isDisplayable) {
        println("done: DISPOSE_ON_CLOSE")
        cancel(true)
        return
      }
      button.isEnabled = true
      monitor.close()
      if (isCancelled) {
        area.append("Cancelled\n")
      } else {
        val msg = runCatching { get() }.onFailure { "Error: ${it.message}" }
        area.append("$msg\n")
      }
      area.caretPosition = area.document.length
    }
  }
  worker.addPropertyChangeListener(ProgressListener(monitor))
  worker.execute()
}

fun makeUI(): Component {
  val area = JTextArea()
  area.isEditable = false
  val dmy = ProgressMonitor(null, "message dummy", "note", 0, 100)
  val millisToDecide = SpinnerNumberModel(dmy.millisToDecideToPopup, 0, 5 * 1000, 100)
  val millisToPopup = SpinnerNumberModel(dmy.millisToPopup, 0, 5 * 1000, 100)
  val runButton = JButton("run")
  runButton.addActionListener {
    val w = SwingUtilities.getWindowAncestor(runButton)
    val toDecideToPopup = millisToDecide.number.toInt()
    val toPopup = millisToPopup.number.toInt()
    val monitor = ProgressMonitor(w, "message", "note", 0, 100)
    monitor.millisToDecideToPopup = toDecideToPopup
    monitor.millisToPopup = toPopup

    // System.out.println(monitor.getMillisToDecideToPopup());
    // System.out.println(monitor.getMillisToPopup());
    val lengthOfTask = 10_000.coerceAtLeast(toDecideToPopup * 5)
    runButton.isEnabled = false
    executeWorker(monitor, lengthOfTask, runButton, area)
  }
  val c = GridBagConstraints()
  c.gridx = 0
  c.insets = Insets(5, 5, 5, 0)
  c.anchor = GridBagConstraints.LINE_END
  val p = JPanel(GridBagLayout())
  p.add(JLabel("MillisToDecideToPopup:"), c)
  p.add(JLabel("MillisToPopup:"), c)
  c.gridx = 1
  c.weightx = 1.0
  c.fill = GridBagConstraints.HORIZONTAL
  p.add(JSpinner(millisToDecide), c)
  p.add(JSpinner(millisToPopup), c)
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(runButton)
  return JPanel(BorderLayout(5, 5)).also {
    it.add(JScrollPane(area))
    it.add(p, BorderLayout.NORTH)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private open class BackgroundTask(private val lengthOfTask: Int) : SwingWorker<String, String>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    var current = 0
    while (current < lengthOfTask && !isCancelled) {
      if (current % 10 == 0) {
        Thread.sleep(5)
      }
      val v = 100 * current / lengthOfTask
      progress = v
      publish("$v%")
      current++
    }
    return "Done"
  }
}

private class ProgressListener(private val monitor: ProgressMonitor) : PropertyChangeListener {
  override fun propertyChange(e: PropertyChangeEvent) {
    if ("progress" == e.propertyName) {
      monitor.setProgress(e.newValue as Int)
      (e.source as? SwingWorker<*, *>)?.takeIf {
        it.isDone || monitor.isCanceled
      }?.cancel(true)
    }
  }

  init {
    monitor.setProgress(0)
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
