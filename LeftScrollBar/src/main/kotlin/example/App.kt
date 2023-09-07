package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*

fun makeUI(): Component {
  val cl = Thread.currentThread().contextClassLoader
  val label = object : JLabel(ImageIcon(cl.getResource("example/CRW_3857_JFR.jpg"))) {
    @Transient private var listener: MouseAdapter? = null

    override fun updateUI() {
      removeMouseMotionListener(listener)
      removeMouseListener(listener)
      super.updateUI()
      listener = DragScrollListener()
      addMouseMotionListener(listener)
      addMouseListener(listener)
    }
  }

  val scroll = JScrollPane(label)
  scroll.componentOrientation = ComponentOrientation.RIGHT_TO_LEFT

  val p = JPanel(BorderLayout())
  p.add(Box.createHorizontalStrut(scroll.verticalScrollBar.preferredSize.width), BorderLayout.WEST)
  p.add(scroll.horizontalScrollBar)

  return JPanel(BorderLayout()).also {
    it.add(p, BorderLayout.NORTH)
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DragScrollListener : MouseAdapter() {
  private val defCursor = Cursor.getDefaultCursor()
  private val hndCursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val pp = Point()

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val viewPort = SwingUtilities.getUnwrappedParent(c)
    if (viewPort is JViewport) {
      val cp = SwingUtilities.convertPoint(c, e.point, viewPort)
      val vp = viewPort.viewPosition
      vp.translate(pp.x - cp.x, pp.y - cp.y)
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, viewPort.size))
      pp.location = cp
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hndCursor
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      val cp = SwingUtilities.convertPoint(c, e.point, p)
      pp.location = cp
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    e.component.cursor = defCursor
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
