package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.geom.Path2D
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

class MainPanel : JPanel(BorderLayout()) {
  init {
    val columnNames = arrayOf("Name", "Comment")
    val data = arrayOf(
        arrayOf<Any>("test1.jpg", "adfasd"),
        arrayOf<Any>("test1234.jpg", "  "),
        arrayOf<Any>("test15354.gif", "fasdf"),
        arrayOf<Any>("t.png", "comment"),
        arrayOf<Any>("tfasdfasd.jpg", "123"),
        arrayOf<Any>("afsdfasdfffffffffffasdfasdf.mpg", "test"),
        arrayOf<Any>("fffffffffffasdfasdf", ""),
        arrayOf<Any>("test1.jpg", ""))
    val model = object : DefaultTableModel(data, columnNames) {
      override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

      override fun isCellEditable(row: Int, column: Int) = false
    }
    add(JScrollPane(FileListTable(model)))
    setPreferredSize(Dimension(320, 240))
  }
}

internal class SelectedImageFilter : RGBImageFilter() {
  override fun filterRGB(x: Int, y: Int, argb: Int): Int {
    val r = argb shr 16 and 0xFF
    val g = argb shr 8 and 0xFF
    val mask = 0xFF_00_00_FF.toInt() // -0xffff01
    return argb and mask or (r shr 1 shl 16) or (g shr 1 shl 8)
  }
}

internal class FileNameRenderer(table: JTable) : TableCellRenderer {
  protected val dim = Dimension()
  private val renderer = JPanel(BorderLayout())
  private val textLabel = JLabel(" ")
  private val iconLabel: JLabel
  private val focusBorder = UIManager.getBorder("Table.focusCellHighlightBorder")
  private val noFocusBorder = UIManager.getBorder("Table.noFocusBorder") ?: let {
    val i = focusBorder.getBorderInsets(textLabel)
    BorderFactory.createEmptyBorder(i.top, i.left, i.bottom, i.right)
  }
  private val nicon: ImageIcon
  private val sicon: ImageIcon

  init {
    val p = object : JPanel(BorderLayout()) {
      override fun getPreferredSize() = dim
    }
    p.setOpaque(false)
    renderer.setOpaque(false)

    // [XP Style Icons - Download](https://xp-style-icons.en.softonic.com/)
    nicon = ImageIcon(javaClass.getResource("wi0063-16.png"))

    val ip = FilteredImageSource(nicon.getImage().getSource(), SelectedImageFilter())
    sicon = ImageIcon(p.createImage(ip))

    iconLabel = JLabel(nicon)
    iconLabel.setBorder(BorderFactory.createEmptyBorder())

    p.add(iconLabel, BorderLayout.WEST)
    p.add(textLabel)
    renderer.add(p, BorderLayout.WEST)

    val d = iconLabel.getPreferredSize()
    dim.setSize(d)
    table.setRowHeight(d.height)
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    textLabel.setFont(table.getFont())
    textLabel.setText(value?.toString() ?: "")
    textLabel.setBorder(if (hasFocus) focusBorder else noFocusBorder)

    val fm = table.getFontMetrics(table.getFont())
    val i = textLabel.getInsets()
    val swidth = iconLabel.getPreferredSize().width + fm.stringWidth(textLabel.getText()) + i.left + i.right
    val cwidth = table.getColumnModel().getColumn(column).getWidth()
    dim.width = Math.min(swidth, cwidth)

    if (isSelected) {
      textLabel.setOpaque(true)
      textLabel.setForeground(table.getSelectionForeground())
      textLabel.setBackground(table.getSelectionBackground())
      iconLabel.setIcon(sicon)
    } else {
      textLabel.setOpaque(false)
      textLabel.setForeground(table.getForeground())
      textLabel.setBackground(table.getBackground())
      iconLabel.setIcon(nicon)
    }
    return renderer
  }
}

