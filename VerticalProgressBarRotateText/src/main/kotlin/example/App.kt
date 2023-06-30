package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.*

private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  // UIManager.put("ProgressBar.rotateText", false)
  val progressBar1 = JProgressBar(SwingConstants.VERTICAL)
  progressBar1.isStringPainted = true

  val progressBar2 = JProgressBar(SwingConstants.VERTICAL)
  progressBar2.isStringPainted = true
  val d = UIDefaults()
  d["ProgressBar.rotateText"] = false
  // NimbusDefaults has a typo in a L&F property - Java Bug System
  // https://bugs.openjdk.org/browse/JDK-8285962
  // d["ProgressBar.vertictalSize"] = Dimension(50, 150)
  d["ProgressBar.verticalSize"] = Dimension(50, 150)
  progressBar2.putClientProperty("Nimbus.Overrides", d)
  progressBar2.putClientProperty("Nimbus.Overrides.InheritDefaults", true)

  val button = JButton("Test")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar1))
      it.addPropertyChangeListener(ProgressListener(progressBar2))
      it.execute()
    }
  }

  val p = JPanel()
  p.add(progressBar1)
  p.add(Box.createHorizontalStrut(16))
  p.add(progressBar2)
  p.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker?.cancel(true)
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class BackgroundTask : SwingWorker<String, Void>() {
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
  private fun doSomething(): Int {
    val iv = (1..50).random()
    Thread.sleep(iv.toLong())
    return iv
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
