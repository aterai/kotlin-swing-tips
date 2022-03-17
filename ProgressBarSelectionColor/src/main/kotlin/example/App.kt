package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.basic.BasicProgressBarUI

private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel()
  val progressBar0 = JProgressBar(model)
  progressBar0.isStringPainted = true
  UIManager.put("ProgressBar.foreground", Color.RED)
  UIManager.put("ProgressBar.selectionForeground", Color.ORANGE)
  UIManager.put("ProgressBar.background", Color.WHITE)
  UIManager.put("ProgressBar.selectionBackground", Color.RED)

  val progressBar1 = JProgressBar(model)
  progressBar1.isStringPainted = true

  val progressBar2 = JProgressBar(model)
  progressBar2.isStringPainted = true
  progressBar2.foreground = Color.BLUE
  progressBar2.background = Color.CYAN.brighter()
  progressBar2.ui = object : BasicProgressBarUI() {
    override fun getSelectionForeground() = Color.PINK

    override fun getSelectionBackground() = Color.BLUE
  }

  val p = JPanel(GridLayout(5, 1))
  p.add(makePanel(progressBar0))
  p.add(makePanel(progressBar1))
  p.add(makePanel(progressBar2))

  val button = JButton("Test start")
  button.addActionListener {
    if (worker?.isDone != true) {
      worker?.cancel(true)
    }
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar0))
      it.addPropertyChangeListener(ProgressListener(progressBar1))
      it.addPropertyChangeListener(ProgressListener(progressBar2))
      it.execute()
    }
  }

  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))
  box.addHierarchyListener { e ->
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      worker?.cancel(true)
      worker = null
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel(cmp: Component): Component {
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  val p = JPanel(GridBagLayout())
  p.add(cmp, c)
  return p
}

private open class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      doSomething()
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  protected fun doSomething() {
    Thread.sleep(50) // dummy task
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
//    runCatching {
//      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
//    }.onFailure {
//      it.printStackTrace()
//      Toolkit.getDefaultToolkit().beep()
//    }
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
