package example

import java.awt.*
import javax.swing.*
import kotlin.math.roundToInt

fun createUI(): Component {
  val grid = GridPanel(4, 3, Dimension(160, 120))
  for (i in 0..<grid.rows * grid.columns) {
    grid.add(createSampleComponent(i))
  }

  val scroll = JScrollPane(grid)
  scroll.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val p = JPanel()
  p.add(scroll)

  // All buttons share one animator so that only one animation runs at a time
  val animator = GridScrollAnimator(scroll.viewport)
  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(createScrollButton("right", animator, 1, 0), BorderLayout.EAST)
    it.add(createScrollButton("left", animator, -1, 0), BorderLayout.WEST)
    it.add(createScrollButton("bottom", animator, 0, 1), BorderLayout.SOUTH)
    it.add(createScrollButton("top", animator, 0, -1), BorderLayout.NORTH)
  }
}

private fun createSampleComponent(idx: Int): Component =
  if (idx % 2 == 0) JButton("button$idx") else JScrollPane(JTree())

private fun createScrollButton(
  title: String,
  animator: GridScrollAnimator,
  dx: Int,
  dy: Int,
) = JButton(title).also {
  it.addActionListener { animator.scrollBy(dx, dy) }
}

private class GridPanel(
  rows: Int,
  cols: Int,
  cellSize: Dimension,
) : JPanel(GridLayout(rows, cols, 0, 0)),
  Scrollable {
  private val cellSize = Dimension(cellSize)
  val rows get() = (layout as? GridLayout)?.rows ?: -1
  val columns get() = (layout as? GridLayout)?.columns ?: -1

  override fun getPreferredSize(): Dimension {
    val w = cellSize.width * columns
    val h = cellSize.height * rows
    return Dimension(w, h)
  }

  // Show one cell of the grid at a time
  override fun getPreferredScrollableViewportSize() = Dimension(cellSize)

  override fun getScrollableUnitIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation == SwingConstants.HORIZONTAL) {
    visibleRect.width
  } else {
    visibleRect.height
  }

  override fun getScrollableBlockIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation == SwingConstants.HORIZONTAL) {
    visibleRect.width
  } else {
    visibleRect.height
  }

  override fun getScrollableTracksViewportWidth() = false

  override fun getScrollableTracksViewportHeight() = false
}

private class GridScrollAnimator(
  private val viewport: JViewport,
) {
  private val timer = Timer(5) { step() }
  private val start = Point()
  private val end = Point()
  private var count = 0

  fun scrollBy(
    dx: Int,
    dy: Int,
  ) {
    if (timer.isRunning || viewport.view == null) {
      return
    }
    val extent = viewport.extentSize
    val viewSize = viewport.viewSize
    start.location = viewport.viewPosition
    end.setLocation(
      clamp(start.x + dx * extent.width, viewSize.width - extent.width),
      clamp(start.y + dy * extent.height, viewSize.height - extent.height),
    )
    if (end != start) {
      count = 0
      timer.start()
    }
  }

  private fun step() {
    count++
    var a = easeInOut(count / STEPS.toDouble())
    if (count >= STEPS) {
      a = 1.0
      timer.stop()
    }
    val x = start.x + (a * (end.x - start.x)).roundToInt()
    val y = start.y + (a * (end.y - start.y)).roundToInt()
    viewport.viewPosition = Point(x, y)
  }

  companion object {
    private const val STEPS = 32

    private fun clamp(
      value: Int,
      max: Int,
    ) = minOf(maxOf(value, 0), max)

    // range: 0.0 <= t <= 1.0
    fun easeInOut(t: Double) = if (t < .5) {
      .5 * pow3(t * 2.0)
    } else {
      .5 * (pow3(t * 2.0 - 2.0) + 2.0)
    }

    private fun pow3(a: Double) = a * a * a
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
      isResizable = false
      isVisible = true
    }
  }
}
