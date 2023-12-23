package example

import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel
import kotlin.math.abs

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val model = object : DefaultTableModel(columnNames, 0) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  for (i in 0 until 1000) {
    model.addRow(arrayOf("12345", i, i % 2 == 0))
  }

  val table = object : JTable(model) {
    private var handler: TableTouchScreenHandler? = null

    override fun updateUI() {
      removeMouseMotionListener(handler)
      removeMouseListener(handler)
      getSelectionModel().removeListSelectionListener(handler)
      super.updateUI()
      handler = TableTouchScreenHandler(this)
      addMouseMotionListener(handler)
      addMouseListener(handler)
      getSelectionModel().addListSelectionListener(handler)
      setRowHeight(30)
    }
  }
  UIManager.put("ScrollBar.width", 30)
  val scroll = JScrollPane(table)
  scroll.preferredSize = Dimension(320, 240)
  return scroll
}

private class TableTouchScreenHandler(table: JTable) : MouseAdapter(), ListSelectionListener {
  private val dc = Cursor.getDefaultCursor()
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scroller = Timer(DELAY) { e ->
    print(".")
    (SwingUtilities.getUnwrappedParent(table) as? JViewport)?.also {
      val vp = it.viewPosition
      vp.translate(-delta.x, -delta.y)
      table.scrollRectToVisible(Rectangle(vp, it.size))
      if (abs(delta.x) > 0 || abs(delta.y) > 0) {
        delta.setLocation((delta.x * GRAVITY).toInt(), (delta.y * GRAVITY).toInt())
      } else {
        (e.source as? Timer)?.stop()
      }
    }
  }
  private val startPt = Point()
  private val delta = Point()

  override fun mousePressed(e: MouseEvent) {
    // println("mousePressed: $delta")
    val c = e.component
    c.cursor = hc
    // table.isEnabled = false
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      startPt.location = SwingUtilities.convertPoint(c, e.point, p)
      scroller.stop()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.component
    val p = SwingUtilities.getUnwrappedParent(c)
    (p as? JViewport)?.also {
      val cp = SwingUtilities.convertPoint(c, e.point, it)
      val vp = it.viewPosition
      vp.translate(startPt.x - cp.x, startPt.y - cp.y)
      delta.setLocation(VELOCITY * (cp.x - startPt.x), VELOCITY * (cp.y - startPt.y))
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, it.size))
      startPt.location = cp
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    // println("mouseReleased: $delta")
    val table = e.component as? JTable ?: return
    table.cursor = dc
    // table.isEnabled = true
    if (table.isEditing) {
      delta.setLocation(0, 0)
    } else {
      scroller.start()
    }
  }

  override fun valueChanged(e: ListSelectionEvent) {
    // println("\nvalueChanged: " + e.valueIsAdjusting)
    if (scroller.isRunning) {
      // println("isRunning")
      delta.setLocation(0, 0)
    }
    scroller.stop()
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
