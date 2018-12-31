package example

import java.awt.*
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.Arrays
import java.util.Objects
import javax.swing.*
import javax.swing.plaf.basic.BasicProgressBarUI

class MainPanel : JPanel(BorderLayout()) {
  init {
    val progress = object : JProgressBar() {
      override fun updateUI() {
        super.updateUI()
        setUI(OneDirectionIndeterminateProgressBarUI())
      }
    }
    // TEST: progress.setComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);

    val list = Arrays.asList(JProgressBar(), progress)

    val p = JPanel(GridLayout(2, 1))
    list.forEach({ bar -> p.add(makePanel(bar)) })

    val button = JButton("Start")
    button.addActionListener({
      val worker: SwingWorker<String, Void> = BackgroundTask()
      list.forEach({ bar ->
        bar.setIndeterminate(true)
        worker.addPropertyChangeListener(ProgressListener(bar))
      })
      worker.execute()
    })

    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(button)
    box.add(Box.createHorizontalStrut(5))

    add(p)
    add(box, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
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
}

internal class OneDirectionIndeterminateProgressBarUI : BasicProgressBarUI() {
  // @see com/sun/java/swing/plaf/windows/WindowsProgressBarUI.java
  override protected fun getBox(r: Rectangle): Rectangle {
    val rect = super.getBox(r)
    val framecount = getFrameCount() / 2
    val currentFrame = getAnimationIndex() % framecount

    if (progressBar.getOrientation() == JProgressBar.VERTICAL) {
      var len = progressBar.getHeight()
      len += rect.height * 2 // add 2x for the trails
      val delta = len / framecount.toDouble()
      rect.y = (delta * currentFrame).toInt()
    } else {
      var len = progressBar.getWidth()
      len += rect.width * 2 // add 2x for the trails
      val delta = len / framecount.toDouble()
      rect.x = (delta * currentFrame).toInt()
    }
    return rect
  }
}

class BackgroundTask : SwingWorker<String, Void>() {
  override fun doInBackground(): String {
    try { // dummy task
      Thread.sleep(5000)
    } catch (ex: InterruptedException) {
      return "Interrupted"
    }

    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled()) {
      try { // dummy task
        Thread.sleep(50)
      } catch (ex: InterruptedException) {
        return "Interrupted"
      }
      setProgress(100 * current / lengthOfTask)
      current++
    }
    return "Done"
  }
}

internal class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    this.progressBar.setValue(0)
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val strPropertyName = e.getPropertyName()
    if ("progress".equals(strPropertyName)) {
      progressBar.setIndeterminate(false)
      val progress = e.getNewValue() as Int
      progressBar.setValue(progress)
    }
  }
}

fun main() {
  EventQueue.invokeLater({
    JFrame().apply {
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  })
}
