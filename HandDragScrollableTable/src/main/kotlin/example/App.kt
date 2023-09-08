package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.table.DefaultTableModel
import kotlin.math.abs

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val model = object : DefaultTableModel(null, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  for (i in 0 until 1000) {
    model.addRow(arrayOf("Java Swing", i, i % 2 == 0))
  }
  val table = object : JTable(model) {
    private var handler: MouseAdapter? = null

    override fun updateUI() {
      removeMouseMotionListener(handler)
      removeMouseListener(handler)
      super.updateUI()
      handler = DragScrollingListener(this)
      addMouseMotionListener(handler)
      addMouseListener(handler)
    }

    override fun isCellEditable(row: Int, column: Int) = false
  }
  return JScrollPane(table).also {
    it.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER
    it.preferredSize = Dimension(320, 240)
  }
}

private class DragScrollingListener(c: JComponent) : MouseAdapter() {
  private val dc = Cursor.getDefaultCursor()
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scroller: Timer
  private val startPt = Point()
  private val delta = Point()

  init {
    scroller = Timer(DELAY) { e ->
      (SwingUtilities.getUnwrappedParent(c) as? JViewport)?.also {
        val vp = it.viewPosition
        vp.translate(-delta.x, -delta.y)
        c.scrollRectToVisible(Rectangle(vp, it.size))
        if (abs(delta.x) > 0 || abs(delta.y) > 0) {
          delta.setLocation(
            (delta.x * GRAVITY).toInt(),
            (delta.y * GRAVITY).toInt(),
          )
        } else {
          (e.source as? Timer)?.stop()
        }
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    val c = e.component
    c.cursor = hc
    c.isEnabled = false
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      startPt.location = SwingUtilities.convertPoint(c, e.point, p)
      scroller.stop()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val viewPort = SwingUtilities.getUnwrappedParent(c)
    if (viewPort is JViewport) {
      val cp = SwingUtilities.convertPoint(c, e.point, viewPort)
      val vp = viewPort.viewPosition
      vp.translate(startPt.x - cp.x, startPt.y - cp.y)
      delta.setLocation(VELOCITY * (cp.x - startPt.x), VELOCITY * (cp.y - startPt.y))
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, viewPort.size))
      startPt.location = cp
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    val c = e.component
    c.cursor = dc
    c.isEnabled = true
    scroller.start()
  }

  companion object {
    const val VELOCITY = 5
    const val DELAY = 10
    const val GRAVITY = .95
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
