package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.LayerUI

private var worker: SwingWorker<String, Void>? = null

fun makeUI(): Component {
  val model = DefaultBoundedRangeModel()
  val progress01 = JProgressBar(model)
  progress01.isStringPainted = true
  val progress02 = JProgressBar(model)
  progress02.isStringPainted = true
  val progress03 = JProgressBar(model)
  progress03.isOpaque = false
  val progress04 = JProgressBar(model)
  progress04.isOpaque = true // for NimbusLookAndFeel

  val layerUI = BlockedColorLayerUI<Component>()
  val p = JPanel(GridLayout(2, 1))
  p.add(makeTitledPanel("setStringPainted(true)", progress01, progress02))
  p.add(makeTitledPanel("setStringPainted(false)", progress03, JLayer(progress04, layerUI)))

  val check = JCheckBox("Turn the progress bar red")
  check.addActionListener { e ->
    val b = (e.source as? JCheckBox)?.isSelected == true
    val color = if (b) Color(0x64_FF_00_00, true) else progress01.foreground
    progress02.foreground = color
    layerUI.isPreventing = b
    p.repaint()
  }
  val button = JButton("Start")
  button.addActionListener {
    worker?.takeUnless { it.isDone }?.cancel(true)
    worker = BackgroundTask().also {
      it.addPropertyChangeListener(ProgressListener(progress01))
      it.execute()
    }
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(check)
  box.add(Box.createHorizontalStrut(2))
  box.add(button)
  box.add(Box.createHorizontalStrut(2))

  return JPanel(BorderLayout()).also {
    it.addHierarchyListener { e ->
      val b = e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0
      if (b && !e.component.isDisplayable) {
        // println("DISPOSE_ON_CLOSE")
        worker?.cancel(true)
        worker = null
      }
    }
    it.add(p)
    it.add(box, BorderLayout.SOUTH)
    it.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTitledPanel(title: String, vararg list: Component): Component {
  val p = JPanel(GridBagLayout())
  p.border = BorderFactory.createTitledBorder(title)
  val c = GridBagConstraints()
  c.fill = GridBagConstraints.HORIZONTAL
  c.insets = Insets(5, 5, 5, 5)
  c.weightx = 1.0
  c.gridx = GridBagConstraints.REMAINDER
  list.forEach { p.add(it, c) }
  return p
}

private class BlockedColorLayerUI<V : Component> : LayerUI<V>() {
  var isPreventing = false
  private var buf: BufferedImage? = null

  override fun paint(g: Graphics, c: JComponent) {
    if (isPreventing && c is JLayer<*>) {
      val d = c.view.size
      val buffer = buf?.takeIf { it.width == d.width && it.height == d.height }
        ?: BufferedImage(d.width, d.height, BufferedImage.TYPE_INT_ARGB)
      buf = buffer
      val g2 = buffer.createGraphics()
      super.paint(g2, c)
      g2.dispose()
      val image = c.createImage(FilteredImageSource(buffer.source, RedGreenChannelSwapFilter()))
      g.drawImage(image, 0, 0, c.view)
    } else {
      super.paint(g, c)
    }
  }
}

private class RedGreenChannelSwapFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    return argb and 0xFF_00_00_FF.toInt() or (g shl 16) or (r shl 8)
  }
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
  init {
    progressBar.value = 0
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val nv = e.newValue
    if ("progress" == e.propertyName && nv is Int) {
      progressBar.isIndeterminate = false
      progressBar.value = nv
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
