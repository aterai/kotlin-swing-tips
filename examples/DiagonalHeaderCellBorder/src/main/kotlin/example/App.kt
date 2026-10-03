package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.border.MatteBorder
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

fun createUI(): Component {
  val columnNames = arrayOf("", "Boolean1", "Boolean2", "Boolean3", "Boolean4")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", true, true, false, true),
    arrayOf("bbb", false, false, false, true),
    arrayOf("ccc", false, true, false, true),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val size = 32
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      setRowHeight(size)
      val verticalRenderer = VerticalTableHeaderRenderer()
      val cm = getColumnModel()
      val firstColumn = cm.getColumn(0)
      firstColumn.headerRenderer = DiagonallySplitHeaderRenderer()
      firstColumn.preferredWidth = size * 5
      for (i in 1..<cm.columnCount) {
        val tc = cm.getColumn(i)
        tc.headerRenderer = verticalRenderer
        tc.preferredWidth = size
      }
    }
  }
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val scroll = JScrollPane(table)
  scroll.columnHeader = object : JViewport() {
    override fun getPreferredSize(): Dimension {
      val d = super.getPreferredSize()
      d.height = size * 2
      return d
    }
  }

  return JPanel(BorderLayout()).also {
    it.add(scroll)
    it.preferredSize = Dimension(320, 240)
  }
}

private class DiagonallySplitBorder(
  top: Int,
  left: Int,
  bottom: Int,
  right: Int,
  matteColor: Color,
) : MatteBorder(top, left, bottom, right, matteColor) {
  override fun paintBorder(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
    width: Int,
    height: Int,
  ) {
    super.paintBorder(c, g, x, y, width, height)
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.paint = matteColor
    g2.drawLine(0, 0, width - 1, height - 1)
    g2.dispose()
  }
}

private class DiagonallySplitHeaderRenderer : TableCellRenderer {
  private val panel = JPanel(BorderLayout())

  init {
    val columnHeader = JLabel("TOP-RIGHT", null, SwingConstants.RIGHT)
    columnHeader.border = BorderFactory.createEmptyBorder(8, 0, 0, 4)
    val rowHeader = JLabel("BOTTOM-LEFT", null, SwingConstants.LEFT)
    rowHeader.border = BorderFactory.createEmptyBorder(0, 4, 8, 0)
    panel.isOpaque = true
    panel.background = Color.WHITE
    panel.border = DiagonallySplitBorder(0, 0, 1, 1, Color.GRAY)
    panel.add(columnHeader, BorderLayout.NORTH)
    panel.add(rowHeader, BorderLayout.SOUTH)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ) = panel
}

private class VerticalTableHeaderRenderer : TableCellRenderer {
  private val intermediate = JPanel()
  private val label = JLabel("", null, SwingConstants.LEADING)

  init {
    label.horizontalTextPosition = SwingConstants.LEFT
    label.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int,
  ): Component {
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(
      table,
      value,
      isSelected,
      hasFocus,
      row,
      column,
    )
    if (c is JLabel) {
      label.text = c.text
      label.font = c.font
      label.foreground = c.foreground
      c.horizontalAlignment = SwingConstants.CENTER
      c.border = BorderFactory.createMatteBorder(0, 0, 1, 1, Color.GRAY)
      c.icon = makeVerticalHeaderIcon(label)
      c.text = null
    }
    return c
  }

  private fun makeVerticalHeaderIcon(c: Component): Icon {
    val d = c.preferredSize
    val w = d.height
    val h = d.width
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    val at = AffineTransform.getTranslateInstance(0.0, h.toDouble())
    at.quadrantRotate(-1)
    g2.transform = at
    SwingUtilities.paintComponent(g2, c, intermediate, 0, 0, h, w)
    g2.dispose()
    return ImageIcon(bi)
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
      contentPane.add(createUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
