package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.MouseEvent
import java.time.LocalDateTime
import java.time.ZoneId
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.Timer
import javax.swing.plaf.LayerUI
import javax.swing.plaf.basic.BasicScrollBarUI

private const val MIN_WIDTH = 6
private val scrollBar = JPanel()
private val expand = Timer(10) { scrollBar.revalidate() }
private val collapse = Timer(10) { scrollBar.revalidate() }

fun makeUI() = JPanel(GridLayout(1, 2)).also {
  it.add(makeScrollBarOnHoverScrollPane())
  it.add(JLayer(makeTranslucentScrollBar(makeList()), ScrollBarOnHoverLayerUI()))
  it.preferredSize = Dimension(320, 240)
}

private fun makeScrollBarOnHoverScrollPane(): Component {
  val scroll = JScrollPane(makeList())
  scroll.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  scrollBar.layout = HoverLayout()
  scrollBar.add(scroll.verticalScrollBar)
  val wrap = JPanel(BorderLayout())
  wrap.add(scrollBar, BorderLayout.EAST)
  wrap.add(scroll)
  return JLayer(wrap, HoverLayer())
}

private fun makeList(): Component {
  val m = DefaultListModel<String>()
  (0 until 50).map {
    "%05d: %s".format(it, LocalDateTime.now(ZoneId.systemDefault()))
  }.forEach { m.addElement(it) }
  return JList(m)
}

private fun makeTranslucentScrollBar(c: Component) = object : JScrollPane(c) {
  override fun isOptimizedDrawingEnabled() = false // JScrollBar is overlap

  override fun updateUI() {
    super.updateUI()
    EventQueue.invokeLater {
      getVerticalScrollBar().ui = TranslucentScrollBarUI()
      setComponentZOrder(getVerticalScrollBar(), 0)
      setComponentZOrder(getViewport(), 1)
      getVerticalScrollBar().isOpaque = false
      getVerticalScrollBar().preferredSize = Dimension(6, 0)
    }
    setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS)
    setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    layout = TranslucentScrollPaneLayout()
  }
}

private class HoverLayout : BorderLayout(0, 0) {
  private var controlsWidth = MIN_WIDTH

  override fun preferredLayoutSize(target: Container): Dimension {
    val ps = super.preferredLayoutSize(target)
    val barInitWidth = ps.width
    if (expand.isRunning && scrollBar.width < barInitWidth) {
      controlsWidth += 1
      if (controlsWidth >= barInitWidth) {
        controlsWidth = barInitWidth
        expand.stop()
      }
    } else if (collapse.isRunning && scrollBar.width > MIN_WIDTH) {
      controlsWidth -= 1
      if (controlsWidth <= MIN_WIDTH) {
        controlsWidth = MIN_WIDTH
        collapse.stop()
      }
    }
    ps.width = controlsWidth
    return ps
  }
}

private class HoverLayer : LayerUI<JPanel>() {
  private var isDragging = false
  override fun installUI(c: JComponent) {
    super.installUI(c)
    if (c is JLayer<*>) {
      c.layerEventMask = AWTEvent.MOUSE_EVENT_MASK or AWTEvent.MOUSE_MOTION_EVENT_MASK
    }
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseMotionEvent(e: MouseEvent, l: JLayer<out JPanel>) {
    val id = e.id
    val c = e.component
    if (c is JScrollBar && id == MouseEvent.MOUSE_DRAGGED) {
      isDragging = true
    }
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JPanel>) {
    if (e.component is JScrollBar) {
      when (e.id) {
        MouseEvent.MOUSE_ENTERED -> expandStart(isDragging)
        MouseEvent.MOUSE_EXITED -> collapseStart(isDragging)
        MouseEvent.MOUSE_RELEASED -> {
          isDragging = false
          collapseStart(!e.component.bounds.contains(e.point))
        }
      }
      l.view.repaint()
    }
  }

  private fun expandStart(dragging: Boolean) {
    if (!expand.isRunning && !dragging) {
      expand.initialDelay = 0
      expand.start()
    }
  }

  private fun collapseStart(dragging: Boolean) {
    if (!collapse.isRunning && !dragging) {
      collapse.initialDelay = 500
      collapse.start()
    }
  }
}

private class TranslucentScrollPaneLayout : ScrollPaneLayout() {
  override fun layoutContainer(parent: Container) {
    if (parent is JScrollPane) {
      val availR = SwingUtilities.calculateInnerArea(parent, null)
      viewport?.bounds = availR
      vsb?.also {
        it.setLocation(availR.x + availR.width - BAR_SIZE, availR.y)
        it.setSize(BAR_SIZE, availR.height)
        vsb.isVisible = true
      }
    }
  }

  companion object {
    private const val BAR_SIZE = 12
  }
}

private class ZeroSizeButton : JButton() {
  override fun getPreferredSize() = Dimension()
}

private class TranslucentScrollBarUI : BasicScrollBarUI() {
  override fun createDecreaseButton(orientation: Int) = ZeroSizeButton()

  override fun createIncreaseButton(orientation: Int) = ZeroSizeButton()

  override fun paintTrack(g: Graphics, c: JComponent, r: Rectangle) {
    // val g2 = g.create() as? Graphics2D ?: return
    // g2.setPaint(Color(100, 100, 100, 100))
    // g2.fillRect(r.x, r.y, r.width - 1, r.height - 1)
    // g2.dispose()
  }

  override fun paintThumb(g: Graphics, c: JComponent, r: Rectangle) {
    val sb = c as? JScrollBar
    val color: Color
    if (sb == null || !sb.isEnabled || r.width > r.height) {
      return
    } else if (isDragging) {
      color = DRAGGING_COLOR
    } else if (isThumbRollover) {
      color = ROLLOVER_COLOR
    } else {
      color = DEFAULT_COLOR
      val dw = r.width - sb.preferredSize.width
      r.x += dw
      r.width -= dw
    }
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    g2.paint = color
    g2.fillRect(r.x, r.y, r.width - 2, r.height - 1)
    g2.dispose()
  }

  companion object {
    const val MAX_WIDTH = 12
    const val MIN_WIDTH = 6
    private val DEFAULT_COLOR = Color(100, 100, 100, 190)
    private val DRAGGING_COLOR = Color(100, 100, 100, 220)
    private val ROLLOVER_COLOR = Color(100, 100, 100, 220)
  }
}

private class ScrollBarOnHoverLayerUI : LayerUI<JScrollPane>() {
  private val timer = Timer(2000, null)
  private var listener: ActionListener? = null

  override fun installUI(c: JComponent) {
    super.installUI(c)
    (c as? JLayer<*>)?.layerEventMask = AWTEvent.MOUSE_EVENT_MASK
  }

  override fun uninstallUI(c: JComponent) {
    (c as? JLayer<*>)?.layerEventMask = 0
    super.uninstallUI(c)
  }

  override fun processMouseEvent(e: MouseEvent, l: JLayer<out JScrollPane>) {
    val c = e.component as? JScrollBar ?: return
    when (e.id) {
      MouseEvent.MOUSE_ENTERED ->
        c.preferredSize = Dimension(TranslucentScrollBarUI.MAX_WIDTH, 0)
      MouseEvent.MOUSE_EXITED -> {
        timer.removeActionListener(listener)
        listener = ActionListener {
          c.preferredSize = Dimension(TranslucentScrollBarUI.MIN_WIDTH, 0)
          l.view.revalidate()
          l.view.repaint()
        }
        timer.addActionListener(listener)
        timer.isRepeats = false
        timer.start()
      }
    }
    l.view.repaint()
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
