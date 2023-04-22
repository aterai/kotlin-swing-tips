package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableRowSorter

fun makeUI(): Component {
  val columnNames = arrayOf("String", "Integer", "Boolean")
  val data = arrayOf(
    arrayOf("a", 12, true),
    arrayOf("b", 5, false),
    arrayOf("C", 92, true),
    arrayOf("D", 0, false)
  )
  val model = object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass
  }
  val table = object : JTable(model) {
    override fun updateUI() {
      super.updateUI()
      val hr = VerticalTableHeaderRenderer()
      val cm = getColumnModel()
      for (i in 0 until cm.columnCount) {
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
  button.addActionListener { sorter.setSortKeys(null) }

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
  private val emptyIcon: EmptyIcon
  private val intermediate = JPanel()
  private val label = JLabel("", null, SwingConstants.LEADING)

  init {
    ascendingIcon = UIManager.getLookAndFeelDefaults().getIcon(ASCENDING)
    descendingIcon = UIManager.getLookAndFeelDefaults().getIcon(DESCENDING)
    emptyIcon = EmptyIcon()
  }

  override fun getTableCellRendererComponent(
    table: JTable,
    value: Any?,
    isSelected: Boolean,
    hasFocus: Boolean,
    row: Int,
    column: Int
  ): Component {
    emptyIcon.width = ascendingIcon.iconWidth
    emptyIcon.height = ascendingIcon.iconHeight
    UIManager.put(ASCENDING, emptyIcon)
    UIManager.put(DESCENDING, emptyIcon)
    val r = table.tableHeader.defaultRenderer
    val c = r.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
    UIManager.put(ASCENDING, ascendingIcon)
    UIManager.put(DESCENDING, descendingIcon)
    if (c is JLabel) {
      val sortOrder = getColumnSortOrder(table, column)
      val sortIcon = when (sortOrder) {
        SortOrder.ASCENDING -> ascendingIcon
        SortOrder.DESCENDING -> descendingIcon
        else -> emptyIcon
      }
      label.text = c.getText()
      label.icon = RotateIcon(sortIcon, 90)
      label.horizontalTextPosition = SwingConstants.LEFT
      label.border = BorderFactory.createEmptyBorder(0, 2, 0, 2)
      c.setIcon(makeVerticalHeaderIcon(label))
      c.setText(null)
    }
    return c
  }

  private fun makeVerticalHeaderIcon(c: Component): Icon? {
    val d = c.preferredSize
    val w = d.height
    val h = d.width
    val bi = BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB)
    val g2 = bi.graphics as? Graphics2D ?: return null
    val at = AffineTransform.getTranslateInstance(0.0, h.toDouble())
    at.quadrantRotate(-1)
    g2.transform = at
    SwingUtilities.paintComponent(g2, c, intermediate, 0, 0, h, w)
    g2.dispose()
    return ImageIcon(bi)
  }

  companion object {
    private const val ASCENDING = "Table.ascendingSortIcon"
    private const val DESCENDING = "Table.descendingSortIcon"
    fun getColumnSortOrder(table: JTable?, column: Int): SortOrder {
      var rv = SortOrder.UNSORTED
      if (table != null && table.rowSorter != null) {
        val sortKeys = table.rowSorter.sortKeys
        val mi = table.convertColumnIndexToModel(column)
        if (!sortKeys.isEmpty() && sortKeys[0].column == mi) {
          rv = sortKeys[0].sortOrder
        }
      }
      return rv
    }
  }
}

private class EmptyIcon : Icon {
  var width = 5
  var height = 5

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
    // do nothing
  }

  override fun getIconWidth() = width

  override fun getIconHeight() = height
}

private class RotateIcon(icon: Icon, rotate: Int) : Icon {
  private val dim = Dimension()
  private val image: Image
  private var trans: AffineTransform? = null
  init {
    require(rotate % 90 == 0) { "$rotate: Rotate must be (rotate % 90 == 0)" }
    dim.setSize(icon.iconWidth, icon.iconHeight)
    image = BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB)
    val g = image.getGraphics()
    icon.paintIcon(null, g, 0, 0)
    g.dispose()
    val numQuadrants = rotate / 90 % 4
    when (numQuadrants) {
      3, -1 -> {
        trans = AffineTransform.getTranslateInstance(0.0, dim.width.toDouble())
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }
      1, -3 -> {
        trans = AffineTransform.getTranslateInstance(dim.height.toDouble(), 0.0)
        dim.setSize(icon.iconHeight, icon.iconWidth)
      }
      2 -> trans = AffineTransform.getTranslateInstance(dim.width.toDouble(), dim.height.toDouble())
      else -> trans = AffineTransform.getTranslateInstance(0.0, 0.0)
    }
    trans?.quadrantRotate(numQuadrants)
  }

  override fun paintIcon(c: Component?, g: Graphics, x: Int, y: Int) {
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

  fun initLookAndFeelAction(info: UIManager.LookAndFeelInfo, b: AbstractButton) {
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
    UnsupportedLookAndFeelException::class
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
      contentPane.add(makeUI())
      pack()
      setLocationRelativeTo(null)
      isVisible = true
    }
  }
}
