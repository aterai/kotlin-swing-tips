package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.ListSelectionEvent
import javax.swing.event.ListSelectionListener
import javax.swing.table.DefaultTableModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val model = object : DefaultTableModel(null, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }

    for (i in 0 until 1000) {
      model.addRow(arrayOf("aaaaa", i, i % 2 == 0))
    }

    val table = object : JTable(model) {
      @Transient
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
    add(scroll)
    setPreferredSize(Dimension(320, 240))
  }
}

internal class TableTouchScreenHandler(table: JTable) : MouseAdapter(), ListSelectionListener {
  private val dc = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
  private val hc = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
  private val scroller: Timer
  private val startPt = Point()
  private val delta = Point()

  init {
    this.scroller = Timer(DELAY) { e ->
      print(".")
      val vport = SwingUtilities.getUnwrappedParent(table) as? JViewport ?: return@Timer
      val vp = vport.getViewPosition()
      vp.translate(-delta.x, -delta.y)
      table.scrollRectToVisible(Rectangle(vp, vport.getSize()))
      if (Math.abs(delta.x) > 0 || Math.abs(delta.y) > 0) {
        delta.setLocation((delta.x * GRAVITY).toInt(), (delta.y * GRAVITY).toInt())
      } else {
        (e.getSource() as? Timer)?.stop()
      }
    }
  }

  override fun mousePressed(e: MouseEvent) {
    println("mousePressed: $delta")
    val c = e.getComponent()
    c.setCursor(hc)
    // c.setEnabled(false);
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      startPt.setLocation(SwingUtilities.convertPoint(c, e.getPoint(), p))
      scroller.stop()
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    val c = e.getComponent()
    val p = SwingUtilities.getUnwrappedParent(c)
    if (p is JViewport) {
      val vport = p as JViewport
      val cp = SwingUtilities.convertPoint(c, e.getPoint(), vport)
      val vp = vport.getViewPosition()
      vp.translate(startPt.x - cp.x, startPt.y - cp.y)
      delta.setLocation(VELOCITY * (cp.x - startPt.x), VELOCITY * (cp.y - startPt.y))
      (c as? JComponent)?.scrollRectToVisible(Rectangle(vp, vport.getSize()))
      startPt.setLocation(cp)
    }
  }

  override fun mouseReleased(e: MouseEvent) {
    println("mouseReleased: $delta")
    val c = e.getComponent() as JTable
    c.setCursor(dc)
    // c.setEnabled(true);
    if (c.isEditing()) {
      delta.setLocation(0, 0)
    } else {
      scroller.start()
    }
  }

  override fun valueChanged(e: ListSelectionEvent) {
    println("\nvalueChanged: " + e.getValueIsAdjusting())
    if (scroller.isRunning()) {
      println("isRunning")
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
