package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import kotlin.math.abs

fun makeUI(): Component {
  val scroll = JScrollPane().also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }

  // val heavyweightLightweightMixing = false
  // val viewport = scroll.getViewport() // Java 6
  val viewport = object : JViewport() { // Java 7
    // private val HEAVYWEIGHT_LIGHTWEIGHT_MIXING = false
    private var adjusting = false
    override fun revalidate() {
      // if (!heavyweightLightweightMixing && adjusting) {
      if (adjusting) {
        return
      }
      super.revalidate()
    }

    override fun setViewPosition(p: Point) {
      // if (heavyweightLightweightMixing) {
      //   super.setViewPosition(p)
      // } else {
      adjusting = true
      super.setViewPosition(p)
      adjusting = false
    }
  }
  scroll.viewport = viewport

  val cl = Thread.currentThread().contextClassLoader
  val label = JLabel(ImageIcon(cl.getResource("example/CRW_3857_JFR.jpg")))
  viewport.add(label)
  val l1 = KineticScrollingListener1(label)
  val l2 = KineticScrollingListener2(label)

  val r1 = JRadioButton("scrollRectToVisible", true)
  r1.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      viewport.removeMouseListener(l2)
      viewport.removeMouseMotionListener(l2)
      viewport.removeHierarchyListener(l2)
      viewport.addMouseMotionListener(l1)
      viewport.addMouseListener(l1)
      viewport.addHierarchyListener(l1)
    }
  }

  val r2 = JRadioButton("setViewPosition")
  r2.addItemListener { e ->
    if (e.stateChange == ItemEvent.SELECTED) {
      viewport.removeMouseListener(l1)
      viewport.removeMouseMotionListener(l1)
      viewport.removeHierarchyListener(l1)
      viewport.addMouseMotionListener(l2)
      viewport.addMouseListener(l2)
      viewport.addHierarchyListener(l2)
    }
  }

  val box = Box.createHorizontalBox()
  val bg = ButtonGroup()
  listOf(r1, r2).forEach {
    box.add(it)
    bg.add(it)
  }

  viewport.addMouseMotionListener(l1)
  viewport.addMouseListener(l1)
  viewport.addHierarchyListener(l1)

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(box, BorderLayout.NORTH)
    scroll.preferredSize = Dimension(320, 240)
  }
}

private class KineticScrollingListener1(private val label: JComponent) : MouseAdapter(), HierarchyListener {
  private val dc = label.cursor
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scroller: Timer
  private val startPt = Point()
  private val delta = Point()

  init {
    this.scroller = Timer(DELAY) { e ->
      val src = e.source
      val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return@Timer
      val vp = vport.viewPosition
      vp.translate(-delta.x, -delta.y)
      label.scrollRectToVisible(Rectangle(vp, vport.size))
      if (abs(delta.x) > 0 || abs(delta.y) > 0) {
        delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      } else if (src is Timer) {
        src.stop()
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hc
    startPt.location = e.point
    scroller.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val vport = e.component as? JViewport ?: return
    val vp = vport.viewPosition // = SwingUtilities.convertPoint(vport, 0, 0, label)
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    label.scrollRectToVisible(Rectangle(vp, vport.size))
    startPt.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = dc
    scroller.start()
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      scroller.stop()
    }
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private const val D = .8
  }
}

private class KineticScrollingListener2(private val label: JComponent) : MouseAdapter(), HierarchyListener {
  private val dc = label.cursor
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val startPt = Point()
  private val delta = Point()
  private val inside = Timer(DELAY) { e ->
    val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return@Timer
    val vp = vport.viewPosition
    vp.translate(-delta.x, -delta.y)
    vport.viewPosition = vp
    if (abs(delta.x) > 0 || abs(delta.y) > 0) {
      delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      // Outside
      if (vp.x < 0 || vp.x + vport.width - label.width > 0) {
        delta.x = (delta.x * D).toInt()
      }
      if (vp.y < 0 || vp.y + vport.height - label.height > 0) {
        delta.y = (delta.y * D).toInt()
      }
    } else {
      (e.source as? Timer)?.stop() // inside.stop()
      if (!isInside(vport, label)) {
        outside.start()
      }
    }
  }
  private val outside = Timer(DELAY) { e ->
    val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return@Timer
    val vp = vport.viewPosition
    if (vp.x < 0) {
      vp.x = (vp.x * D).toInt()
    }
    if (vp.y < 0) {
      vp.y = (vp.y * D).toInt()
    }
    if (vp.x + vport.width - label.width > 0) {
      vp.x = (vp.x - (vp.x + vport.width - label.width) * (1.0 - D)).toInt()
    }
    if (vp.y + vport.height > label.height) {
      vp.y = (vp.y - (vp.y + vport.height - label.height) * (1.0 - D)).toInt()
    }
    vport.viewPosition = vp
    if (isInside(vport, label)) {
      (e.source as? Timer)?.stop() // outside.stop()
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = hc
    startPt.location = e.point
    inside.stop()
    outside.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.point
    val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return
    val vp = vport.viewPosition
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    vport.viewPosition = vp
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    startPt.location = pt
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = dc
    val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return
    if (isInside(vport, label)) {
      inside.start()
    } else {
      outside.start()
    }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.changeFlags.toInt() and mask != 0 && !e.component.isDisplayable) {
      inside.stop()
      outside.stop()
    }
  }

  private fun isInside(vport: JViewport, comp: JComponent) = vport.viewPosition.let {
    val ww = it.x >= 0 && it.x + vport.width - comp.width <= 0
    val hh = it.y >= 0 && it.y + vport.height - comp.height <= 0
    ww && hh
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private const val D = .8
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
