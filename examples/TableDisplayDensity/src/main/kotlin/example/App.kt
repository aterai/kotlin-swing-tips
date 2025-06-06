package example

import java.awt.*
import java.awt.geom.Path2D
import java.awt.geom.Rectangle2D
import javax.swing.*
import javax.swing.plaf.ColorUIResource
import javax.swing.plaf.DimensionUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("aaa", 12, true),
    arrayOf("bbb", 5, false),
    arrayOf("CCC", 92, true),
    arrayOf("DDD", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    private val iconIns = Insets(2, 2, 2, 2)
    private val checkIcon = CheckBoxIcon()

    override fun updateUI() {
      val reset = ColorUIResource(Color.RED)
      setSelectionForeground(reset)
      setSelectionBackground(reset)
      super.updateUI()
      val def = UIManager.getLookAndFeelDefaults()
      val showGrid = def["Table.showGrid"]
      val gridColor = def.getColor("Table.gridColor")
      if (showGrid == null && gridColor != null) {
        setShowGrid(true)
        setIntercellSpacing(DimensionUIResource(1, 1))
        createDefaultRenderers()
      }
    }

    override fun prepareRenderer(
      renderer: TableCellRenderer,
      row: Int,
      column: Int,
    ) = super.prepareRenderer(renderer, row, column).also {
      it.setFont(font)
      if (it is JCheckBox) {
        it.isBorderPainted = false
        updateCheckIcon(it)
      }
    }

    override fun prepareEditor(
      editor: TableCellEditor,
      row: Int,
      column: Int,
    ) = super.prepareEditor(editor, row, column).also {
      it.setFont(font)
      if (it is JCheckBox) {
        it.foreground = selectionForeground
        it.background = selectionBackground
        it.isBorderPainted = false
        updateCheckIcon(it)
      }
    }

    private fun updateCheckIcon(checkBox: JCheckBox) {
      val s = getRowHeight() - iconIns.top - iconIns.bottom
      checkBox.setIcon(ScaledIcon(checkIcon, s, s))
    }
  }
  table.setAutoCreateRowSorter(true)
  return JPanel(BorderLayout()).also {
    it.add(makeToolBar(table), BorderLayout.NORTH)
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private fun makeToolBar(table: JTable): JToolBar {
  val font = table.font
  val fontSize = font.size2D
  val rowHeight = table.rowHeight
  val b1 = JToggleButton("*1", true)
  b1.addActionListener {
    scaling(
      table,
      font,
      fontSize,
      rowHeight,
      1f,
    )
  }
  val b2 = JToggleButton("*1.5")
  b2.addActionListener {
    scaling(
      table,
      font,
      fontSize,
      rowHeight,
      1.5f,
    )
  }
  val b3 = JToggleButton("*2")
  b3.addActionListener {
    scaling(
      table,
      font,
      fontSize,
      rowHeight,
      2f,
    )
  }
  val toolBar = JToolBar()
  val group = ButtonGroup()
  for (b in listOf(b1, b2, b3)) {
    b.setFocusable(false)
    group.add(b)
    toolBar.add(b)
    toolBar.add(Box.createHorizontalStrut(5))
  }
  return toolBar
}

private fun scaling(
  table: JTable,
  font: Font,
  fontSize: Float,
  rowHeight: Int,
  x: Float,
) {
  table.removeEditor()
  val f = font.deriveFont(fontSize * x)
  table.setFont(f)
  table.tableHeader.setFont(f)
  table.setRowHeight((.5f + rowHeight * x).toInt())
}

private class ScaledIcon(
  private val icon: Icon,
  private val width: Int,
  private val height: Int,
) : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.setRenderingHint(
      RenderingHints.KEY_ANTIALIASING,
      RenderingHints.VALUE_ANTIALIAS_ON,
    )
    g2.translate(x, y)
    val sx = width / icon.iconWidth.toDouble()
    val sy = height / icon.iconHeight.toDouble()
    g2.scale(sx, sy)
    icon.paintIcon(c, g2, 0, 0)
    g2.dispose()
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class CheckBoxIcon : Icon {
  override fun paintIcon(
    c: Component,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create()
    if (g2 is Graphics2D && c is AbstractButton) {
      g2.setRenderingHint(
        RenderingHints.KEY_ANTIALIASING,
        RenderingHints.VALUE_ANTIALIAS_ON,
      )
      g2.translate(x, y)
      g2.paint = c.foreground
      val s = iconWidth.coerceAtMost(iconHeight) * .05
      val w = iconWidth - s - s
      val h = iconHeight - s - s
      val gw = w / 8.0
      val gh = h / 8.0
      g2.stroke = BasicStroke(s.toFloat())
      g2.draw(Rectangle2D.Double(s, s, w, h))
      if (c.model.isSelected) {
        g2.stroke = BasicStroke(3f * s.toFloat())
        val p = Path2D.Double()
        p.moveTo(x + 2f * gw, y + .5f * h)
        p.lineTo(x + .4f * w, y + h - 2f * gh)
        p.lineTo(x + w - 2f * gw, y + 2f * gh)
        g2.draw(p)
      }
      g2.dispose()
    }
  }

  override fun getIconWidth() = 1000 // 16

  override fun getIconHeight() = 1000 // 16
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