internal class FileListTable(model: TableModel) : JTable(model) {
  private val rcolor = SystemColor.activeCaption
  private val pcolor = makeColor(rcolor)
  protected val rubberBand: Path2D = Path2D.Double()
  @Transient
  private var rbl: RubberBandingListener? = null

  override fun updateUI() {
    // [JDK-6788475] Changing to Nimbus LAF and back doesn't reset look and feel of JTable completely - Java Bug System
    // https://bugs.openjdk.java.net/browse/JDK-6788475
    // XXX: set dummy ColorUIResource
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
    setIntercellSpacing(Dimension())
    setShowGrid(false)
    setAutoCreateRowSorter(true)
    setFillsViewportHeight(true)

    setDefaultRenderer(Any::class.java, object : DefaultTableCellRenderer() {
      override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
      ) = super.getTableCellRendererComponent(table, value, false, false, row, column)
    })

    var col = getColumnModel().getColumn(0)
    col.setCellRenderer(FileNameRenderer(this))
    col.setPreferredWidth(200)
    col = getColumnModel().getColumn(1)
    col.setPreferredWidth(300)
  }

  override fun getToolTipText(e: MouseEvent): String? {
    val pt = e.getPoint()
    val row = rowAtPoint(pt)
    val col = columnAtPoint(pt)
    if (convertColumnIndexToModel(col) != 0 || row < 0 || row > getRowCount()) {
      return null
    }
    val rect = getCellRect2(this, row, col)
    return if (rect.contains(pt)) {
      getValueAt(row, col).toString()
    } else null
  }

  override fun setColumnSelectionInterval(index0: Int, index1: Int) {
    val idx = convertColumnIndexToView(0)
    super.setColumnSelectionInterval(idx, idx)
  }

  private inner class RubberBandingListener : MouseAdapter() {
    private val srcPoint = Point()

    override fun mouseDragged(e: MouseEvent) {
      val destPoint = e.getPoint()
      rubberBand.reset()
      rubberBand.moveTo(srcPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), srcPoint.getY())
      rubberBand.lineTo(destPoint.getX(), destPoint.getY())
      rubberBand.lineTo(srcPoint.getX(), destPoint.getY())
      rubberBand.closePath()
      clearSelection()
      val col = convertColumnIndexToView(0)
      (0 until getModel().getRowCount())
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
      srcPoint.setLocation(e.getPoint())
      if (rowAtPoint(e.getPoint()) < 0) {
        clearSelection()
        repaint()
      } else {
        val index = rowAtPoint(e.getPoint())
        val rect = getCellRect2(this@FileListTable, index, convertColumnIndexToView(0))
        if (!rect.contains(e.getPoint())) {
          clearSelection()
          repaint()
        }
      }
    }
  }

  protected override fun paintComponent(g: Graphics) {
    super.paintComponent(g)
    val g2 = g.create() as Graphics2D
    g2.setPaint(rcolor)
    g2.draw(rubberBand)
    g2.setComposite(ALPHA)
    g2.setPaint(pcolor)
    g2.fill(rubberBand)
    g2.dispose()
  }

  // SwingUtilities2.pointOutsidePrefSize(...)
  protected fun getCellRect2(table: JTable, row: Int, col: Int): Rectangle {
    val tcr = table.getCellRenderer(row, col)
    val value = table.getValueAt(row, col)
    val cell = tcr.getTableCellRendererComponent(table, value, false, false, row, col)
    val itemSize = cell.getPreferredSize()
    val cellBounds = table.getCellRect(row, col, false)
    cellBounds.width = itemSize.width
    return cellBounds
  }

  private fun makeColor(c: Color): Color {
    val r = c.getRed()
    val g = c.getGreen()
    val b = c.getBlue()
    return if (r > g) if (r > b) Color(r, 0, 0) else Color(0, 0, b)
    else if (g > b) Color(0, g, 0) else Color(0, 0, b)
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
      setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      getContentPane().add(MainPanel())
      pack()
      setLocationRelativeTo(null)
      setVisible(true)
    }
  }
}
