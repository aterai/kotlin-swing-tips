package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.pow

fun makeUI(): Component {
  val handler = SlideInNotification()
  val easeIn = JButton("easeIn")
  easeIn.addActionListener {
    handler.startSlideIn(SlideInAnimation.EASE_IN)
  }

  val easeOut = JButton("easeOut")
  easeOut.addActionListener {
    handler.startSlideIn(SlideInAnimation.EASE_OUT)
  }

  val easeInOut = JButton("easeInOut")
  easeInOut.addActionListener {
    handler.startSlideIn(SlideInAnimation.EASE_IN_OUT)
  }

  val p = JPanel()
  p.add(easeIn)
  p.add(easeOut)
  p.add(easeInOut)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(JScrollPane(JTextArea()))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SlideInNotification : PropertyChangeListener, HierarchyListener {
  private val dialog = JWindow()
  private val animator = Timer(DELAY, null)
  private var listener: ActionListener? = null
  fun startSlideIn(slideInAnimation: SlideInAnimation) {
    if (animator.isRunning) {
      return
    }
    if (dialog.isVisible) {
      dialog.isVisible = false
      dialog.contentPane.removeAll()
    }
    val optionPane = JOptionPane("Warning", JOptionPane.WARNING_MESSAGE)
    val dwl = DragWindowListener()
    optionPane.addMouseListener(dwl)
    optionPane.addMouseMotionListener(dwl)
    optionPane.addPropertyChangeListener(this)
    optionPane.addHierarchyListener(this)
    dialog.contentPane.add(optionPane)
    dialog.pack()
    val d = dialog.contentPane.preferredSize
    val env = GraphicsEnvironment.getLocalGraphicsEnvironment()
    val desktopBounds = env.maximumWindowBounds
    val dx = desktopBounds.width - d.width
    val dy = desktopBounds.height
    dialog.location = Point(dx, dy)
    dialog.isVisible = true
    animator.removeActionListener(listener)
    val count = AtomicInteger()
    listener = ActionListener {
      val v = count.addAndGet(STEP) / d.height.toDouble()
      val a = when (slideInAnimation) {
        SlideInAnimation.EASE_IN -> AnimationUtils.easeIn(v)
        SlideInAnimation.EASE_OUT -> AnimationUtils.easeOut(v)
        else -> AnimationUtils.easeInOut(v)
      }
      var visibleHeight = (.5 + a * d.height).toInt()
      if (visibleHeight >= d.height) {
        visibleHeight = d.height
        animator.stop()
      }
      dialog.location = Point(dx, dy - visibleHeight)
    }
    animator.addActionListener(listener)
    animator.start()
  }

  override fun propertyChange(e: PropertyChangeEvent) {
    if (dialog.isVisible && JOptionPane.UNINITIALIZED_VALUE != e.newValue) {
      dialog.isVisible = false
      dialog.contentPane.removeAll()
    }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val b = e.changeFlags and HierarchyEvent.DISPLAYABILITY_CHANGED.toLong() != 0L
    if (b && !e.component.isDisplayable) {
      animator.stop()
    }
  }

  companion object {
    const val DELAY = 5
    const val STEP = 3
  }
}

private class DragWindowListener : MouseAdapter() {
  private val startPt = Point()
  override fun mousePressed(e: MouseEvent) {
    if (SwingUtilities.isLeftMouseButton(e)) {
      startPt.location = e.point
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = SwingUtilities.getRoot(e.component)
    if (c is Window && SwingUtilities.isLeftMouseButton(e)) {
      val pt = c.getLocation()
      c.setLocation(pt.x - startPt.x + e.x, pt.y - startPt.y + e.y)
    }
  }
}

private enum class SlideInAnimation {
  EASE_IN, EASE_OUT, EASE_IN_OUT
}

private object AnimationUtils {
  private const val N = 3

  fun easeIn(t: Double) = t.pow(N.toDouble())

  fun easeOut(t: Double) = (t - 1.0).pow(N.toDouble()) + 1.0

  fun easeInOut(t: Double): Double {
    val ret: Double
    val isFirstHalf = t < .5
    ret = if (isFirstHalf) {
      .5 * intPow(t * 2.0, N)
    } else {
      .5 * (intPow(t * 2.0 - 2.0, N) + 2.0)
    }
    return ret
  }

  fun intPow(da: Double, ib: Int): Double {
    var b = ib
    require(b >= 0) { "B must be a positive integer or zero" }
    var a = da
    var d = 1.0
    while (b > 0) {
      if (b and 1 != 0) {
        d *= a
      }
      a *= a
      b = b ushr 1
    }
    return d
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
