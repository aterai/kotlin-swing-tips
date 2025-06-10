package example

import java.awt.*
import java.awt.geom.Area
import java.awt.geom.Rectangle2D
import java.awt.geom.RoundRectangle2D
import javax.swing.*
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel

fun makeUI(): Component {
  val scroll = JScrollPane(makeTable())
  scroll.background = Color.WHITE
  scroll.viewport.background = Color.WHITE
  scroll.border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.isOpaque = true
    it.background = Color.WHITE
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeTable(): JTable {
  val columnNames = arrayOf("A", "B", "C", "Integer")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", "aa", "a", 12),
    arrayOf("bbb", "bb", "b", 5),
    arrayOf("ccc", "cc", "c", 92),
    arrayOf("ddd", "dd", "d", 0),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  return object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(24)
      intercellSpacing = Dimension(0, 3)
      setShowGrid(false)
      autoCreateRowSorter = true
      setRowSelectionAllowed(true)
      val columns = getColumnModel()
      val r = RoundSelectionRenderer()
      for (i in 0..<columns.columnCount) {
        columns.getColumn(i).cellRenderer = r
      }
    }
  }
}

private class RoundSelectionRenderer : DefaultTableCellRenderer() {
  private var pos: Position? = null

  override fun paintComponent(g: Graphics) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.color = background.brighter()
    val area = pos?.getArea(width - 1.0, height - 1.0, ARC)
    g2.fill(area)
    if (isFocusable) {
      g2.color = background
      g2.draw(area)
    }
    g2.dispose()
    super.paintComponent(g)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val c = super.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      false,
      row,
      column,
    )
    if (c is JLabel) {
      c.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
      c.isOpaque = false
      pos = getPosition(table, column)
      val b = value is Number
      c.horizontalAlignment = if (b) RIGHT else LEFT
    }
    c.isFocusable = hasFocus || table.selectionModel.leadSelectionIndex == row
    return c
  }

  companion object {
    private const val ARC = 6.0
  }
}

private enum class Position {
  FIRST,
  MIDDLE,
  LAST,
  ;

  fun getArea(w: Double, h: Double, arc: Double): Area {
    val area = Area()
    if (this == FIRST) {
      area.add(Area(Rectangle2D.Double(w - arc, 0.0, arc + arc, h)))
      area.add(Area(RoundRectangle2D.Double(0.0, 0.0, w, h, arc, arc)))
    } else if (this == LAST) {
      area.add(Area(Rectangle2D.Double(-arc, 0.0, arc + arc, h)))
      area.add(Area(RoundRectangle2D.Double(0.0, 0.0, w, h, arc, arc)))
    } else {
      area.add(Area(Rectangle2D.Double(-arc, 0.0, w + arc + arc, h)))
    }
    return area
  }
}

private fun getPosition(table: JTable, column: Int): Position {
  val isFirst = column == 0
  val isLast = column == table.columnCount - 1
  return if (isFirst) {
    Position.FIRST
  } else if (isLast) {
    Position.LAST
  } else {
    Position.MIDDLE
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
