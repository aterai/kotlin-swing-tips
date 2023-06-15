package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("Name", "Comment")
  val data = arrayOf(
    arrayOf("test1.jpg", "111111"),
    arrayOf("test1234.jpg", "  "),
    arrayOf("test15354.gif", "22222222"),
    arrayOf("t.png", "comment"),
    arrayOf("3333333333.jpg", "123"),
    arrayOf("444444444444444444444444.mpg", "test"),
    arrayOf("5555555555555555", ""),
    arrayOf("test1.jpg", "")
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = false
  }
  return JPanel(BorderLayout()).also {
    it.add(JScrollPane(FileListTable(model)))
    it.preferredSize = Dimension(320, 240)
  }
}

private class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    return argb and 0xFF_00_00_FF.toInt() or (r shr 1 shl 16) or (g shr 1 shl 8)
  }
}

private class FileNameRenderer(table: JTable) : TableCellRenderer {
  private val dim = Dimension()
  private val renderer = JPanel(BorderLayout())
  private val textLabel = JLabel(" ")
  private val iconLabel: JLabel
  private val focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("Table.noFocusBorder") ?: makeNoFocusBorder()
  private val icon: ImageIcon
  private val selectedIcon: ImageIcon

  init {
    val p = object : JPanel(BorderLayout()) {
      override fun getPreferredSize() = dim
    }
    p.isOpaque = false
    renderer.isOpaque = false

    // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
    val path = "example/wi0063-16.png"
    val url = Thread.currentThread().contextClassLoader.getResource(path)
    val img = url?.openStream()?.use(ImageIO::read) ?: makeMissingImage()
    icon = ImageIcon(img)

    val ip = FilteredImageSource(img.source, SelectedImageFilter())
    selectedIcon = ImageIcon(p.createImage(ip))

    iconLabel = JLabel(icon)
    iconLabel.border = BorderFactory.createEmptyBorder()

    p.add(iconLabel, BorderLayout.WEST)
    p.add(textLabel)
    renderer.add(p, BorderLayout.WEST)

    val d = iconLabel.preferredSize
    dim.size = d
    table.rowHeight = d.height
  }

  private fun makeMissingImage(): Image {
    val missingIcon = UIManager.getIcon("html.missingImage")
    val iw = missingIcon.iconWidth
    val ih = missingIcon.iconHeight
    val bi = BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.createGraphics()
    missingIcon.paintIcon(null, g2, (16 - iw) / 2, (16 - ih) / 2)
    g2.dispose()
    return bi
  }

  private fun makeNoFocusBorder(): Border {
    val i = focusBorder.getBorderInsets(textLabel)
    return BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    textLabel.font = table.font
    textLabel.text = value?.toString() ?: ""
    textLabel.border = if (hasFocus) focusBorder else noFocusBorder

    val fm = table.getFontMetrics(table.font)
    val i = textLabel.insets
    val sw = iconLabel.preferredSize.width + fm.stringWidth(textLabel.text) + i.left + i.right
    val cw = table.columnModel.getColumn(column).width
    dim.width = minOf(sw, cw)

    if (isSelected) {
      textLabel.isOpaque = true
      textLabel.foreground = table.selectionForeground
      textLabel.background = table.selectionBackground
      iconLabel.icon = selectedIcon
    } else {
      textLabel.isOpaque = false
      textLabel.foreground = table.foreground
      textLabel.background = table.background
      iconLabel.icon = icon
    }
    return renderer
  }
}

private class FileListTable(model: TableModel) : JTable(model) {
  private val bandColor = SystemColor.activeCaption
  private val rectColor = makeRubberBandColor(bandColor)
  private val rubberBand = Path2D.Double()

  private var rbl: RubberBandingListener? = null

  override fun updateUI() {
    // Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely
    // https://bugs.openjdk.org/browse/JDK-6788475
    // Set a temporary ColorUIResource to avoid this issue
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    removeMouseMotionListener(rbl)
    removeMouseListener(rbl)
    super.updateUI()
    rbl = RubberBandingListener()
    addMouseMotionListener(rbl)
    addMouseListener(rbl)

    putClientProperty("Table.isFileList", true)
    setCellSelectionEnabled(true)
    intercellSpacing = Dimension()
    setShowGrid(false)
    autoCreateRowSorter = true
    fillsViewportHeight = true

    val renderer = getDefaultRenderer(Any::class.java)
    setDefaultRenderer(Any::class.java) { table, value, _, _, row, column ->
      renderer.getTableCellRendererComponent(table, value, false, false, row, column)
    }

    var col = getColumnModel().getColumn(0)
    col.cellRenderer = FileNameRenderer(this)
    col.preferredWidth = 200
    col = getColumnModel().getColumn(1)
    col.preferredWidth = 300
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val pt = e.point
    val row = rowAtPoint(pt)
    val col = columnAtPoint(pt)
    if (convertColumnIndexToModel(col) != 0 || row < 0 || row > rowCount) {
      return null
    }
    val rect = getCellRect2(this, row, col)
    return if (rect.contains(pt)) getValueAt(row, col).toString() else null
  }

  override fun setColumnSelectionInterval(index0: Int, index1: Int) {
    val idx = convertColumnIndexToView(0)
    super.setColumnSelectionInterval(idx, idx)
  }

  private inner class RubberBandingListener : MouseAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
      val destPoint = e.point
      rubberBand.reset()
      rubberBand.moveTo(srcPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), destPoint.getY())
      rubberBand.lineTo(srcPoint.getX(), destPoint.getY())
      rubberBand.closePath()
      clearSelection()
      val col = convertColumnIndexToView(0)
      (0 until model.rowCount)
        .filter { rubberBand.intersects(getCellRect2(this@FileListTable, it, col)) }
        .forEach {
          addRowSelectionInterval(it, it)
          changeSelection(it, col, true, true)
        }
      repaint()
    }

    override fun mouseReleased(e: MouseEvent) {
      rubberBand.reset()
      repaint()
    }

    override fun mousePressed(e: MouseEvent) {
      srcPoint.location = e.point
      if (rowAtPoint(e.point) < 0) {
        clearSelection()
        repaint()
      } else {
        val index = rowAtPoint(e.point)
        val rect = getCellRect2(this@FileListTable, index, convertColumnIndexToView(0))
        if (!rect.contains(e.point)) {
          clearSelection()
          repaint()
        }
      }
    }
  }

  override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as? Graphics2D ?: return
    g2.paint = bandColor
    g2.draw(rubberBand)
    g2.composite = ALPHA
    g2.paint = rectColor
    g2.fill(rubberBand)
    g2.dispose()
  }

  // SwingUtilities2.pointOutsidePrefSize(...)
  private fun getCellRect2(table: JTable, row: Int, col: Int): Rectangle {
    val tcr = table.getCellRenderer(row, col)
    val value = table.getValueAt(row, col)
    val cell = tcr.getTableCellRendererComponent(table, value, false, false, row, col)
    val itemSize = cell.preferredSize
    val cellBounds = table.getCellRect(row, col, false)
    cellBounds.width = itemSize.width
    return cellBounds
  }

  private fun makeRubberBandColor(c: Color): Color {
    val r = c.red
    val g = c.green
    val b = c.blue
    val v = when (val max = maxOf(r, g, b)) {
      r -> max shl 8
      g -> max shl 4
      else -> max
    }
    return Color(v)
  }

  companion object {
    private val ALPHA = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .1f)
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
