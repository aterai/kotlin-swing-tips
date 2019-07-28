package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.event.MouseInputListener
import javax.swing.table.DefaultTableModel
import javax.swing.table.JTableHeader

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("String", "Integer", "Boolean")
    val data = arrayOf(
        arrayOf("aaa", 12, true),
        arrayOf("bbb", 5, false),
        arrayOf("CCC", 92, true),
        arrayOf("DDD", 0, false))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
    }

    val table = object : JTable(model) {
      @Transient
      private var handler: MouseInputListener? = null

      override fun updateUI() {
        getTableHeader().removeMouseListener(handler)
        getTableHeader().removeMouseMotionListener(handler)
        super.updateUI()
        handler = ColumnWidthResizeHandler()
        getTableHeader().addMouseListener(handler)
        getTableHeader().addMouseMotionListener(handler)
      }
    }
    add(JScrollPane(table))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class ColumnWidthResizeHandler : MouseInputAdapter() {
  private val window = JWindow()
  private val tip = JToolTip()
  private var prev = ""

  private fun getToolTipLocation(e: MouseEvent): Point {
    val p = e.getPoint()
    val c = e.getComponent()
    SwingUtilities.convertPointToScreen(p, c)
    p.translate(0, -tip.getPreferredSize().height)
    return p
  }

  private fun getResizingColumn(e: MouseEvent) = (e.getComponent() as? JTableHeader)?.getResizingColumn()

  private fun updateTooltipText(e: MouseEvent) {
    getResizingColumn(e)?.also {
      val txt = String.format("Width: %dpx", it.getWidth())
      tip.setTipText(txt)
      if (prev.length != txt.length) {
        window.pack()
      }
      window.setLocation(getToolTipLocation(e))
      prev = txt
    }
  }

  override fun mouseDragged(e: MouseEvent) {
    if (!window.isVisible() && getResizingColumn(e) != null) {
      window.add(tip)
      window.setAlwaysOnTop(true)
      window.setVisible(true)
    }
    updateTooltipText(e)
  }

  override fun mouseReleased(e: MouseEvent) {
    window.setVisible(false)
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
