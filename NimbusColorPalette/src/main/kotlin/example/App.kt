package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports

@Transient
private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  val def = UIManager.getLookAndFeelDefaults()
  def["nimbusOrange"] = Color(255, 220, 35, 200)
  val d = UIDefaults()
  val painter = Painter { g, _: JComponent, w, h ->
    g.color = Color(100, 250, 120, 50)
    g.fillRect(0, 0, w, h)
    g.color = Color(100, 250, 120, 150)
    g.fillRect(3, h / 2, w - 6, h / 2 - 2)
  }
  d["ProgressBar[Enabled].foregroundPainter"] = painter
  d["ProgressBar[Enabled+Finished].foregroundPainter"] = painter
  val model = DefaultBoundedRangeModel(0, 0, 0, 100)
  val progressBar1 = JProgressBar(model)
  val progressBar2 = JProgressBar(model)
  progressBar2.putClientProperty("Nimbus.Overrides", d)
  val p = JPanel()
  p.border = BorderFactory.createEmptyBorder(16, 16, 16, 16)
  p.add(progressBar1)
  p.add(progressBar2)
  val button = JButton("Test start")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progressBar1))
      it.execute()
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  box.add(Box.createHorizontalStrut(5))

  val panel = JPanel(BorderLayout())
  panel.addHierarchyListener { e ->
    val isDisplayableChanged = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (worker != null && isDisplayableChanged && !e.component.isDisplayable) {
      println("DISPOSE_ON_CLOSE")
      worker?.cancel(true)
      worker = null
    }
  }
  panel.add(p)
  panel.add(box, BorderLayout.SOUTH)
  panel.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  panel.preferredSize = Dimension(320, 240)
  return panel
}

private class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  public override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled) {
      Thread.sleep(50) // dummy task
      progress = 100 * current / lengthOfTask
      current++
    }
    return "Done"
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.propertyName
    if ("progress" == strPropertyName) {
      progressBar.isIndeterminate = false
      val progress = e.newValue as Int
      progressBar.value = progress
    }
  }

  init {
    progressBar.value = 0
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
