package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.image.BufferedImage
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  val m = DefaultBoundedRangeModel()
  val progress = JProgressBar(m)

  val button = JButton("Test")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progress))
      it.execute()
    }
  }
  val box = Box.createVerticalBox()
  box.border = BorderFactory.createEmptyBorder(15, 15, 15, 15)
  box.add(Box.createVerticalGlue())
  box.add(button)

  val listener = HierarchyListener { e ->
    val changed = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (changed && !e.component.isDisplayable) {
      println("DISPOSE_ON_CLOSE")
      worker?.cancel(true)
      worker = null
    }
  }

  return JPanel(BorderLayout()).also {
    it.addHierarchyListener(listener)
    it.add(makePanel1(m), BorderLayout.NORTH)
    it.add(makePanel2(m), BorderLayout.WEST)
    it.add(box, BorderLayout.EAST)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makePanel1(m: BoundedRangeModel): Component {
  val progress0 = JProgressBar(m)
  progress0.isStringPainted = false

  val progress1 = JProgressBar(m)
  progress1.isStringPainted = true

  val progress2 = JProgressBar(m)
  progress2.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT

  val progress3 = JProgressBar(m)
  progress3.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT
  progress3.isStringPainted = true

  val p1 = JPanel(GridLayout(2, 2, 10, 10))
  p1.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  listOf(progress0, progress1, progress2, progress3).forEach { p1.add(it) }

  return p1
}

private fun makePanel2(m: BoundedRangeModel): Component {
  val progress4 = JProgressBar(m)
  progress4.orientation = SwingConstants.VERTICAL

  val progress5 = JProgressBar(m)
  progress5.orientation = SwingConstants.VERTICAL
  progress5.isStringPainted = true

  val progress6 = JProgressBar(m)
  progress6.orientation = SwingConstants.VERTICAL
  progress6.isStringPainted = true
  progress6.componentOrientation = ComponentOrientation.LEFT_TO_RIGHT

  val progress7 = object : JProgressBar(m) {
    override fun paintComponent(g: Graphics) {
      val g2 = g.create() as? Graphics2D ?: return
      g2.scale(1.0, -1.0)
      g2.translate(0, -height)
      super.paintComponent(g2)
      g2.dispose()
    }
  }
  progress7.orientation = SwingConstants.VERTICAL
  progress7.isStringPainted = true

  val progress8 = JProgressBar(m)
  progress8.orientation = SwingConstants.VERTICAL
  val layer = JLayer<JProgressBar>(progress8, VerticalFlipLayerUI())

  val p2 = Box.createHorizontalBox()
  p2.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
  p2.add(Box.createHorizontalGlue())
  listOf<Component>(progress4, progress5, progress6, progress7, layer).forEach {
    p2.add(it)
    p2.add(Box.createHorizontalStrut(25))
  }

  return p2
}

private class BackgroundTask : SwingWorker<String, Void>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    var current = 0
    val lengthOfTask = 200
    while (current <= lengthOfTask && !isCancelled) {
      doSomething(100 * current / lengthOfTask)
      current++
    }
    return "Done"
  }

  @Throws(InterruptedException::class)
  private fun doSomething(progress: Int) {
    Thread.sleep(10)
    setProgress(progress)
  }
}

private class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.propertyName
    if ("progress" == strPropertyName) {
      progressBar.isIndeterminate = false
      val progress = e.newValue as? Int ?: 0
      progressBar.value = progress
    }
  }
}

private open class VerticalFlipLayerUI<V : Component> : LayerUI<V>() {
  @Transient private var buf: BufferedImage? = null
  override fun paint(g: Graphics, c: JComponent) {
    if (c is JLayer<*>) {
      val d = c.view.size
      val bi = buf?.takeIf { it.width == d.width && it.height == d.height }
        ?: BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
      val g2 = bi.createGraphics()
      g2.scale(1.0, -1.0)
      g2.translate(0, -d.height)
      super.paint(g2, c)
      g2.dispose()
      buf = bi
      g.drawImage(buf, 0, 0, c.view)
    } else {
      super.paint(g, c)
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
