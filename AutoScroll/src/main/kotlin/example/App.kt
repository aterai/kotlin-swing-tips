package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val label = JLabel(ImageIcon(cl.getResource("example/CRW_3857_JFR.jpg")))
  val scroll = JScrollPane(label).also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
  }

  val listener = ViewportDragScrollListener()
  scroll.viewport.also {
    it.addMouseMotionListener(listener)
    it.addMouseListener(listener)
    it.addHierarchyListener(listener)
  }

  scroll.preferredSize = Dimension(320, 240)
  return scroll
}

private class ViewportDragScrollListener : MouseAdapter(), HierarchyListener {
  private val startPt = Point()
  private val move = Point()
  private val scroller = Timer(DELAY, null)
  @Transient
  private var listener: ActionListener? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0 &&
      !e.component.isDisplayable
    ) {
      scroller.stop()
      scroller.removeActionListener(listener)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val vport = e.component as? JViewport ?: return
    val c = vport.view as? JComponent ?: return
    val pt = e.point
    val dx = startPt.x - pt.x
    val dy = startPt.y - pt.y
    val vp = vport.viewPosition
    vp.translate(dx, dy)
    c.scrollRectToVisible(Rectangle(vp, vport.size))
    move.setLocation(SPEED * dx, SPEED * dy)
    startPt.location = pt
  }

  override fun mousePressed(e: MouseEvent) {
    e.component.cursor = HC
    startPt.location = e.point
    move.setLocation(0, 0)
    scroller.stop()
    scroller.removeActionListener(listener)
  }

  override fun mouseReleased(e: MouseEvent) {
    val c = e.component
    c.cursor = DC
    val vport = c as? JViewport ?: return
    val label = vport.view as? JComponent ?: return
    listener = ActionListener {
      val vp = vport.viewPosition
      vp.translate(move.x, move.y)
      label.scrollRectToVisible(Rectangle(vp, vport.size))
    }
    scroller.addActionListener(listener)
    scroller.start()
  }

  override fun mouseExited(e: MouseEvent) {
    e.component.cursor = DC
    move.setLocation(0, 0)
    scroller.stop()
    scroller.removeActionListener(listener)
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private val DC = Cursor.getDefaultCursor()
    private val HC = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
}

// private class ComponentDragScrollListener : MouseAdapter(), HierarchyListener {
//   private val startPt = Point()
//   private val move = Point()
//   private val scroller = Timer(DELAY, null)
//   @Transient
//   private var listener: ActionListener? = null
//
//   override fun hierarchyChanged(e: HierarchyEvent) {
//     if (e.changeFlags.toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0 &&
//       !e.component.isDisplayable
//     ) {
//       scroller.stop()
//       scroller.removeActionListener(listener)
//     }
//   }
//
//   override fun mouseDragged(e: MouseEvent) {
//     scroller.stop()
//     scroller.removeActionListener(listener)
//     val jc = e.component as? JComponent ?: return
//     val vport = SwingUtilities.getAncestorOfClass(JViewport::class.java, jc) as? JViewport ?: return
//     val cp = SwingUtilities.convertPoint(jc, e.point, vport)
//     val dx = startPt.x - cp.x
//     val dy = startPt.y - cp.y
//     val vp = vport.viewPosition
//     vp.translate(dx, dy)
//     jc.scrollRectToVisible(Rectangle(vp, vport.size))
//     move.setLocation(SPEED * dx, SPEED * dy)
//     startPt.location = cp
//   }
//
//   override fun mousePressed(e: MouseEvent) {
//     scroller.stop()
//     scroller.removeActionListener(listener)
//     move.setLocation(0, 0)
//     val c = e.component
//     c.cursor = HC
//     (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
//       startPt.location = SwingUtilities.convertPoint(c, e.point, it)
//     }
//   }
//
//   override fun mouseReleased(e: MouseEvent) {
//     val c = e.component
//     c.cursor = DC
//     listener = ActionListener {
//       val vport = SwingUtilities.getUnwrappedParent(c) as? JViewport ?: return@ActionListener
//       val vp = vport.viewPosition
//       vp.translate(move.x, move.y)
//       (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, vport.size))
//     }
//     scroller.addActionListener(listener)
//     scroller.start()
//   }
//
//   override fun mouseExited(e: MouseEvent) {
//     scroller.stop()
//     scroller.removeActionListener(listener)
//     e.component.cursor = DC
//     move.setLocation(0, 0)
//   }
//
//   companion object {
//     private const val SPEED = 4
//     private const val DELAY = 10
//     private val DC = Cursor.getDefaultCursor()
//     private val HC = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
//   }
// }

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
