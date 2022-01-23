package example

import java.awt.* // ktlint-disable no-wildcard-imports
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.InputEvent
import java.awt.event.KeyEvent
import java.awt.image.BufferedImage
import java.awt.image.FilteredImageSource
import java.awt.image.RGBImageFilter
import javax.imageio.ImageIO
import javax.swing.* // ktlint-disable no-wildcard-imports
import javax.swing.border.Border
import javax.swing.plaf.ColorUIResource
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.DefaultTableModel
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableModel

fun makeUI(): Component {
  val columnNames = arrayOf("Name", "Comment")
  val data = arrayOf(
    arrayOf("test1.jpg", "11111"),
    arrayOf("test1234.jpg", "  "),
    arrayOf("test15354.gif", "22222"),
    arrayOf("t.png", "comment"),
    arrayOf("33333.jpg", "123"),
    arrayOf("4444444444444444.mpg", "test"),
    arrayOf("5555555555555", ""),
    arrayOf("test1.jpg", "")
  )
  val table = FileListTable(object : DefaultTableModel(data, columnNames) {
    override fun getColumnClass(column: Int) = getValueAt(0, column).javaClass

    override fun isCellEditable(row: Int, column: Int) = false
  })
  val scroll = JScrollPane(table)
  val tab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, 0)
  val stab = KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.SHIFT_DOWN_MASK)
  val im = table.getInputMap(JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
  im.put(tab, im[KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0)])
  im.put(stab, im[KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, InputEvent.SHIFT_DOWN_MASK)])
  val orgColor = table.selectionBackground
  val tflColor = scroll.background
  val fl = object : FocusListener {
    override fun focusGained(e: FocusEvent) {
      table.selectionForeground = Color.WHITE
      table.selectionBackground = orgColor
    }

    override fun focusLost(e: FocusEvent) {
      table.selectionForeground = Color.BLACK
      table.selectionBackground = tflColor
    }
  }
  table.addFocusListener(fl)
  table.componentPopupMenu = TablePopupMenu()

  return JPanel(BorderLayout()).also {
    it.add(scroll)
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

  private fun makeMissingImage(): BufferedImage {
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
  override fun updateUI() {
    setSelectionForeground(ColorUIResource(Color.RED))
    setSelectionBackground(ColorUIResource(Color.RED))
    setDefaultRenderer(Any::class.java, null)
    super.updateUI()
    putClientProperty("Table.isFileList", true)
    setCellSelectionEnabled(true)
    intercellSpacing = Dimension()
    setShowGrid(false)
    autoCreateRowSorter = true
    fillsViewportHeight = true
    val r = DefaultTableCellRenderer()
    setDefaultRenderer(Any::class.java) { table, value, _, _, row, column ->
      r.getTableCellRendererComponent(table, value, false, false, row, column)
    }
    getColumnModel().getColumn(0).also {
      it.cellRenderer = FileNameRenderer(this)
      it.preferredWidth = 200
    }
    getColumnModel().getColumn(1).preferredWidth = 300
  }
}

private class TablePopupMenu : JPopupMenu() {
  private val delete: JMenuItem

  init {
    add("add").addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        model.addRow(arrayOf("New row", model.rowCount, false))
        val r = table.getCellRect(model.rowCount - 1, 0, true)
        table.scrollRectToVisible(r)
      }
    }
    add("clearSelection").addActionListener { (invoker as? JTable)?.clearSelection() }
    addSeparator()
    delete = add("delete")
    delete.addActionListener {
      val table = invoker as? JTable
      val model = table?.model
      if (model is DefaultTableModel) {
        val selection = table.selectedRows
        for (i in selection.indices.reversed()) {
          model.removeRow(table.convertRowIndexToModel(selection[i]))
        }
      }
    }
  }

  override fun show(c: Component, x: Int, y: Int) {
    if (c is JTable) {
      delete.isEnabled = c.selectedRowCount > 0
      super.show(c, x, y)
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
