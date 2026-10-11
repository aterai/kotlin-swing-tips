package example

import java.awt.*
import javax.swing.*
import kotlin.math.min
import kotlin.math.roundToInt

fun createUI(): Component {
  val scroll0 = JScrollPane(JList(createModel()))
  scroll0.border = BorderFactory.createTitledBorder("Default")

  val scroll1 = ScrollSmoothingPane(JList(createModel()))
  scroll1.border = BorderFactory.createTitledBorder("ScrollSmoothingPane")

  val scroll2 = PaintOffsetScrollPane(JList(createModel()))
  scroll2.border = BorderFactory.createTitledBorder("PaintOffsetScrollPane")

  return JPanel(GridLayout(1, 0)).also {
    it.add(scroll0)
    it.add(scroll1)
    it.add(scroll2)
    it.preferredSize = Dimension(320, 240)
  }
}

private fun createModel(): ListModel<String> {
  val model = DefaultListModel<String>()
  (0..<5000).map { it.toString() }.forEach { model.addElement(it) }
  return model
}

private class ScrollSmoothingPane(
  view: Component,
) : JScrollPane(view) {
  override fun createViewport(): JViewport = SmoothViewport()
}

// Approach 1: delay the view position itself. Because JScrollPane keeps the
// scrollbar and the viewport in sync, every intermediate position painted here
// comes back as a new request and must be filtered out.
private class SmoothViewport : JViewport() {
  private val animator = Timer(FRAME_INTERVAL_MS, null)
  private val from = Point(UNSET_XY, UNSET_XY)
  private val target = Point(UNSET_XY, UNSET_XY)
  private var startTime = 0L
  private var adjusting = false

  init {
    animator.addActionListener { animate() }
  }

  override fun setViewPosition(p: Point) {
    // Ignore the echo of an intermediate position this viewport itself has just
    // painted: JScrollPane synchronizes the scrollbar with the viewport, and the
    // scrollbar then requests that same intermediate position back, which would
    // otherwise overwrite the animation target.
    val viewPosition = super.getViewPosition()
    val echo = adjusting || animator.isRunning && viewPosition == p
    if (!echo && target != p) {
      from.location = viewPosition
      target.location = p
      startTime = System.currentTimeMillis()
      animator.start() // Timer#start() is ignored while the Timer is running
    }
  }

  private fun animate() {
    val time = System.currentTimeMillis() - startTime
    val t = min(Easing.COMPLETE, time / DURATION_MS.toDouble())
    val e = Easing.easeOutCubic(t)
    val x = (from.x + (target.x - from.x) * e).roundToInt()
    val y = (from.y + (target.y - from.y) * e).roundToInt()
    adjusting = true
    try {
      super.setViewPosition(Point(x, y))
    } finally {
      adjusting = false
    }
    if (t >= Easing.COMPLETE) {
      animator.stop()
    }
  }

  companion object {
    private const val DURATION_MS = 200
    private const val FRAME_INTERVAL_MS = 1000 / 60

    // A position that JScrollPane never requests, so the first scroll is not
    // mistaken for a repeat of the current target.
    private const val UNSET_XY = -100
  }
}

private class PaintOffsetScrollPane(
  view: Component,
) : JScrollPane(view) {
  override fun createViewport(): JViewport = PaintOffsetViewport()
}

// Approach 2: keep the view position always up to date and delay only the
// painting. A scroll request is immediately applied to the logical position and
// canceled out by an equal Graphics#translate(...) offset, which then decays to
// zero. Swing never sees a stale position, so no feedback filtering is needed.
private class PaintOffsetViewport : JViewport() {
  private val animator = Timer(FRAME_INTERVAL_MS, null)
  private var offsetX = 0.0
  private var offsetY = 0.0
  private var startOffsetX = 0.0
  private var startOffsetY = 0.0
  private var startTime = 0L

  init {
    animator.addActionListener { animate() }
  }

  // BLIT_SCROLL_MODE and BACKINGSTORE_SCROLL_MODE copy pixels on the
  // assumption that the view is painted without any offset.
  override fun getScrollMode() = SIMPLE_SCROLL_MODE

  // While the offset is decaying, a repaint requested by the view itself
  // (e.g. a selection change in the JList) must also go through paint(...)
  // below, otherwise the view is painted directly without the offset.
  override fun isPaintingOrigin() = animator.isRunning || super.isPaintingOrigin()

  override fun setViewPosition(p: Point) {
    val prev = viewPosition
    super.setViewPosition(p)
    val next = viewPosition
    val dx = next.x - prev.x
    val dy = next.y - prev.y
    if (dx != 0 || dy != 0) {
      // Cancel out the jump that has just been applied to the logical position.
      offsetX += dx
      offsetY += dy
      startOffsetX = offsetX
      startOffsetY = offsetY
      startTime = System.currentTimeMillis()
      if (!animator.isRunning) {
        animator.start()
      }
      repaint()
    }
  }

  private fun animate() {
    val time = System.currentTimeMillis() - startTime
    val t = min(Easing.COMPLETE, time / DURATION_MS.toDouble())
    val e = Easing.COMPLETE - Easing.easeOutCubic(t)
    offsetX = startOffsetX * e
    offsetY = startOffsetY * e
    if (t >= Easing.COMPLETE) {
      offsetX = 0.0
      offsetY = 0.0
      animator.stop()
    }
    repaint()
  }

  override fun paint(g: Graphics) {
    val ox = offsetX.roundToInt()
    val oy = offsetY.roundToInt()
    if (ox == 0 && oy == 0) {
      super.paint(g)
    } else {
      // Both ends of the view are shifted away from the viewport bounds, so the
      // background must be filled before translating.
      g.color = view?.background ?: background
      g.fillRect(0, 0, width, height)
      val g2 = g.create()
      g2.translate(ox, oy)
      super.paint(g2)
      g2.dispose()
    }
  }

  companion object {
    private const val DURATION_MS = 200
    private const val FRAME_INTERVAL_MS = 1000 / 60
  }
}

private object Easing {
  const val COMPLETE = 1.0

  fun easeOutCubic(t: Double): Double {
    val u = COMPLETE - t
    return COMPLETE - u * u * u
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
