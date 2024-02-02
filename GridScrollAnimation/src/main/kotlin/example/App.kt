package example

import java.awt.*
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.*
import kotlin.math.pow

fun makeUI(): Component {
  val gp = GridPanel(4, 3)
  for (i in 0 until gp.columns * gp.rows) {
    gp.add(makeSampleComponent(i))
  }

  val scrollPane = JScrollPane(gp)
  scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
  scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER

  val p = JPanel()
  p.add(scrollPane)

  return JPanel(BorderLayout()).also {
    it.add(p)
    it.add(JButton(ScrollAction("right", scrollPane, Point(1, 0))), BorderLayout.EAST)
    it.add(JButton(ScrollAction("left", scrollPane, Point(-1, 0))), BorderLayout.WEST)
    it.add(JButton(ScrollAction("bottom", scrollPane, Point(0, 1))), BorderLayout.SOUTH)
    it.add(JButton(ScrollAction("top", scrollPane, Point(0, -1))), BorderLayout.NORTH)
  }
}

private fun makeSampleComponent(idx: Int): Component =
  if (idx % 2 == 0) JButton("button$idx") else JScrollPane(JTree())

private class GridPanel(
  rows: Int,
  cols: Int,
) : JPanel(GridLayout(rows, cols, 0, 0)), Scrollable {
  private val sz = Dimension(160 * cols, 120 * rows)
  val rows get() = (layout as? GridLayout)?.rows ?: -1
  val columns get() = (layout as? GridLayout)?.columns ?: -1

  override fun getPreferredScrollableViewportSize(): Dimension {
    val d = preferredSize
    return Dimension(d.width / columns, d.height / rows)
  }

  override fun getScrollableUnitIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation == SwingConstants.HORIZONTAL) visibleRect.width else visibleRect.height

  override fun getScrollableBlockIncrement(
    visibleRect: Rectangle,
    orientation: Int,
    direction: Int,
  ) = if (orientation == SwingConstants.HORIZONTAL) visibleRect.width else visibleRect.height

  override fun getScrollableTracksViewportWidth() = false

  override fun getScrollableTracksViewportHeight() = false

  override fun getPreferredSize() = sz
}

private class ScrollAction(
  name: String?,
  private val scrollPane: JScrollPane,
  private val vec: Point,
) : AbstractAction(name) {
  private val scroller = Timer(5, null)
  private var listener: ActionListener? = null
  private var count = 0

  override fun actionPerformed(e: ActionEvent) {
    start()
  }

  private fun start() {
    val vp = scrollPane.viewport
    val v = vp.view as? JComponent
    if (scroller.isRunning || v == null) {
      return
    }
    val w = vp.width
    val h = vp.height
    val sx = vp.viewPosition.x
    val sy = vp.viewPosition.y
    val rect = Rectangle(w, h)
    scroller.removeActionListener(listener)
    count = SIZE.toInt()
    listener = ActionListener {
      val a = easeInOut(--count / SIZE)
      var dx = (w - a * w + .5).toInt()
      var dy = (h - a * h + .5).toInt()
      if (count <= 0) {
        dx = w
        dy = h
        scroller.stop()
      }
      rect.setLocation(sx + vec.x * dx, sy + vec.y * dy)
      v.scrollRectToVisible(rect)
    }
    scroller.addActionListener(listener)
    scroller.start()
  }

  private fun easeInOut(t: Double) = if (t < .5) {
    .5 * (t * 2.0).pow(3)
  } else {
    .5 * ((t * 2.0 - 2.0).pow(3) + 2.0)
  }

  companion object {
    private const val SIZE = 32.0
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
      isResizable = false
      isVisible = true
    }
  }
}
