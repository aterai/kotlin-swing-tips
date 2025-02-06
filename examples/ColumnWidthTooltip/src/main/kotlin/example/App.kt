package example

import java.awt.*
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }

  val table = object : JTable(model) {
    @Transient private var handler: MouseInputListener? = null

    override fun updateUI() {
      getTableHeader().removeMouseListener(handler)
      getTableHeader().removeMouseMotionListener(handler)
      super.updateUI()
      handler = ColumnWidthResizeHandler()
      getTableHeader().addMouseListener(handler)
      getTableHeader().addMouseMotionListener(handler)
    }
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class ColumnWidthResizeHandler : MouseInputAdapter() {
  private val window = JWindow()
  private val tip = JToolTip()
  private var prev = ""

  private fun getToolTipLocation(e: MouseEvent): Point {
    val p = e.point
    val c = e.component
    SwingUtilities.convertPointToScreen(p, c)
    p.translate(0, -tip.preferredSize.height)
    return p
  }

  private fun getResizingColumn(e: MouseEvent) =
    (e.component as? JTableHeader)?.resizingColumn

  private fun updateTooltipText(e: MouseEvent) {
    getResizingColumn(e)?.also {
      val txt = "Width: ${it.width}px"
      tip.tipText = txt
      if (prev.length != txt.length) {
        window.pack()
      }
      window.location = getToolTipLocation(e)
      prev = txt
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (!window.isVisible && getResizingColumn(e) != null) {
      window.add(tip)
      window.isAlwaysOnTop = true
      window.isVisible = true
    }
    updateTooltipText(e)
  }

  override fun mouseReleased(e: MouseEvent) {
    window.isVisible = false
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
