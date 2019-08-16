package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.ItemEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val scroll = JScrollPane().also {
      it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
      it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    }

    val heavyweightLightweightMixing = false
    // val viewport = scroll.getViewport() // Java 6
    val viewport = object : JViewport() { // Java 7
      // private val HEAVYWEIGHT_LIGHTWEIGHT_MIXING = false
      private var flag = false
      override fun revalidate() {
        if (!heavyweightLightweightMixing && flag) {
          return
        }
        super.revalidate()
      }

      override fun setViewPosition(p: Point) {
        if (heavyweightLightweightMixing) {
          super.setViewPosition(p)
        } else {
          flag = true
          super.setViewPosition(p)
          flag = false
        }
      }
    }
    scroll.setViewport(viewport)

    val label = JLabel(ImageIcon(javaClass.getResource("CRW_3857_JFR.jpg")))
    viewport.add(label)
    val l1 = KineticScrollingListener1(label)
    val l2 = KineticScrollingListener2(label)

    val r1 = JRadioButton("scrollRectToVisible", true)
    r1.addItemListener { e ->
      if (e.getStateChange() == ItemEvent.SELECTED) {
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
      if (e.getStateChange() == ItemEvent.SELECTED) {
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

    add(scroll)
    add(box, BorderLayout.NORTH)
    scroll.setPreferredSize(Dimension(320, 240))
  }
}

class KineticScrollingListener1(protected val label: JComponent) : MouseAdapter(), HierarchyListener {
  protected val dc: Cursor
  protected val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  protected val scroller: Timer
  protected val startPt = Point()
  protected val delta = Point()

  init {
    this.dc = label.getCursor()
    this.scroller = Timer(DELAY) { e ->
      val src = e.getSource()
      val vport = SwingUtilities.getUnwrappedParent(label) as? JViewport ?: return@Timer
      val vp = vport.getViewPosition()
      vp.translate(-delta.x, -delta.y)
      label.scrollRectToVisible(Rectangle(vp, vport.getSize()))
      if (Math.abs(delta.x) > 0 || Math.abs(delta.y) > 0) {
        delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      } else if (src is Timer) {
        src.stop()
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    e.getComponent().setCursor(hc)
    startPt.setLocation(e.getPoint())
    scroller.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.getPoint()
    val vport = e.getComponent() as JViewport // label.getParent()
    val vp = vport.getViewPosition() // = SwingUtilities.convertPoint(vport, 0, 0, label)
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    label.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    startPt.setLocation(pt)
  }

  override fun mouseReleased(e: MouseEvent) {
    e.getComponent().setCursor(dc)
    scroller.start()
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.getChangeFlags().toInt() and mask != 0 && !e.getComponent().isDisplayable()) {
      scroller.stop()
    }
  }

  companion object {
    protected const val SPEED = 4
    protected const val DELAY = 10
    protected const val D = .8
  }
}

class KineticScrollingListener2(protected val label: JComponent) : MouseAdapter(), HierarchyListener {
  protected val startPt = Point()
  protected val delta = Point()
  protected val dc: Cursor
  protected val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  protected val inside = Timer(DELAY) { e ->
    val vport = SwingUtilities.getUnwrappedParent(label) as JViewport
    val vp = vport.getViewPosition()
    vp.translate(-delta.x, -delta.y)
    vport.setViewPosition(vp)
    if (Math.abs(delta.x) > 0 || Math.abs(delta.y) > 0) {
      delta.setLocation((delta.x * D).toInt(), (delta.y * D).toInt())
      // Outside
      if (vp.x < 0 || vp.x + vport.getWidth() - label.getWidth() > 0) {
        delta.x = (delta.x * D).toInt()
      }
      if (vp.y < 0 || vp.y + vport.getHeight() - label.getHeight() > 0) {
        delta.y = (delta.y * D).toInt()
      }
    } else {
      (e.getSource() as? Timer)?.stop() // inside.stop()
      if (!isInside(vport, label)) {
        outside.start()
      }
    }
  }
  protected val outside = Timer(DELAY) { e ->
    val vport = SwingUtilities.getUnwrappedParent(label) as JViewport
    val vp = vport.getViewPosition()
    if (vp.x < 0) {
      vp.x = (vp.x * D).toInt()
    }
    if (vp.y < 0) {
      vp.y = (vp.y * D).toInt()
    }
    if (vp.x + vport.getWidth() - label.getWidth() > 0) {
      vp.x = (vp.x - (vp.x + vport.getWidth() - label.getWidth()) * (1.0 - D)).toInt()
    }
    if (vp.y + vport.getHeight() > label.getHeight()) {
      vp.y = (vp.y - (vp.y + vport.getHeight() - label.getHeight()) * (1.0 - D)).toInt()
    }
    vport.setViewPosition(vp)
    if (isInside(vport, label)) {
      (e.getSource() as? Timer)?.stop() // outside.stop()
    }
  }

  init {
    this.dc = label.getCursor()
  }

  override fun mousePressed(e: MouseEvent) {
    e.getComponent().setCursor(hc)
    startPt.setLocation(e.getPoint())
    inside.stop()
    outside.stop()
  }

  override fun mouseDragged(e: MouseEvent) {
    val pt = e.getPoint()
    val vport = SwingUtilities.getUnwrappedParent(label) as JViewport
    val vp = vport.getViewPosition()
    vp.translate(startPt.x - pt.x, startPt.y - pt.y)
    vport.setViewPosition(vp)
    delta.setLocation(SPEED * (pt.x - startPt.x), SPEED * (pt.y - startPt.y))
    startPt.setLocation(pt)
  }

  override fun mouseReleased(e: MouseEvent) {
    e.getComponent().setCursor(dc)
    val vport = SwingUtilities.getUnwrappedParent(label) as JViewport
    if (isInside(vport, label)) {
      inside.start()
    } else {
      outside.start()
    }
  }

  override fun hierarchyChanged(e: HierarchyEvent) {
    val mask = HierarchyEvent.DISPLAYABILITY_CHANGED
    if (e.getChangeFlags().toInt() and mask != 0 && !e.getComponent().isDisplayable()) {
      inside.stop()
      outside.stop()
    }
  }

  protected fun isInside(vport: JViewport, comp: JComponent) = vport.getViewPosition().let {
    val ww = it.x >= 0 && it.x + vport.getWidth() - comp.getWidth() <= 0
    val hh = it.y >= 0 && it.y + vport.getHeight() - comp.getHeight() <= 0
    ww && hh
  }

  companion object {
    protected const val SPEED = 4
    protected const val DELAY = 10
    protected const val D = .8
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
