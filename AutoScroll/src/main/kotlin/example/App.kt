package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.ActionListener
import java.awt.event.HierarchyEvent
import java.awt.event.HierarchyListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports

class MainPanel : JPanel(BorderLayout()) {
  init {
    val label = JLabel(ImageIcon(javaClass.getResource("CRW_3857_JFR.jpg")))
    val scroll = JScrollPane(label).also {
      it.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER)
      it.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
    }

    val listener = ViewportDragScrollListener()
    scroll.getViewport().also {
      it.addMouseMotionListener(listener)
      it.addMouseListener(listener)
      it.addHierarchyListener(listener)
    }

    add(scroll)
    scroll.setPreferredSize(Dimension(320, 240))
  }
}

internal class ViewportDragScrollListener : MouseAdapter(), HierarchyListener {
  private val startPt = Point()
  private val move = Point()
  private val scroller = Timer(DELAY, null)
  @Transient
  private var listener: ActionListener? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.getChangeFlags().toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0 &&
        !e.getComponent().isDisplayable()) {
      scroller.stop()
      scroller.removeActionListener(listener)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val vport = e.getComponent() as JViewport
    val c = vport.getView() as JComponent
    val pt = e.getPoint()
    val dx = startPt.x - pt.x
    val dy = startPt.y - pt.y
    val vp = vport.getViewPosition()
    vp.translate(dx, dy)
    c.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    move.setLocation(SPEED * dx, SPEED * dy)
    startPt.setLocation(pt)
  }

  override fun mousePressed(e: MouseEvent) {
    e.getComponent().setCursor(HC)
    startPt.setLocation(e.getPoint())
    move.setLocation(0, 0)
    scroller.stop()
    scroller.removeActionListener(listener)
  }

  override fun mouseReleased(e: MouseEvent) {
    val c = e.getComponent()
    c.setCursor(DC)
    val vport = c as? JViewport ?: return
    val label = vport.getView() as JComponent
    listener = ActionListener {
      val vp = vport.getViewPosition()
      vp.translate(move.x, move.y)
      label.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    }
    scroller.addActionListener(listener)
    scroller.start()
  }

  override fun mouseExited(e: MouseEvent) {
    e.getComponent().setCursor(DC)
    move.setLocation(0, 0)
    scroller.stop()
    scroller.removeActionListener(listener)
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private val DC = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    private val HC = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
}

internal class ComponentDragScrollListener : MouseAdapter(), HierarchyListener {
  private val startPt = Point()
  private val move = Point()
  private val scroller = Timer(DELAY, null)
  @Transient
  private var listener: ActionListener? = null

  override fun hierarchyChanged(e: HierarchyEvent) {
    if (e.getChangeFlags().toInt() and HierarchyEvent.DISPLAYABILITY_CHANGED != 0 &&
        !e.getComponent().isDisplayable()) {
      scroller.stop()
      scroller.removeActionListener(listener)
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    scroller.stop()
    scroller.removeActionListener(listener)
    val jc = e.getComponent() as JComponent
    val vport = SwingUtilities.getAncestorOfClass(JViewport::class.java, jc) as? JViewport ?: return
    val cp = SwingUtilities.convertPoint(jc, e.getPoint(), vport)
    val dx = startPt.x - cp.x
    val dy = startPt.y - cp.y
    val vp = vport.getViewPosition()
    vp.translate(dx, dy)
    jc.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    move.setLocation(SPEED * dx, SPEED * dy)
    startPt.setLocation(cp)
  }

  override fun mousePressed(e: MouseEvent) {
    scroller.stop()
    scroller.removeActionListener(listener)
    move.setLocation(0, 0)
    val c = e.getComponent()
    c.setCursor(HC)
    (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
      startPt.setLocation(SwingUtilities.convertPoint(c, e.getPoint(), it))
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val c = e.getComponent()
    c.setCursor(DC)
    listener = ActionListener {
      val vport = SwingUtilities.getUnwrappedParent(c) as? JViewport ?: return@ActionListener
      val vp = vport.getViewPosition()
      vp.translate(move.x, move.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, vport.getSize()))
    }
    scroller.addActionListener(listener)
    scroller.start()
  }

  override fun mouseExited(e: MouseEvent) {
    scroller.stop()
    scroller.removeActionListener(listener)
    e.getComponent().setCursor(DC)
    move.setLocation(0, 0)
  }

  companion object {
    private const val SPEED = 4
    private const val DELAY = 10
    private val DC = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
    private val HC = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  }
}

fun main() {
  EventQueue.invokeLater {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())
    } catch (ex: ClassNotFoundException) {
      ex.printStackTrace()
    } catch (ex: InstantiationException) {
      ex.printStackTrace()
    } catch (ex: IllegalAccessException) {
      ex.printStackTrace()
    } catch (ex: UnsupportedLookAndFeelException) {
      ex.printStackTrace()
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
