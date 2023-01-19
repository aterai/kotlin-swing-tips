package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.font.FontRenderContext
import java.awt.font.GlyphMetrics
import java.awt.font.GlyphVector
import java.awt.geom.Point2D
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer

private const val STR0 = "Default Default Default Default"
private const val STR1 = "GlyphVector GlyphVector GlyphVector GlyphVector"
private const val STR2 = "JTextArea JTextArea JTextArea JTextArea"
private const val STR3 = "***************************************"

fun makeUI(): Component {
  val columnNames = arrayOf("Default", "GlyphVector", "JTextArea")
  val data = arrayOf(
    arrayOf(STR0, STR1, STR2),
    arrayOf(STR0, STR1, STR2),
    arrayOf(STR3, STR3, STR3),
    arrayOf(STR3, STR3, STR3)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      setSelectionForeground(null)
      setSelectionBackground(null)
      getColumnModel().getColumn(0).cellRenderer = null
      getColumnModel().getColumn(1).cellRenderer = null
      getColumnModel().getColumn(2).cellRenderer = null
      super.updateUI()
      setRowSelectionAllowed(true)
      setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION)
      setRowHeight(50)
      getColumnModel().getColumn(0).cellRenderer = DefaultTableCellRenderer()
      getColumnModel().getColumn(1).cellRenderer = WrappedLabelRenderer()
      getColumnModel().getColumn(2).cellRenderer = TextAreaCellRenderer()
    }
  }
  val tableHeader = table.tableHeader
  tableHeader.reorderingAllowed = false

  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(table))
    it.preferredSize = Dimension(320, 240)
  }
}

private class WrappedLabelRenderer : TableCellRenderer {
  private val renderer = object : WrappedLabel() {
    override fun updateUI() {
      super.updateUI()
      isOpaque = true
      border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
    }
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (isSelected) {
      renderer.foreground = table.selectionForeground
      renderer.background = table.selectionBackground
    } else {
      renderer.foreground = table.foreground
      renderer.background = table.background
    }
    val b = if (value is Number) SwingConstants.RIGHT else SwingConstants.LEFT
    renderer.horizontalAlignment = b
    renderer.font = table.font
    renderer.text = value?.toString() ?: ""
    return renderer
  }
}

private open class WrappedLabel(str: String? = "") : JLabel(str) {
  private var gvText: GlyphVector? = null
  override fun doLayout() {
    val i = insets
    val w = width - i.left - i.right
    val font = font
    val fm = getFontMetrics(font)
    val frc = fm.fontRenderContext
    gvText = getWrappedGlyphVector(text, w.toDouble(), font, frc)
    super.doLayout()
  }

  override fun paintComponent(g: Graphics) {
    if (gvText != null) {
      val i = insets
      val g2 = g.create() as? Graphics2D ?: return
      g2.paint = background
      g2.fillRect(0, 0, width, height)
      g2.paint = foreground
      g2.drawGlyphVector(gvText, i.left.toFloat(), font.size2D + i.top)
      g2.dispose()
    } else {
      super.paintComponent(g)
    }
  }

  companion object {
    private fun getWrappedGlyphVector(
      str: String,
      width: Double,
      font: Font,
      frc: FontRenderContext
    ): GlyphVector {
      val gmPos = Point2D.Float()
      val gv = font.createGlyphVector(frc, str)
      val lineHeight = gv.logicalBounds.height.toFloat()
      var pos = 0f
      var lineCount = 0
      var gm: GlyphMetrics
      for (i in 0 until gv.numGlyphs) {
        gm = gv.getGlyphMetrics(i)
        val advance = gm.advance
        if (pos < width && width <= pos + advance) {
          lineCount++
          pos = 0f
        }
        gmPos.setLocation(pos.toDouble(), lineHeight * lineCount.toDouble())
        gv.setGlyphPosition(i, gmPos)
        pos += advance
      }
      return gv
    }
  }
}

private class TextAreaCellRenderer : TableCellRenderer {
  private val renderer = JTextArea()

  init {
    renderer.lineWrap = true
    renderer.border = BorderFactory.createEmptyBorder(0, 5, 0, 5)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    if (isSelected) {
      renderer.foreground = table.selectionForeground
      renderer.background = table.selectionBackground
    } else {
      renderer.foreground = table.foreground
      renderer.background = table.background
    }
    renderer.font = table.font
    renderer.text = value?.toString() ?: ""
    return renderer
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
