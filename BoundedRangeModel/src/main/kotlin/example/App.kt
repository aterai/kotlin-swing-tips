package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseEvent
import java.awt.geom.AffineTransform
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.event.MouseInputAdapter
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import kotlin.math.roundToInt

@Transient
private val emphasisIndices: MutableList<Int> = ArrayList()
private val model = DefaultTableModel(0, 2)
private val table = object : JTable(model) {
  override fun updateUI() {
    setDefaultRenderer(Any::class.java, null)
    super.updateUI()
    val renderer = DefaultTableCellRenderer()
    setDefaultRenderer(Any::class.java) { tbl, value, isSelected, hasFocus, row, column ->
      val c = renderer.getTableCellRendererComponent(tbl, value, isSelected, hasFocus, row, column)
      if (emphasisIndices.contains(row)) {
        c.background = Color.YELLOW
      } else {
        c.background = if (isSelected) tbl.selectionBackground else tbl.background
      }
      c
    }
    fillsViewportHeight = true
  }
}
private val scroll = JScrollPane(table)
private val label = JLabel()
val scrollbar = JScrollBar(Adjustable.VERTICAL)

private class HighlightBarHandler : MouseInputAdapter() {
  override fun mousePressed(e: MouseEvent) {
    processHighlightBarMouseEvent(e)
  }

  override fun mouseDragged(e: MouseEvent) {
    processHighlightBarMouseEvent(e)
  }

  private fun processHighlightBarMouseEvent(e: MouseEvent) {
    val pt = e.point
    val c = e.component
    val m = scrollbar.model
    val iv = (pt.y * (m.maximum - m.minimum) / c.height.toFloat() - m.extent / 2f).roundToInt()
    m.value = iv
  }
}

private fun updateHighlighter() {
  for (i in 0 until table.rowCount) {
    if (PATTERN == table.getValueAt(i, 0)) {
      emphasisIndices.add(i)
    }
  }
}

private class HighlightIcon : Icon {
  override fun paintIcon(c: Component, g: Graphics, x: Int, y: Int) {
    val vport = SwingUtilities.getAncestorOfClass(JViewport::class.java, table) as? JViewport ?: return
    val viewRect = vport.bounds
    val tableRect = table.bounds
    val cellRect = SwingUtilities.calculateInnerArea(label, label.bounds)

    // paint Background
    g.color = Color.WHITE
    g.fillRect(cellRect.x, cellRect.y, cellRect.width, cellRect.height)
    val sy = cellRect.getHeight() / tableRect.getHeight()
    val at = AffineTransform.getScaleInstance(1.0, sy)

    // paint Highlight
    g.color = Color.YELLOW
    emphasisIndices.forEach { viewIndex ->
      val r = table.getCellRect(viewIndex, 0, true)
      val s = at.createTransformedShape(r).bounds
      g.fillRect(x, cellRect.y + s.y, iconWidth, 2.coerceAtLeast(s.height - 2))
    }

    // paint Thumb
    if (scrollbar.isVisible) {
      val thumbRect = Rectangle(viewRect)
      thumbRect.y = vport.viewPosition.y
      g.color = THUMB_COLOR
      val r = at.createTransformedShape(thumbRect).bounds
      g.fillRect(x, cellRect.y + r.y, iconWidth, r.height)
      g.color = THUMB_COLOR.darker()
      g.drawRect(x, cellRect.y + r.y, iconWidth - 1, r.height - 1)
    }
  }

  override fun getIconWidth() = 14

  override fun getIconHeight() = scroll.height
}

private val THUMB_COLOR = Color(0, 0, 255, 50)
private const val PATTERN = "Swing"

fun makeUI(): Component {
  for (i in 0 until 100) {
    val o = if (i % 19 == 0 || i % 17 == 0) PATTERN else "Java"
    model.addRow(arrayOf(o, ""))
  }
  scroll.verticalScrollBar = scrollbar
  scrollbar.model.addChangeListener { label.repaint() }
  label.icon = HighlightIcon()
  val inner = BorderFactory.createLineBorder(Color.BLACK)
  val outer = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  label.border = BorderFactory.createCompoundBorder(outer, inner)
  val handler = HighlightBarHandler()
  label.addMouseListener(handler)
  label.addMouseMotionListener(handler)
  val button = JToggleButton("highlight")
  button.addActionListener { e ->
    emphasisIndices.clear()
    if ((e.source as? JToggleButton)?.isSelected == true) {
      updateHighlighter()
    }
    label.rootPane.repaint()
  }
  val box = Box.createHorizontalBox()
  box.add(Box.createHorizontalGlue())
  box.add(button)
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.add(label, BorderLayout.EAST)
    it.add(box, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
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
      getContentPane().add(makeUI())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
