package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.* // ktlint-disable no-wildcard-imports
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

    val list = listOf(JProgressBar(), progress)

    val p = JPanel(GridLayout(2, 1))
    list.forEach { p.add(makePanel(it)) }

    val button = JButton("Start")
    button.addActionListener {
      val worker = BackgroundTask()
      list.forEach {
        it.setIndeterminate(true)
        worker.addPropertyChangeListener(ProgressListener(it))
      }
      worker.execute()
    }

    val box = Box.createHorizontalBox()
    box.add(Box.createHorizontalGlue())
    box.add(button)
    box.add(Box.createHorizontalStrut(5))

    add(p)
    add(box, BorderLayout.SOUTH)
    setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5))
    setPreferredSize(Dimension(320, 240))
  }

  private fun makePanel(cmp: Component) = JPanel(GridBagLayout()).also {
    val c = GridBagConstraints()
    c.fill = GridBagConstraints.HORIZONTAL
    c.insets = Insets(5, 5, 5, 5)
    c.weightx = 1.0
    it.add(cmp, c)
  }
}

class OneDirectionIndeterminateProgressBarUI : BasicProgressBarUI() {
  // @see com/sun/java/swing/plaf/windows/WindowsProgressBarUI.java
  override fun getBox(r: Rectangle): Rectangle {
    val rect = super.getBox(r)
    val frameCount = getFrameCount() / 2
    val currentFrame = getAnimationIndex() % frameCount

    if (progressBar.getOrientation() == JProgressBar.VERTICAL) {
      var len = progressBar.getHeight()
      len += rect.height * 2 // add 2x for the trails
      val delta = len / frameCount.toDouble()
      rect.y = (delta * currentFrame).toInt()
    } else {
      var len = progressBar.getWidth()
      len += rect.width * 2 // add 2x for the trails
      val delta = len / frameCount.toDouble()
      rect.x = (delta * currentFrame).toInt()
    }
    return rect
  }
}

class BackgroundTask : SwingWorker<String, Unit>() {
  @Throws(InterruptedException::class)
  override fun doInBackground(): String {
    Thread.sleep(5000)
    var current = 0
    val lengthOfTask = 100
    while (current <= lengthOfTask && !isCancelled()) {
      Thread.sleep(50)
      setProgress(100 * current / lengthOfTask)
      current++
    }
    return "Done"
  }
}

class ProgressListener(private val progressBar: JProgressBar) : PropertyChangeListener {
  init {
    this.progressBar.setValue(0)
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    val nv = e.getNewValue()
    if (e.getPropertyName() == "progress" && nv is Int) {
      progressBar.setIndeterminate(false)
      progressBar.setValue(nv)
    }
  }
}

fun main() {
  EventQueue.invokeLater {
    JFrame().apply {
      defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
      contentPane.add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
