package example

import java.awt.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.*
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

fun createUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf<Array<Any>>(
    arrayOf("a", 12, true),
    arrayOf("b", 5, false),
    arrayOf("C", 92, true),
    arrayOf("D", 0, false),
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val hr = VerticalTableHeaderRenderer()
      val cm = getColumnModel()
      for (i in 0..<cm.columnCount) {
        val tc = cm.getColumn(i)
        tc.headerRenderer = hr
        tc.preferredWidth = 32
      }
    }
  }
  val sorter = TableRowSorter(model)
  table.rowSorter = sorter
  table.autoResizeMode = JTable.AUTO_RESIZE_OFF

  val button = JButton("clear SortKeys")
  button.addActionListener { sorter.sortKeys = null }

  val mb = JMenuBar()
  mb.add(LookAndFeelUtils.createLookAndFeelMenu())

  return JPanel(BorderLayout()).also {
    EventQueue.invokeLater { it.rootPane.jMenuBar = mb }
    it.add(JScrollPane(table))
    it.add(button, BorderLayout.SOUTH)
    it.preferredSize = Dimension(320, 240)
  }
}

private class VerticalTableHeaderRenderer : TableCellRenderer {
  private val ascendingIcon: Icon
  private val descendingIcon: Icon
  private val emptyIcon: Icon
  private val intermediate = JPanel()
  private val label = JLabel("", null, SwingConstants.LEADING)

  init {
    val def = UIManager.getLookAndFeelDefaults()
    val ascending = def.getIcon(ASCENDING_KEY)
    val descending = def.getIcon(DESCENDING_KEY)
    // The whole header label is rotated 90 degrees counterclockwise later,
    // so rotate the sort icons 90 degrees clockwise in advance to keep them
    // pointing up and down. The rotated icons are created only once here.
    ascendingIcon = RotateIcon(ascending, 90)
    descendingIcon = RotateIcon(descending, 90)
    // Reserve the same space for the unsorted columns
    emptyIcon = EmptyIcon(ascendingIcon.iconWidth, ascendingIcon.iconHeight)
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
    UIManager.put(ASCENDING_KEY, NO_SORT_ICON)
    UIManager.put(DESCENDING_KEY, NO_SORT_ICON)
    val c = try {
      r.getTableCellRendererComponent(
        table,
        value,
        isSelected,
        hasFocus,
        row,
        column,
      )
    } finally {
      // Putting null removes the temporary values from the developer defaults,
      // so the LookAndFeel defaults are used again
      UIManager.put(ASCENDING_KEY, null)
      UIManager.put(DESCENDING_KEY, null)
    }
    if (c is JLabel) {
      val modelColumnIndex = table.convertColumnIndexToModel(column)
      val sortOrder = getColumnSortOrder(table, modelColumnIndex)
      val sortIcon = when (sortOrder) {
        SortOrder.ASCENDING -> ascendingIcon
        SortOrder.DESCENDING -> descendingIcon
        else -> emptyIcon
      }
      label.text = c.text
      label.icon = sortIcon
      c.setIcon(createVerticalHeaderIcon(label))
      c.setText(null)
    }
    return c
  }

  private fun createVerticalHeaderIcon(c: Component): Icon {
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

  companion object {
    private const val ASCENDING_KEY = "Table.ascendingSortIcon"
    private const val DESCENDING_KEY = "Table.descendingSortIcon"

    // A non-Icon value makes UIManager#getIcon(...) and DefaultLookup return null,
    // so the default header renderer neither shows nor paints its own sort icon
    private const val NO_SORT_ICON = ""

    fun getColumnSortOrder(
      table: JTable,
      column: Int,
    ) = table.rowSorter
      ?.sortKeys
      ?.firstOrNull()
      ?.takeIf { it.column == column }
      ?.sortOrder
      ?: SortOrder.UNSORTED
  }
}

private class EmptyIcon(
  private val width: Int,
  private val height: Int,
) : Icon {
  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    // do nothing
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class RotateIcon(
  icon: Icon,
  rotate: Int,
) : Icon {
  private val dim = Dimension()
  private val image: Image
  private var trans: AffineTransform? = null

  init {
    require(rotate % 90 == 0) { "$rotate: Rotate must be (rotate % 90 == 0)" }
    dim.setSize(icon.iconWidth, icon.iconHeight)
    image = BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB)
    val g = image.graphics
    icon.paintIcon(null, g, 0, 0)
    g.dispose()
    val numQuadrants = rotate / 90 % 4
    val tx: Double
    val ty: Double
    when (numQuadrants) {
      3, -1 -> {
        tx = 0.0
        ty = dim.getWidth()
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }

      1, -3 -> {
        tx = dim.getHeight()
        ty = 0.0
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }

      2 -> {
        tx = dim.getWidth()
        ty = dim.getHeight()
      }

      else -> {
        tx = 0.0
        ty = 0.0
      }
    }
    trans = AffineTransform.getTranslateInstance(tx, ty).also {
      it.quadrantRotate(numQuadrants)
    }
  }

  override fun paintIcon(
    c: Component?,
    g: Graphics,
    x: Int,
    y: Int,
  ) {
    val g2 = g.create() as? Graphics2D ?: return
    g2.translate(x, y)
    g2.drawImage(image, trans, c)
    g2.dispose()
  }

  override fun getIconWidth() = dim.width

  override fun getIconHeight() = dim.height
}

private object LookAndFeelUtils {
  private var lookAndFeel = UIManager.getLookAndFeel().javaClass.name

  fun createLookAndFeelMenu(): JMenu {
    val menu = JMenu("LookAndFeel")
    val buttonGroup = ButtonGroup()
    for (info in UIManager.getInstalledLookAndFeels()) {
      val b = JRadioButtonMenuItem(info.name, info.className == lookAndFeel)
      initLookAndFeelAction(info, b)
      menu.add(b)
      buttonGroup.add(b)
    }
    return menu
  }

  fun initLookAndFeelAction(
    info: UIManager.LookAndFeelInfo,
    b: AbstractButton,
  ) {
    val cmd = info.className
    b.text = info.name
    b.actionCommand = cmd
    b.hideActionText = true
    b.addActionListener { setLookAndFeel(cmd) }
  }

  @Throws(
    ClassNotFoundException::class,
    InstantiationException::class,
    IllegalAccessException::class,
    UnsupportedLookAndFeelException::class,
  )
  private fun setLookAndFeel(newLookAndFeel: String) {
    val oldLookAndFeel = lookAndFeel
    if (oldLookAndFeel != newLookAndFeel) {
      UIManager.setLookAndFeel(newLookAndFeel)
      lookAndFeel = newLookAndFeel
      updateLookAndFeel()
    }
  }

  private fun updateLookAndFeel() {
    for (window in Window.getWindows()) {
      SwingUtilities.updateComponentTreeUI(window)
    }
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
